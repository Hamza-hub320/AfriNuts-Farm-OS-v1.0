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
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.utils.NetworkUtils;
import com.google.firebase.firestore.FirebaseFirestoreException;

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
        farmId = String.valueOf(getInputData().getLong("farmId", -1));
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "========== AUTO SYNC WORKER STARTED ==========");
        Log.d(TAG, "Farm ID: " + farmId);

        // Check if we have internet
        if (!NetworkUtils.isConnected(getApplicationContext())) {
            Log.e(TAG, "No internet connection, rescheduling...");
            return Result.retry();
        }

        if (farmId == null) {
            Log.e(TAG, "No farmId provided");
            return Result.failure();
        }

        try {
            // Get all pending sync items
            List<SyncEntity> pendingItems = database.syncDao().getPendingSyncItems();
            Log.d(TAG, "Found " + pendingItems.size() + " pending items");

            if (pendingItems.isEmpty()) {
                Log.d(TAG, "No pending items to sync");
                return Result.success();
            }

            int successCount = 0;
            int failCount = 0;

            // Process each item
            for (SyncEntity syncItem : pendingItems) {
                Log.d(TAG, "Processing sync item: " + syncItem.getEntityType() +
                        ":" + syncItem.getEntityId() + " action: " + syncItem.getAction());

                boolean success = processSyncItem(syncItem);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            Log.d(TAG, "Sync completed - Success: " + successCount + ", Failed: " + failCount);

            // Clean up old completed items (older than 7 days)
            long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
            int deleted = database.syncDao().deleteOldCompleted(weekAgo);
            Log.d(TAG, "Cleaned up " + deleted + " old completed items");

            Log.d(TAG, "========== AUTO SYNC WORKER FINISHED ==========");

            if (failCount > 0) {
                return Result.retry();
            }
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Sync failed with exception", e);
            return Result.retry();
        }
    }

    private boolean processSyncItem(SyncEntity syncItem) {
        syncItem.setStatus(SyncEntity.SyncStatus.SYNCING);
        syncItem.setLastAttemptAt(System.currentTimeMillis());
        database.syncDao().update(syncItem);

        try {
            Log.d(TAG, "EntityType received: " + syncItem.getEntityType());
            switch (syncItem.getEntityType()) {
                case "block":
                case "blocks":
                    syncBlock(syncItem);
                    break;
                case "expense":
                case "expenses":
                    syncExpense(syncItem);
                    break;
                case "revenue":
                case "revenues":
                    syncRevenue(syncItem);
                    break;
                case "task":
                case "tasks":
                    syncTask(syncItem);
                    break;
                case "photo":
                case "photos":
                    syncPhoto(syncItem);
                    break;
                case "users":
                    syncUser(syncItem);
                    break;
                case "assignment":
                case "assignments":
                    syncAssignment(syncItem);
                    break;
                default:
                    Log.e(TAG, "Unknown entity type: " + syncItem.getEntityType());
                    throw new RuntimeException("Unknown entity type: " + syncItem.getEntityType());
            }

            // Mark as completed
            syncItem.setStatus(SyncEntity.SyncStatus.COMPLETED);
            database.syncDao().update(syncItem);
            Log.d(TAG, "✓ Successfully synced: " + syncItem.getEntityType() + ":" + syncItem.getEntityId());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to sync item: " + syncItem.getId(), e);

            syncItem.setStatus(SyncEntity.SyncStatus.FAILED);
            syncItem.setAttemptCount(syncItem.getAttemptCount() + 1);
            database.syncDao().update(syncItem);
            return false;
        }
    }

    private void syncAssignment(SyncEntity syncItem) throws Exception {
        TaskAssignmentHistoryEntity assignment = database.taskAssignmentDao().getAssignmentById(syncItem.getEntityId());
        if (assignment == null) {
            Log.e(TAG, "Assignment not found in local DB: " + syncItem.getEntityId());
            return;
        }

        Log.d(TAG, "Syncing assignment ID: " + syncItem.getEntityId() +
                " action: " + syncItem.getAction() +
                " taskId: " + assignment.getTaskId());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveTaskAssignment(assignment, farmId);
                Log.d(TAG, "Assignment saved to Firestore: " + assignment.getId());
                break;
            case DELETE:
                // Delete from Firestore
                firebaseManager.deleteAssignment(farmId, assignment.getId());
                Log.d(TAG, "Assignment deleted from Firestore: " + assignment.getId());
                break;
        }
    }
    private void syncUser(SyncEntity syncItem) throws Exception {
        UserEntity user = database.userDao().getUserById(syncItem.getEntityId());
        if (user == null) {
            Log.e(TAG, "User not found in local DB: " + syncItem.getEntityId());
            return;
        }

        Log.d(TAG, "Syncing user ID: " + syncItem.getEntityId());

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMessage = {null};

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:

                firebaseManager.saveUser(user, farmId);

                success[0] = true;
                latch.countDown();

                break;

            case DELETE:
                Log.d(TAG, "User delete not implemented yet");
                success[0] = true;
                latch.countDown();
                break;
        }

        // WAIT for Firebase
        if (!latch.await(60, TimeUnit.SECONDS)) {
            throw new RuntimeException("User sync timeout");
        }

        if (!success[0]) {
            throw new RuntimeException("User sync failed: " +
                    (errorMessage[0] != null ? errorMessage[0] : "Unknown error"));
        }
    }

    private void syncBlock(SyncEntity syncItem) throws Exception {
        BlockEntity block = database.blockDao().getBlockById(syncItem.getEntityId());
        if (block == null) {
            Log.e(TAG, "Block not found in local DB: " + syncItem.getEntityId());
            return;
        }

        Log.d(TAG, "Syncing block: " + block.getBlockName() + " action: " + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                try {
                    firebaseManager.saveBlock(block, farmId);
                    Log.d(TAG, "Block saved to Firestore: " + block.getBlockName());
                } catch (Exception e) {
                    Log.e(TAG, "Firestore saveBlock failed", e);
                    throw e;
                }
                break;
            case DELETE:
                try {
                    firebaseManager.deleteBlock(farmId, block.getId());
                    Log.d(TAG, "Block deleted from Firestore: " + block.getBlockName());
                } catch (Exception e) {
                    Log.e(TAG, "Firestore deleteBlock failed", e);
                    throw e;
                }
                break;
        }
    }

    private void syncExpense(SyncEntity syncItem) throws Exception {
        ExpenseEntity expense = database.expenseDao().getExpenseById(syncItem.getEntityId());
        if (expense == null) {
            Log.e(TAG, "Expense not found in local DB: " + syncItem.getEntityId());
            return;
        }

        Log.d(TAG, "Syncing expense ID: " + syncItem.getEntityId() + " action: " + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveExpense(expense, farmId);
                Log.d(TAG, "Expense saved to Firestore: " + expense.getId());
                break;
            case DELETE:
                firebaseManager.deleteExpense(farmId, expense.getId());
                Log.d(TAG, "Expense deleted from Firestore: " + expense.getId());
                break;
        }
    }

    private void syncRevenue(SyncEntity syncItem) throws Exception {
        RevenueEntity revenue = database.revenueDao().getRevenueById(syncItem.getEntityId());
        if (revenue == null) {
            Log.e(TAG, "Revenue not found in local DB: " + syncItem.getEntityId());
            return;
        }

        Log.d(TAG, "Syncing revenue ID: " + syncItem.getEntityId() + " action: " + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveRevenue(revenue, farmId);
                Log.d(TAG, "Revenue saved to Firestore: " + revenue.getId());
                break;
            case DELETE:
                firebaseManager.deleteRevenue(farmId, revenue.getId());
                Log.d(TAG, "Revenue deleted from Firestore: " + revenue.getId());
                break;
        }
    }

    private void syncTask(SyncEntity syncItem) throws Exception {
        TaskEntity task = database.taskDao().getTaskById(syncItem.getEntityId());
        if (task == null) {
            Log.e(TAG, "Task not found in local DB: " + syncItem.getEntityId());
            return;
        }

        Log.d(TAG, "Syncing task ID: " + syncItem.getEntityId() + " action: " + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveTask(task, farmId);
                Log.d(TAG, "Task saved to Firestore: " + task.getId());
                break;
            case DELETE:
                firebaseManager.deleteTask(farmId, task.getId());
                Log.d(TAG, "Task deleted from Firestore: " + task.getId());
                break;
        }
    }

    private void syncPhoto(SyncEntity syncItem) throws Exception {
        PhotoEntity photo = database.photoDao().getPhotoById(syncItem.getEntityId());
        if (photo == null) {
            Log.e(TAG, "Photo not found in local DB: " + syncItem.getEntityId());
            syncItem.setStatus(SyncEntity.SyncStatus.COMPLETED);
            database.syncDao().update(syncItem);
            return;
        }

        File photoFile = new File(photo.getFilePath());
        Log.d(TAG, "📸 Photo file path: " + photo.getFilePath());
        Log.d(TAG, "📸 File exists: " + photoFile.exists());

        if (!photoFile.exists()) {
            Log.e(TAG, "Photo file not found: " + photo.getFilePath());
            syncItem.setStatus(SyncEntity.SyncStatus.COMPLETED);
            database.syncDao().update(syncItem);
            return;
        }

        Log.d(TAG, "📸 File size: " + photoFile.length() + " bytes");

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMessage = {null};

        // FIXED: Pass all 7 required arguments
        firebaseManager.uploadPhoto(
                photoFile,                                    // File photoFile
                String.valueOf(photo.getId()),                // String photoId
                farmId,                                       // String farmId
                photo.getBlockId(),                           // Long blockId
                photo.getTaskId(),                            // Long taskId
                photo.getExpenseId(),                         // Long expenseId
                new FirebaseManager.PhotoUploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        Log.d(TAG, "✅ Photo uploaded successfully to Storage. URL: " + downloadUrl);

                        // Save metadata to Firestore
                        firebaseManager.savePhotoMetadata(photo, farmId, downloadUrl,
                                new FirebaseManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "✅ Photo metadata saved to Firestore");
                                        success[0] = true;
                                        latch.countDown();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "❌ Failed to save photo metadata: " + error);
                                        errorMessage[0] = error;
                                        latch.countDown();
                                    }
                                });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Photo upload failed: " + error);
                        errorMessage[0] = error;
                        latch.countDown();
                    }

                    @Override
                    public void onProgress(double progress) {
                        Log.d(TAG, "📤 Photo upload progress: " + (int)progress + "%");
                    }
                });

        try {
            if (!latch.await(2, TimeUnit.MINUTES)) {
                Log.e(TAG, "❌ Photo upload timed out after 2 minutes");
                throw new RuntimeException("Photo upload timed out");
            }
            if (!success[0]) {
                throw new RuntimeException("Photo upload failed: " +
                        (errorMessage[0] != null ? errorMessage[0] : "Unknown error"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "❌ Photo upload interrupted", e);
            throw new RuntimeException("Photo upload interrupted", e);
        }
    }
}