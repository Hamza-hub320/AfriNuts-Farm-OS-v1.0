package com.afrinuts.farmos.data.sync;

import android.content.Context;
import android.util.Log;

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
    private String currentFarmId;
    private SyncListener listener;

    // Add this field to track sync state
    private boolean isSyncing = false;

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

    public void init(LifecycleOwner lifecycleOwner, String farmId, SyncListener listener) {
        this.currentFarmId = farmId;
        this.listener = listener;

        // Observe network changes
        networkUtils.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isConnected) {
                if (isConnected) {
                    Log.d(TAG, "Network connected - triggering sync");
                    if (listener != null) listener.onOnlineMode();
                    triggerAutoSync();
                } else {
                    Log.d(TAG, "Network disconnected");
                    if (listener != null) listener.onOfflineMode();
                }
            }
        });

        // Schedule periodic sync (every 6 hours)
        schedulePeriodicSync();
    }

    /**
     * Queue an item for sync
     */
    public void queueForSync(String entityType, long entityId, SyncEntity.SyncAction action) {
        if (currentFarmId == null) {
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
            } else {
                // Create new
                SyncEntity syncItem = new SyncEntity(
                        entityType,
                        entityId,
                        currentFarmId,
                        action
                );
                database.syncDao().insert(syncItem);
            }

            // If online, trigger sync
            if (NetworkUtils.isConnected(context)) {
                triggerAutoSync();
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
        if (currentFarmId == null) {
            Log.e(TAG, "No farm ID set, cannot sync");
            return;
        }

        // Check if there's anything to sync
        new Thread(() -> {
            int pendingCount = database.syncDao().getPendingCount();
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
                    .putString("farmId", currentFarmId)
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

            // Observe work status
            WorkManager.getInstance(context).getWorkInfoByIdLiveData(syncWork.getId())
                    .observeForever(new Observer<WorkInfo>() {
                        @Override
                        public void onChanged(WorkInfo workInfo) {
                            if (workInfo == null) return;

                            if (workInfo.getState() == WorkInfo.State.RUNNING) {
                                // Already syncing
                            } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
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
                        }
                    });

        }).start();
    }

    /**
     * Schedule periodic background sync (every 6 hours)
     */
    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        Data inputData = new Data.Builder()
                .putString("farmId", currentFarmId)
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
    }

    /**
     * Force immediate sync (manual override)
     */
    public void forceSyncNow() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK);
        triggerAutoSync();
    }

    /**
     * Clean up
     */
    public void shutdown() {
        networkUtils.removeObservers(null);
    }
}