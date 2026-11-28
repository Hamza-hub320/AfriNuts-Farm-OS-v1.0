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
import com.afrinuts.farmos.data.integrity.IntegrityScanWorker;
import com.afrinuts.farmos.utils.NetworkUtils;
import com.afrinuts.farmos.utils.SessionManager;

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
    private SyncListener syncCenterListener;

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

    /** Supplies the active farm and a screen-scoped observer without replacing hub callbacks. */
    public void setSyncCenterContext(long farmId, SyncListener observer) {
        this.currentFarmId = farmId;
        this.syncCenterListener = observer;
    }

    public void clearSyncCenterListener(SyncListener observer) {
        if (syncCenterListener == observer) syncCenterListener = null;
    }

    private void notifyStarted() {
        mainHandler.post(() -> {
            if (listener != null) listener.onSyncStarted();
            if (syncCenterListener != null) syncCenterListener.onSyncStarted();
        });
    }

    private void notifyCompleted(int count) {
        if (listener != null) listener.onSyncCompleted(count);
        if (syncCenterListener != null) syncCenterListener.onSyncCompleted(count);
    }

    private void notifyFailed(String message) {
        if (listener != null) listener.onSyncFailed(message);
        if (syncCenterListener != null) syncCenterListener.onSyncFailed(message);
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

        if ("certifications".equals(entityType) || "certification".equals(entityType)) {
            Log.w(TAG, "Certification sync should use String ID, not Long. Please use the String version.");
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

    // In SyncManager.java - add new method

    public void queueForSync(String entityType, String entityStringId, SyncEntity.SyncAction action) {
        if (currentFarmId <= 0) {
            Log.e(TAG, "No farm ID set, cannot queue sync");
            return;
        }

        new Thread(() -> {
            // Check if already queued
            SyncEntity existing = database.syncDao().getSyncItemByStringId(entityType, entityStringId);

            if (existing != null) {
                // Update existing
                existing.setAction(action);
                existing.setStatus(SyncEntity.SyncStatus.PENDING);
                existing.setAttemptCount(0);
                database.syncDao().update(existing);
                Log.d(TAG, "Updated sync item for entityType: " + entityType + ", entityStringId: " + entityStringId);
            } else {
                // Create new with string ID
                SyncEntity syncItem = new SyncEntity(entityType, entityStringId, String.valueOf(currentFarmId), action);
                database.syncDao().insert(syncItem);
                Log.d(TAG, "Created new sync item for entityType: " + entityType + ", entityStringId: " + entityStringId);
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
            notifyStarted();

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
                                    int failed = workInfo.getOutputData().getInt("failed_count", 0);
                                    int completed = workInfo.getOutputData().getInt("synced_count", Math.max(0, pendingCount - failed));
                                    if (failed > 0) notifyFailed(workInfo.getOutputData().getString("failure_reason") != null
                                            ? workInfo.getOutputData().getString("failure_reason") : "Some records could not be synced");
                                    else notifyCompleted(completed);
                                    WorkManager.getInstance(context)
                                            .getWorkInfoByIdLiveData(syncWork.getId())
                                            .removeObserver(this);
                                } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                                    Log.e(TAG, "❌ Sync work FAILED");
                                    isSyncing = false;
                                    currentWorkId = null;
                                    String reason = workInfo.getOutputData().getString("failure_reason");
                                    notifyFailed(reason == null ? "Sync failed" : reason);
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
                    notifyFailed("Sync timeout");
                    // Cancel the work
                    WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK);
                }
            }, 60000); // 60 second timeout

        }).start();
    }

    private void schedulePeriodicSync() {
        scheduleCertificationDueCheck();
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

    private void scheduleCertificationDueCheck() {
        if (currentFarmId <= 0) {
            Log.d(TAG, "No farm ID, skipping certification due check scheduling");
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();
        Data inputData = new Data.Builder()
                .putLong("farmId", currentFarmId)
                .build();
        PeriodicWorkRequest dueCheckWork =
                new PeriodicWorkRequest.Builder(CertificationDueCheckWorker.class, 24, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setInputData(inputData)
                        .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                CertificationDueCheckWorker.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dueCheckWork);
        Log.d(TAG, "Certification due check scheduled every 24 hours");

        Data integrityInput = new Data.Builder()
                .putLong(IntegrityScanWorker.KEY_FARM_ID, currentFarmId)
                .putString(IntegrityScanWorker.KEY_TRIGGER, "scheduled")
                .build();
        PeriodicWorkRequest integrityWork = new PeriodicWorkRequest.Builder(
                IntegrityScanWorker.class, 24, TimeUnit.HOURS)
                .setInputData(integrityInput)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                IntegrityScanWorker.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                integrityWork);
        Log.d(TAG, "Integrity scan scheduled every 24 hours");
    }

    public void forceSyncNow() {
        Log.d(TAG, "forceSyncNow called");
        isSyncing = false;
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK);
        triggerAutoSync();

        if (currentFarmId > 0) {
            FirestoreSyncManager firestoreSyncManager = FirestoreSyncManager.getInstance(context);
            firestoreSyncManager.stopListening();
            firestoreSyncManager.startListening(String.valueOf(currentFarmId),
                    new SessionManager(context).isOwner());
        } else {
            Log.w(TAG, "No farm ID set; skipped Firestore pull listener refresh");
        }
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
