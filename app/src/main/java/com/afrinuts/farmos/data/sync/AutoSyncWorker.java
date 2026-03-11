package com.afrinuts.farmos.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.utils.NetworkUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AutoSyncWorker extends Worker {
    private static final String TAG = "AutoSyncWorker";

    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private String farmId;

    public AutoSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        database = AppDatabase.getInstance(context);
        firebaseManager = FirebaseManager.getInstance();
        farmId = getInputData().getString("farmId");
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "AutoSyncWorker started");

        // Check if we have internet
        if (!NetworkUtils.isConnected(getApplicationContext())) {
            Log.d(TAG, "No internet connection, rescheduling...");
            return Result.retry();
        }

        if (farmId == null) {
            Log.e(TAG, "No farmId provided");
            return Result.failure();
        }

        try {
            // Get all pending sync items
            List<SyncEntity> pendingItems = database.syncDao().getPendingSyncItems();

            if (pendingItems.isEmpty()) {
                Log.d(TAG, "No pending items to sync");
                return Result.success();
            }

            Log.d(TAG, "Syncing " + pendingItems.size() + " items");

            // Process each item
            for (SyncEntity syncItem : pendingItems) {
                processSyncItem(syncItem);
            }

            // Clean up old completed items (older than 7 days)
            long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
            database.syncDao().deleteOldCompleted(weekAgo);

            Log.d(TAG, "AutoSyncWorker completed successfully");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
            return Result.retry();
        }
    }

    private void processSyncItem(SyncEntity syncItem) {
        syncItem.setStatus(SyncEntity.SyncStatus.SYNCING);
        syncItem.setLastAttemptAt(System.currentTimeMillis());
        database.syncDao().update(syncItem);

        try {
            switch (syncItem.getEntityType()) {
                case "block":
                    syncBlock(syncItem);
                    break;
                case "expense":
                    syncExpense(syncItem);
                    break;
                case "revenue":
                    syncRevenue(syncItem);
                    break;
                case "task":
                    syncTask(syncItem);
                    break;
                case "photo":
                    syncPhoto(syncItem);
                    break;
            }

            // Mark as completed
            syncItem.setStatus(SyncEntity.SyncStatus.COMPLETED);
            database.syncDao().update(syncItem);
            Log.d(TAG, "Synced: " + syncItem.getEntityType() + ":" + syncItem.getEntityId());

        } catch (Exception e) {
            Log.e(TAG, "Failed to sync item: " + syncItem.getId(), e);

            syncItem.setStatus(SyncEntity.SyncStatus.FAILED);
            syncItem.setAttemptCount(syncItem.getAttemptCount() + 1);
            database.syncDao().update(syncItem);
        }
    }

    private void syncBlock(SyncEntity syncItem) {
        BlockEntity block = database.blockDao().getBlockById(syncItem.getEntityId());
        if (block == null) return;

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveBlock(block, farmId);
                break;
            case DELETE:
                firebaseManager.deleteBlock(farmId, block.getBlockName());
                break;
        }
    }

    private void syncExpense(SyncEntity syncItem) {
        ExpenseEntity expense = database.expenseDao().getExpenseById(syncItem.getEntityId());
        if (expense == null) return;

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveExpense(expense, farmId);
                break;
            case DELETE:
                // TODO: Implement deleteExpense in FirebaseManager
                break;
        }
    }

    private void syncRevenue(SyncEntity syncItem) {
        RevenueEntity revenue = database.revenueDao().getRevenueById(syncItem.getEntityId());
        if (revenue == null) return;

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveRevenue(revenue, farmId);
                break;
            case DELETE:
                // TODO: Implement deleteRevenue in FirebaseManager
                break;
        }
    }

    private void syncTask(SyncEntity syncItem) {
        TaskEntity task = database.taskDao().getTaskById(syncItem.getEntityId());
        if (task == null) return;

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveTask(task, farmId);
                break;
            case DELETE:
                // TODO: Implement deleteTask in FirebaseManager
                break;
        }
    }

    private void syncPhoto(SyncEntity syncItem) {
        PhotoEntity photo = database.photoDao().getPhotoById(syncItem.getEntityId());
        if (photo == null) return;

        File photoFile = new File(photo.getFilePath());
        if (!photoFile.exists()) return;

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMessage = {null};

        firebaseManager.uploadPhoto(photoFile, String.valueOf(photo.getId()), farmId,
                new FirebaseManager.PhotoUploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        firebaseManager.savePhotoMetadata(photo, farmId, downloadUrl);
                        success[0] = true;
                        latch.countDown();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Photo upload failed: " + error);
                        errorMessage[0] = error;
                        latch.countDown();
                    }

                    @Override
                    public void onProgress(double progress) {
                        // Optional: update progress
                    }
                });

        try {
            if (!latch.await(2, TimeUnit.MINUTES)) {
                throw new RuntimeException("Photo upload timed out");
            }
            if (!success[0]) {
                throw new RuntimeException("Photo upload failed: " +
                        (errorMessage[0] != null ? errorMessage[0] : "Unknown error"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Photo upload interrupted", e);
        }
    }
}