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

    // Add this field to track sync state
    private boolean isSyncing = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Added flags for initialization and periodic sync scheduling
    private boolean isInitialized = false;
    private boolean isPeriodicSyncScheduled = false;

    // Handler and Runnable for debouncing sync triggers
    private Handler syncHandler = new Handler(Looper.getMainLooper());
    private final long SYNC_DEBOUNCE_MS = 500;
    private Runnable pendingSyncRunnable;

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

        // Remove any existing network observer to prevent duplicates
        if (networkUtils != null) {
            networkUtils.removeObservers(null);
        }

        // Observe network changes with debounce
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

        // Schedule periodic sync only once
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

    /**
     * Queue an item for sync
     */
    public void queueForSync(String entityType, long entityId, SyncEntity.SyncAction action) {
        // 🚫 CRITICAL FIX: Prevent syncing USERS from local → Firestore
        // Users must ONLY be created from Authentication + Admin flow
        if ("users".equalsIgnoreCase(entityType)) {
            Log.w(TAG, "⛔ Blocking user sync to prevent duplicates. entityId: " + entityId);
            return;
        }
        if (currentFarmId <= 0) {
            Log.e(TAG, "No farm ID set, cannot queue sync");
            return;
        }

        new Thread(() -> {
            // Check if already queued
            SyncEntity existing = database.syncDao().getSyncItem(entityType, entityId);

            if (existing != null) {
                // Update existing
                existing.setAction(action);
                existing.setStatus(SyncEntity.SyncStatus.PENDING);
                existing.setAttemptCount(0);
                database.syncDao().update(existing);
                Log.d(TAG, "Updated sync item for entityType: " + entityType + ", entityId: " + entityId);
            } else {
                // Create new
                SyncEntity syncItem = new SyncEntity(
                        entityType,
                        entityId,
                        String.valueOf(currentFarmId),
                        action
                );
                database.syncDao().insert(syncItem);
                Log.d(TAG, "Created new sync item for entityType: " + entityType + ", entityId: " + entityId);
            }

            // If online, trigger sync with debounce
            if (NetworkUtils.isConnected(context)) {
                debouncedTriggerSync();
            }
        }).start();
    }

    /**
     * Check if sync is currently in progress
     */
    public boolean isSyncing() {
        return isSyncing;
    }

    /**
     * Trigger auto-sync (respects WorkManager constraints)
     */
    public void triggerAutoSync() {
        Log.d(TAG, "triggerAutoSync called");
        if (currentFarmId <= 0) {
            Log.e(TAG, "No farm ID set, cannot sync");
            return;
        }

        // Prevent concurrent syncs
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

            // Create work request with constraints
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

            // Set syncing flag to true
            isSyncing = true;
            if (listener != null) listener.onSyncStarted();

            // Enqueue work
            WorkManager.getInstance(context)
                    .beginUniqueWork(UNIQUE_SYNC_WORK, ExistingWorkPolicy.KEEP, syncWork)
                    .enqueue();

            // FIX: Move observeForever to main thread
            mainHandler.post(() -> {
                WorkManager.getInstance(context).getWorkInfoByIdLiveData(syncWork.getId())
                        .observeForever(new Observer<WorkInfo>() {
                            @Override
                            public void onChanged(WorkInfo workInfo) {
                                if (workInfo == null) return;

                                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                    isSyncing = false;
                                    if (listener != null) {
                                        listener.onSyncCompleted(pendingCount);
                                    }
                                    WorkManager.getInstance(context)
                                            .getWorkInfoByIdLiveData(syncWork.getId())
                                            .removeObserver(this);
                                } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                                    isSyncing = false;
                                    if (listener != null) {
                                        listener.onSyncFailed("Sync failed");
                                    }
                                    WorkManager.getInstance(context)
                                            .getWorkInfoByIdLiveData(syncWork.getId())
                                            .removeObserver(this);
                                }
                                // Ignore RUNNING state
                            }
                        });
            });

        }).start();
    }

    /**
     * Schedule periodic background sync (every 6 hours)
     */
    private void schedulePeriodicSync() {
        // Schedule only once
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

    /**
     * Force immediate sync (manual override)
     */
    public void forceSyncNow() {
        Log.d(TAG, "forceSyncNow called");
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK);
        triggerAutoSync();
    }

    public interface SyncCountCallback {
        void onCountReceived(int count);
    }

    // Get number of pending sync items
    public void getPendingCount(SyncCountCallback callback) {
        new Thread(() -> {
            int count = database.syncDao().getPendingCount();
            mainHandler.post(() -> callback.onCountReceived(count));
        }).start();
    }

    /**
     * Clean up
     */
    public void shutdown() {
        if (networkUtils != null) {
            networkUtils.removeObservers(null);
        }
    }

    /**
     * Mark a sync item as successfully synced
     */
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

    /**
     * Mark a sync item as failed and update attempt count
     */
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

    /**
     * Get a sync item by entity type and ID (for AutoSyncWorker)
     */
    public SyncEntity getSyncItem(String entityType, long entityId) {
        return database.syncDao().getSyncItem(entityType, entityId);
    }
}