package com.afrinuts.farmos.data.sync;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.data.remote.FirestoreHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;

public class SyncService extends Service {
    private static final String TAG = "SyncService";
    public static final String ACTION_SYNC_COMPLETE = "com.afrinuts.farmos.SYNC_COMPLETE";
    public static final String ACTION_SYNC_ERROR = "com.afrinuts.farmos.SYNC_ERROR";
    public static final String EXTRA_SYNC_STATUS = "sync_status";
    public static final String EXTRA_ERROR_MESSAGE = "error_message";

    private final IBinder binder = new LocalBinder();
    private HandlerThread syncThread;
    private Handler syncHandler;
    private Handler mainHandler;
    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private FirestoreHelper firestoreHelper;
    private AtomicBoolean isSyncing = new AtomicBoolean(false);
    private String currentFarmId;

    public class LocalBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.getInstance(this);
        firebaseManager = FirebaseManager.getInstance();
        // Initialize FirestoreHelper with 0L, will update when farmId is set
        firestoreHelper = new FirestoreHelper(0L);
        mainHandler = new Handler(Looper.getMainLooper());

        // Create background thread for sync operations
        syncThread = new HandlerThread("SyncThread");
        syncThread.start();
        syncHandler = new Handler(syncThread.getLooper());

        Log.d(TAG, "SyncService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (syncThread != null) {
            syncThread.quitSafely();
        }
        Log.d(TAG, "SyncService destroyed");
    }

    /**
     * Start full sync for a farm
     */
    public void startFullSync(String farmId) {
        if (isSyncing.get()) {
            Log.d(TAG, "Sync already in progress");
            broadcastSyncStatus("Sync already in progress");
            return;
        }

        this.currentFarmId = farmId;
        this.firestoreHelper = new FirestoreHelper(Long.parseLong(farmId));
        isSyncing.set(true);

        syncHandler.post(() -> performFullSync());
    }

    /**
     * Perform the actual sync operation
     */
    private void performFullSync() {
        Log.d(TAG, "Starting full sync for farm: " + currentFarmId);
        broadcastSyncStatus("Starting sync...");

        try {
            // Step 1: Upload local changes to Firebase
            broadcastSyncStatus("Uploading blocks...");
            uploadBlocks();

            broadcastSyncStatus("Uploading expenses...");
            uploadExpenses();

            broadcastSyncStatus("Uploading revenues...");
            uploadRevenues();

            broadcastSyncStatus("Uploading tasks...");
            uploadTasks();

            broadcastSyncStatus("Uploading photos...");
            uploadPhotos();

            // Step 2: Download remote changes
            broadcastSyncStatus("Downloading blocks...");
            downloadBlocks();

            broadcastSyncStatus("Downloading expenses...");
            downloadExpenses();

            broadcastSyncStatus("Downloading revenues...");
            downloadRevenues();

            broadcastSyncStatus("Downloading tasks...");
            downloadTasks();

            broadcastSyncStatus("Downloading photos...");
            downloadPhotos();

            // Step 3: Update last sync timestamp
            updateLastSyncTimestamp();

            broadcastSyncComplete("Sync completed successfully");
            Log.d(TAG, "Full sync completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
            broadcastSyncError("Sync failed: " + e.getMessage());
        } finally {
            isSyncing.set(false);
        }
    }

    /**
     * Upload all blocks for current farm
     */
    private void uploadBlocks() {
        List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(Long.parseLong(currentFarmId));
        for (BlockEntity block : blocks) {
            firestoreHelper.uploadBlock(block);
        }
    }

    /**
     * Upload all expenses for current farm
     */
    private void uploadExpenses() {
        List<ExpenseEntity> expenses = database.expenseDao().getExpensesByFarmId(Long.parseLong(currentFarmId));
        for (ExpenseEntity expense : expenses) {
            firestoreHelper.uploadExpense(expense);
        }
    }

    /**
     * Upload all revenues for current farm
     */
    private void uploadRevenues() {
        List<RevenueEntity> revenues = database.revenueDao().getRevenuesByFarmId(Long.parseLong(currentFarmId));
        for (RevenueEntity revenue : revenues) {
            firestoreHelper.uploadRevenue(revenue);
        }
    }

    /**
     * Upload all tasks for current farm
     */
    private void uploadTasks() {
        List<TaskEntity> tasks = database.taskDao().getTasksByFarmId(Long.parseLong(currentFarmId));
        for (TaskEntity task : tasks) {
            firestoreHelper.uploadTask(task);
        }
    }

    /**
     * Upload all photos for current farm
     */
    private void uploadPhotos() {
        List<PhotoEntity> photos = database.photoDao().getPhotosByFarmId(Long.parseLong(currentFarmId));
        CountDownLatch latch = new CountDownLatch(photos.size());

        for (PhotoEntity photo : photos) {
            File photoFile = new File(photo.getFilePath());
            if (!photoFile.exists()) {
                Log.w(TAG, "Photo file not found: " + photo.getFilePath());
                latch.countDown();
                continue;
            }

            // FIXED: Pass all 7 required arguments
            firebaseManager.uploadPhoto(
                    photoFile,                              // File photoFile
                    String.valueOf(photo.getId()),          // String photoId
                    currentFarmId,                          // String farmId
                    photo.getBlockId(),                     // Long blockId (can be null)
                    photo.getTaskId(),                      // Long taskId (can be null)
                    photo.getExpenseId(),                   // Long expenseId (can be null)
                    new FirebaseManager.PhotoUploadCallback() {
                        @Override
                        public void onSuccess(String downloadUrl) {
                            Log.d(TAG, "✅ Photo uploaded: " + downloadUrl);
                            // Save metadata with cloud URL
                            firebaseManager.savePhotoMetadata(photo, currentFarmId, downloadUrl);
                            latch.countDown();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ Error uploading photo: " + error);
                            latch.countDown();
                        }

                        @Override
                        public void onProgress(double progress) {
                            broadcastSyncStatus("Uploading photos: " + (int) progress + "%");
                        }
                    });
        }

        try {
            latch.await(5, TimeUnit.MINUTES);
            Log.d(TAG, "Photo upload batch completed");
        } catch (InterruptedException e) {
            Log.e(TAG, "Photo upload interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
    /**
     * Download blocks from Firebase and update local DB
     */
    private void downloadBlocks() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        firebaseManager.getBlocksForFarm(currentFarmId)
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BlockEntity block = documentToBlock(doc);
                        if (block != null) {
                            BlockEntity existing = database.blockDao().getBlockById(block.getId());
                            if (existing == null) {
                                database.blockDao().insert(block);
                            } else {
                                database.blockDao().update(block);
                            }
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    error[0] = e;
                    latch.countDown();
                });

        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new Exception("Timeout downloading blocks");
        }
        if (error[0] != null) {
            throw error[0];
        }
    }

    /**
     * Convert Firebase document to BlockEntity
     */
    private BlockEntity documentToBlock(DocumentSnapshot doc) {
        try {
            Long id = doc.getLong("id");
            Long farmId = doc.getLong("farmId");
            String blockName = doc.getString("blockName");
            Double hectareSize = doc.getDouble("hectareSize");
            String status = doc.getString("status");
            Long clearedDate = doc.getLong("clearedDate");
            Long plowedDate = doc.getLong("plowedDate");
            Long plantedDate = doc.getLong("plantedDate");
            Double survivalRate = doc.getDouble("survivalRate");
            Long replacementCount = doc.getLong("replacementCount");
            String notes = doc.getString("notes");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");

            if (id == null || farmId == null || blockName == null) return null;

            BlockEntity block = new BlockEntity(farmId, blockName,
                    hectareSize != null ? hectareSize : 1.0, notes);
            block.setId(id);
            block.setStatus(BlockEntity.BlockStatus.valueOf(status));
            block.setClearedDate(clearedDate);
            block.setPlowedDate(plowedDate);
            block.setPlantedDate(plantedDate);
            block.setSurvivalRate(survivalRate != null ? survivalRate : 0);
            block.setReplacementCount(replacementCount != null ? replacementCount.intValue() : 0);
            block.setCreatedAt(createdAt != null ? createdAt : 0);
            block.setUpdatedAt(updatedAt != null ? updatedAt : 0);

            return block;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to BlockEntity", e);
            return null;
        }
    }

    /**
     * Download expenses from Firebase
     */
    private void downloadExpenses() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        firebaseManager.getExpensesForFarm(currentFarmId)
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ExpenseEntity expense = documentToExpense(doc);
                        if (expense != null) {
                            ExpenseEntity existing = database.expenseDao().getExpenseById(expense.getId());
                            if (existing == null) {
                                database.expenseDao().insert(expense);
                            } else {
                                database.expenseDao().update(expense);
                            }
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    error[0] = e;
                    latch.countDown();
                });

        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new Exception("Timeout downloading expenses");
        }
        if (error[0] != null) throw error[0];
    }

    /**
     * Convert Firebase document to ExpenseEntity
     */
    private ExpenseEntity documentToExpense(DocumentSnapshot doc) {
        try {
            Long id = doc.getLong("id");
            Long farmId = doc.getLong("farmId");
            Long blockId = doc.getLong("blockId");
            String category = doc.getString("category");
            Double amount = doc.getDouble("amount");
            Long date = doc.getLong("date");
            String description = doc.getString("description");
            String receiptPath = doc.getString("receiptPath");
            Long createdAt = doc.getLong("createdAt");

            if (id == null || farmId == null || category == null || amount == null || date == null) {
                return null;
            }

            ExpenseEntity expense = new ExpenseEntity(
                    farmId,
                    blockId,
                    ExpenseEntity.ExpenseCategory.valueOf(category),
                    amount,
                    date,
                    description
            );
            expense.setId(id);
            expense.setReceiptPath(receiptPath);
            expense.setCreatedAt(createdAt != null ? createdAt : 0);

            return expense;

        } catch (Exception e) {
            Log.e(TAG, "Error mapping expense", e);
            return null;
        }
    }

    /**
     * Download revenues from Firebase
     */
    private void downloadRevenues() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        firebaseManager.getRevenuesForFarm(currentFarmId)
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        RevenueEntity revenue = documentToRevenue(doc);
                        if (revenue != null) {
                            RevenueEntity existing = database.revenueDao().getRevenueById(revenue.getId());
                            if (existing == null) {
                                database.revenueDao().insert(revenue);
                            } else {
                                database.revenueDao().update(revenue);
                            }
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    error[0] = e;
                    latch.countDown();
                });

        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new Exception("Timeout downloading revenues");
        }
        if (error[0] != null) throw error[0];
    }

    /**
     * Convert Firebase document to RevenueEntity
     */
    private RevenueEntity documentToRevenue(DocumentSnapshot doc) {
        try {
            Long id = doc.getLong("id");
            Long farmId = doc.getLong("farmId");
            Long blockId = doc.getLong("blockId");
            Long harvestDate = doc.getLong("harvestDate");
            Double quantityKg = doc.getDouble("quantityKg");
            Double pricePerKg = doc.getDouble("pricePerKg");
            Double totalAmount = doc.getDouble("totalAmount");
            String buyer = doc.getString("buyer");
            String quality = doc.getString("quality");
            String notes = doc.getString("notes");
            Long createdAt = doc.getLong("createdAt");

            if (id == null || farmId == null || harvestDate == null ||
                    quantityKg == null || pricePerKg == null || quality == null) {
                return null;
            }

            RevenueEntity.QualityGrade grade;
            try {
                grade = RevenueEntity.QualityGrade.valueOf(quality.toUpperCase());
            } catch (Exception e) {
                grade = RevenueEntity.QualityGrade.STANDARD;
            }

            RevenueEntity revenue = new RevenueEntity(
                    farmId,
                    blockId,
                    harvestDate,
                    quantityKg,
                    pricePerKg,
                    buyer,
                    grade,
                    notes
            );
            revenue.setId(id);
            revenue.setCreatedAt(createdAt != null ? createdAt : 0);

            // totalAmount is calculated automatically, but we can set it if needed
            // revenue.setTotalAmount(totalAmount);

            return revenue;

        } catch (Exception e) {
            Log.e(TAG, "Error mapping revenue", e);
            return null;
        }
    }
    /**
     * Download tasks from Firebase
     */
    private void downloadTasks() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        firebaseManager.getTasksForFarm(currentFarmId)
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        TaskEntity task = documentToTask(doc);
                        if (task != null) {
                            TaskEntity existing = database.taskDao().getTaskById(task.getId());
                            if (existing == null) {
                                database.taskDao().insertTask(task);
                            } else {
                                database.taskDao().updateTask(task);
                            }
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    error[0] = e;
                    latch.countDown();
                });

        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new Exception("Timeout downloading tasks");
        }
        if (error[0] != null) {
            throw error[0];
        }
    }

    /**
     * Convert Firebase document to TaskEntity
     */
    private TaskEntity documentToTask(DocumentSnapshot doc) {
        try {
            Long id = doc.getLong("id");
            Long farmId = doc.getLong("farmId");
            Long blockId = doc.getLong("blockId");
            String title = doc.getString("title");
            String description = doc.getString("description");
            String status = doc.getString("status");
            String type = doc.getString("type");
            Long dueDate = doc.getLong("dueDate");
            Long completedDate = doc.getLong("completedDate");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");

            if (id == null || farmId == null || title == null ||
                    status == null || type == null || dueDate == null) {
                return null;
            }

            TaskEntity task = new TaskEntity(
                    farmId,
                    blockId,
                    title,
                    description,
                    TaskEntity.TaskType.valueOf(type),
                    TaskEntity.TaskStatus.valueOf(status),
                    dueDate
            );
            task.setId(id);
            task.setCompletedDate(completedDate);
            task.setCreatedAt(createdAt != null ? createdAt : 0);
            task.setUpdatedAt(updatedAt != null ? updatedAt : 0);

            return task;

        } catch (Exception e) {
            Log.e(TAG, "Error converting task", e);
            return null;
        }
    }

    /**
     * Download photos from Firebase
     */
    private void downloadPhotos() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        firebaseManager.getPhotosForFarm(currentFarmId)
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        PhotoEntity photo = documentToPhoto(doc);
                        if (photo != null) {
                            PhotoEntity existing = database.photoDao().getPhotoById(photo.getId());
                            if (existing == null) {
                                database.photoDao().insert(photo);
                            } else {
                                database.photoDao().update(photo);
                            }
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    error[0] = e;
                    latch.countDown();
                });

        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new Exception("Timeout downloading photos");
        }
        if (error[0] != null) throw error[0];
    }

    /**
     * Convert Firebase document to PhotoEntity
     */
    private PhotoEntity documentToPhoto(DocumentSnapshot doc) {
        try {
            Long id = doc.getLong("id");
            Long farmId = doc.getLong("farmId");
            Long blockId = doc.getLong("blockId");
            Long taskId = doc.getLong("taskId");
            Long expenseId = doc.getLong("expenseId");
            String filePath = doc.getString("filePath");
            String caption = doc.getString("caption");
            Long takenAt = doc.getLong("takenAt");
            Long createdAt = doc.getLong("createdAt");

            if (id == null || farmId == null || filePath == null || takenAt == null) {
                return null;
            }

            PhotoEntity photo = new PhotoEntity(
                    farmId,
                    blockId,
                    taskId,
                    expenseId,
                    filePath,
                    caption,
                    takenAt
            );
            photo.setId(id);
            photo.setCreatedAt(createdAt != null ? createdAt : 0);

            return photo;

        } catch (Exception e) {
            Log.e(TAG, "Error mapping photo", e);
            return null;
        }
    }

    /**
     * Update last sync timestamp in SharedPreferences
     */
    private void updateLastSyncTimestamp() {
        getSharedPreferences("sync", MODE_PRIVATE)
                .edit()
                .putLong("last_sync_" + currentFarmId, System.currentTimeMillis())
                .apply();
    }

    /**
     * Broadcast sync status update
     */
    private void broadcastSyncStatus(String status) {
        Intent intent = new Intent(ACTION_SYNC_COMPLETE);
        intent.putExtra(EXTRA_SYNC_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Broadcast sync completion
     */
    private void broadcastSyncComplete(String message) {
        Intent intent = new Intent(ACTION_SYNC_COMPLETE);
        intent.putExtra(EXTRA_SYNC_STATUS, message);
        intent.putExtra("completed", true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Broadcast sync error
     */
    private void broadcastSyncError(String error) {
        Intent intent = new Intent(ACTION_SYNC_ERROR);
        intent.putExtra(EXTRA_ERROR_MESSAGE, error);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Check if sync is currently running
     */
    public boolean isSyncing() {
        return isSyncing.get();
    }

    /**
     * Get last sync timestamp
     */
    public long getLastSyncTime(String farmId) {
        return getSharedPreferences("sync", MODE_PRIVATE)
                .getLong("last_sync_" + farmId, 0);
    }
}