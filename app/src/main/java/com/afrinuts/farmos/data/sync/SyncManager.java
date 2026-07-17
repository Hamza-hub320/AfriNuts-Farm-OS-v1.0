package com.afrinuts.farmos.data.sync;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.utils.NetworkUtils;

import java.util.concurrent.TimeUnit;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final String UNIQUE_SYNC_WORK = "auto_sync_work";
    private static final String PERIODIC_SYNC_WORK = "periodic_sync_work";

    private static SyncManager instance;
    private Context context;
    private AppDatabase database;
    private NetworkUtils networkUtils;
    private long currentFarmId;
    private SyncListener listener;

    private boolean isSyncing = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isInitialized = false;
    private boolean isPeriodicSyncScheduled = false;

    private Handler syncHandler = new Handler(Looper.getMainLooper());
    private final long SYNC_DEBOUNCE_MS = 500;
    private Runnable pendingSyncRunnable;

    // ✅ Track the current work ID to prevent duplicate observers
    private String currentWorkId = null;

    public interface SyncListener {
        void onSyncStarted();
        void onSyncCompleted(int syncedItems);
        void onSyncFailed(String error);
        void onOfflineMode();
        void onOnlineMode();
    }

    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.networkUtils = new NetworkUtils(context);
    }

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }

    public void init(LifecycleOwner lifecycleOwner, long farmId, SyncListener listener) {
        if (isInitialized) {
            Log.d(TAG, "Sync already initialized, skipping");
            return;
        }

        this.currentFarmId = farmId;
        this.listener = listener;

        if (networkUtils != null) {
            networkUtils.removeObservers(null);
        }

        networkUtils.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isConnected) {
                if (isConnected) {
                    Log.d(TAG, "Network connected - debounced triggering sync");
                    if (listener != null) listener.onOnlineMode();
                    debouncedTriggerSync();
                } else {
                    Log.d(TAG, "Network disconnected");
                    if (listener != null) listener.onOfflineMode();
                }
            }
        });

        schedulePeriodicSync();

        isInitialized = true;
        Log.d(TAG, "SyncManager initialized for farm: " + farmId);
    }

    private void debouncedTriggerSync() {
        if (pendingSyncRunnable != null) {
            syncHandler.removeCallbacks(pendingSyncRunnable);
        }
        pendingSyncRunnable = () -> {
            triggerAutoSync();
        };
        syncHandler.postDelayed(pendingSyncRunnable, SYNC_DEBOUNCE_MS);
    }

    public void queueForSync(String entityType, long entityId, SyncEntity.SyncAction action) {
        if ("users".equalsIgnoreCase(entityType)) {
            Log.w(TAG, "⛔ Blocking user sync to prevent duplicates. entityId: " + entityId);
            return;
        }
        if (currentFarmId <= 0) {
            Log.e(TAG, "No farm ID set, cannot queue sync");
            return;
        }

        new Thread(() -> {
            SyncEntity existing = database.syncDao().getSyncItem(entityType, entityId);

            if (existing != null) {
                existing.setAction(action);
                existing.setStatus(SyncEntity.SyncStatus.PENDING);
                existing.setAttemptCount(0);
                database.syncDao().update(existing);
                Log.d(TAG, "Updated sync item for entityType: " + entityType + ", entityId: " + entityId);
            } else {
                SyncEntity syncItem = new SyncEntity(
                        entityType,
                        entityId,
                        String.valueOf(currentFarmId),
                        action
                );
                database.syncDao().insert(syncItem);
                Log.d(TAG, "Created new sync item for entityType: " + entityType + ", entityId: " + entityId);
            }

            if (NetworkUtils.isConnected(context)) {
                debouncedTriggerSync();
            }
        }).start();
    }

    public boolean isSyncing() {
        return isSyncing;
    }

    public void triggerAutoSync() {
        Log.d(TAG, "triggerAutoSync called");
        if (currentFarmId <= 0) {
            Log.e(TAG, "No farm ID set, cannot sync");
            return;
        }

        if (isSyncing) {
            Log.d(TAG, "Sync already in progress, skipping trigger");
            return;
        }

        new Thread(() -> {
            int pendingCount = database.syncDao().getPendingCount();
            Log.d(TAG, "Pending items to sync: " + pendingCount);

            if (pendingCount == 0) {
                Log.d(TAG, "No pending items to sync");
                return;
            }

            Log.d(TAG, "Triggering auto-sync for " + pendingCount + " items");

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build();

            Data inputData = new Data.Builder()
                    .putLong("farmId", currentFarmId)
                    .build();

            OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(AutoSyncWorker.class)
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    .build();

            isSyncing = true;
            if (listener != null) listener.onSyncStarted();

            // ✅ Cancel any existing work
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK);

            // ✅ Store the work ID
            String workId = syncWork.getId().toString();
            currentWorkId = workId;

            WorkManager.getInstance(context)
                    .beginUniqueWork(UNIQUE_SYNC_WORK, ExistingWorkPolicy.REPLACE, syncWork)
                    .enqueue();

            // ✅ Use a more reliable way to observe work completion
            mainHandler.post(() -> {
                WorkManager.getInstance(context).getWorkInfoByIdLiveData(syncWork.getId())
                        .observeForever(new Observer<WorkInfo>() {
                            @Override
                            public void onChanged(WorkInfo workInfo) {
                                if (workInfo == null) return;

                                Log.d(TAG, "WorkInfo state: " + workInfo.getState());

                                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                    Log.d(TAG, "✅ Sync work SUCCEEDED");
                                    isSyncing = false;
                                    currentWorkId = null;
                                    if (listener != null) {
                                        listener.onSyncCompleted(pendingCount);
                                    }
                                    WorkManager.getInstance(context)
                                            .getWorkInfoByIdLiveData(syncWork.getId())
                                            .removeObserver(this);
                                } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                                    Log.e(TAG, "❌ Sync work FAILED");
                                    isSyncing = false;
                                    currentWorkId = null;
                                    if (listener != null) {
                                        listener.onSyncFailed("Sync failed");
                                    }
                                    WorkManager.getInstance(context)
                                            .getWorkInfoByIdLiveData(syncWork.getId())
                                            .removeObserver(this);
                                } else if (workInfo.getState() == WorkInfo.State.CANCELLED) {
                                    Log.d(TAG, "⚠️ Sync work CANCELLED");
                                    isSyncing = false;
                                    currentWorkId = null;
                                    WorkManager.getInstance(context)
                                            .getWorkInfoByIdLiveData(syncWork.getId())
                                            .removeObserver(this);
                                }
                            }
                        });
            });

            // ✅ Add a timeout to prevent infinite sync
            mainHandler.postDelayed(() -> {
                if (isSyncing) {
                    Log.e(TAG, "⚠️ Sync timed out after 60 seconds - forcing reset");
                    isSyncing = false;
                    currentWorkId = null;
                    if (listener != null) {
                        listener.onSyncFailed("Sync timeout");
                    }
                    // Cancel the work
                    WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK);
                }
            }, 60000); // 60 second timeout

        }).start();
    }

    private void schedulePeriodicSync() {
        if (isPeriodicSyncScheduled) {
            Log.d(TAG, "Periodic sync already scheduled");
            return;
        }

        if (currentFarmId <= 0) {
            Log.d(TAG, "No farm ID, skipping periodic sync");
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        Data inputData = new Data.Builder()
                .putLong("farmId", currentFarmId)
                .build();

        PeriodicWorkRequest periodicSyncWork =
                new PeriodicWorkRequest.Builder(AutoSyncWorker.class, 6, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        PERIODIC_SYNC_WORK,
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicSyncWork
                );

        Log.d(TAG, "Periodic sync scheduled every 6 hours");
        isPeriodicSyncScheduled = true;
    }

    public void forceSyncNow() {
        Log.d(TAG, "forceSyncNow called");
        isSyncing = false;
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK);
        triggerAutoSync();
    }

    public interface SyncCountCallback {
        void onCountReceived(int count);
    }

    public void getPendingCount(SyncCountCallback callback) {
        new Thread(() -> {
            int count = database.syncDao().getPendingCount();
            mainHandler.post(() -> callback.onCountReceived(count));
        }).start();
    }

    public void shutdown() {
        if (networkUtils != null) {
            networkUtils.removeObservers(null);
        }
    }

    public void markAsSynced(long syncItemId) {
        new Thread(() -> {
            SyncEntity syncItem = database.syncDao().getSyncItemById(syncItemId);
            if (syncItem != null) {
                syncItem.setStatus(SyncEntity.SyncStatus.COMPLETED);
                database.syncDao().update(syncItem);
                Log.d(TAG, "Marked sync item as synced: " + syncItemId);
            }
        }).start();
    }

    public void markAsFailed(long syncItemId) {
        new Thread(() -> {
            SyncEntity syncItem = database.syncDao().getSyncItemById(syncItemId);
            if (syncItem != null) {
                syncItem.setStatus(SyncEntity.SyncStatus.FAILED);
                syncItem.setAttemptCount(syncItem.getAttemptCount() + 1);
                database.syncDao().update(syncItem);
                Log.d(TAG, "Marked sync item as failed: " + syncItemId +
                        " (attempt " + syncItem.getAttemptCount() + ")");
            }
        }).start();
    }

    public SyncEntity getSyncItem(String entityType, long entityId) {
        return database.syncDao().getSyncItem(entityType, entityId);
    }
}