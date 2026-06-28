package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QualityBatchRepository {
    private static final String TAG = "QualityBatchRepo";

    private final AppDatabase database;
    private final SyncManager syncManager;
    private final FirestoreSyncManager firestoreSyncManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final long farmId;

    public interface InsertCallback {
        void onSuccess(long id);
        void onError(String error);
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public QualityBatchEntity getBatchByIdSync(long batchId) {
        return database.qualityBatchDao().getBatchById(batchId);
    }

    public QualityBatchRepository(Context context, long farmId) {
        this.database = AppDatabase.getInstance(context);
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertBatch(QualityBatchEntity batch, InsertCallback callback) {
        executor.execute(() -> {
            try {
                long id = database.qualityBatchDao().insert(batch);
                batch.setId(id);
                Log.d(TAG, "Quality batch saved locally with ID: " + id);

                if (batch.getSupplierId() != null && batch.getSupplierId() > 0) {
                    createDeliveryFromBatch(batch);
                }

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("quality_batch", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                    Log.d(TAG, "Queued quality batch for sync");
                }

                mainHandler.post(() -> callback.onSuccess(id));

            } catch (Exception e) {
                Log.e(TAG, "Error inserting quality batch", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private void createDeliveryFromBatch(QualityBatchEntity batch) {
        try {
            SupplierEntity supplier = database.supplierDao().getSupplierById(batch.getSupplierId());
            if (supplier == null) {
                Log.e(TAG, "Supplier not found for batch: " + batch.getId());
                return;
            }

            double newVolume = supplier.getAnnualVolumeKg() + batch.getWeightKg();
            supplier.setAnnualVolumeKg(newVolume);

            double currentAvg = supplier.getAverageQualityScore();
            double oldVolume = supplier.getAnnualVolumeKg() - batch.getWeightKg();
            if (oldVolume > 0) {
                double newAvg = (currentAvg * oldVolume + batch.getQualityScore() * batch.getWeightKg()) / newVolume;
                supplier.setAverageQualityScore(newAvg);
            } else {
                supplier.setAverageQualityScore(batch.getQualityScore());
            }

            supplier.setUpdatedAt(System.currentTimeMillis());
            database.supplierDao().update(supplier);

            Log.d(TAG, "✅ Updated supplier metrics for: " + supplier.getName() +
                    " | New Volume: " + newVolume + "kg" +
                    " | Avg Quality: " + supplier.getAverageQualityScore());

        } catch (Exception e) {
            Log.e(TAG, "Error creating delivery from batch", e);
        }
    }

    private double calculateNewAverageQualityScore(SupplierEntity supplier, QualityBatchEntity batch) {
        double currentAvg = supplier.getAverageQualityScore();
        double currentVolume = supplier.getAnnualVolumeKg();
        double newVolume = batch.getWeightKg();

        if (currentVolume == 0) {
            return batch.getQualityScore();
        }

        return (currentAvg * currentVolume + batch.getQualityScore() * newVolume) /
                (currentVolume + newVolume);
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<QualityBatchEntity>> getAllBatches() {
        MutableLiveData<List<QualityBatchEntity>> liveData = new MutableLiveData<>();

        executor.execute(() -> {
            List<QualityBatchEntity> batches = database.qualityBatchDao().getBatchesByFarmId(farmId);
            for (QualityBatchEntity batch : batches) {
                if (batch.getSupplierId() != null) {
                    SupplierEntity supplier = database.supplierDao().getSupplierById(batch.getSupplierId());
                    if (supplier != null) {
                        batch.setSupplierName(supplier.getName());
                    }
                }
            }
            mainHandler.post(() -> liveData.setValue(batches));
        });

        return liveData;
    }

    public LiveData<QualityBatchEntity> getBatchById(long batchId) {
        return database.qualityBatchDao().getBatchByIdLiveData(batchId);
    }

    public LiveData<List<QualityBatchEntity>> getBatchesByBlockId(long blockId) {
        return database.qualityBatchDao().getBatchesByBlockIdLiveData(blockId);
    }

    public List<QualityBatchEntity> getBatchesByBlockIdSync(long blockId) {
        return database.qualityBatchDao().getBatchesByBlockId(blockId);
    }

    public List<QualityBatchEntity> getAllBatchesSync() {
        return database.qualityBatchDao().getBatchesByFarmId(farmId);
    }

    // Supplier-related batch queries
    public LiveData<List<QualityBatchEntity>> getBatchesBySupplierId(long supplierId) {
        return database.qualityBatchDao().getBatchesBySupplierIdLiveData(supplierId);
    }

    public List<QualityBatchEntity> getBatchesBySupplierIdSync(long supplierId) {
        return database.qualityBatchDao().getBatchesBySupplierIdSync(supplierId);
    }

    public QualityBatchEntity getBatchByNumberSync(String batchNumber) {
        return database.qualityBatchDao().getBatchByNumber(batchNumber);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateBatch(QualityBatchEntity batch, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.qualityBatchDao().update(batch);
                Log.d(TAG, "Quality batch updated locally: " + batch.getId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("quality_batch", batch.getId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error updating quality batch", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    // In QualityBatchRepository.java
    public void deleteBatch(QualityBatchEntity batch, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                // Delete from local database
                database.qualityBatchDao().delete(batch);
                Log.d(TAG, "Quality batch deleted locally: " + batch.getBatchNumber());

                // Queue for sync to Firestore
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("quality_batch", batch.getId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error deleting quality batch", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // ANALYTICS
    // ========================================

    public CompletableFuture<Double> getAverageMoisture() {
        return CompletableFuture.supplyAsync(() ->
                database.qualityBatchDao().getAverageMoisture(farmId), executor);
    }

    public CompletableFuture<Double> getAverageDefectiveRate() {
        return CompletableFuture.supplyAsync(() ->
                database.qualityBatchDao().getAverageDefectiveRate(farmId), executor);
    }

    public CompletableFuture<Double> getAverageKernelRatio() {
        return CompletableFuture.supplyAsync(() ->
                database.qualityBatchDao().getAverageKernelRatio(farmId), executor);
    }

    public CompletableFuture<Double> getAverageQualityScore() {
        return CompletableFuture.supplyAsync(() ->
                database.qualityBatchDao().getAverageQualityScore(farmId), executor);
    }

    public CompletableFuture<Integer> getTotalBatchCount() {
        return CompletableFuture.supplyAsync(() ->
                database.qualityBatchDao().getTotalBatchCount(farmId), executor);
    }

    public CompletableFuture<Double> getTotalProcessedWeight() {
        return CompletableFuture.supplyAsync(() ->
                database.qualityBatchDao().getTotalProcessedWeight(farmId), executor);
    }

    public CompletableFuture<List<QualityBatchEntity>> getLowQualityBatches() {
        return CompletableFuture.supplyAsync(() ->
                database.qualityBatchDao().getLowQualityBatches(farmId), executor);
    }

    public CompletableFuture<List<QualityBatchEntity>> getTopQualityBatches() {
        return CompletableFuture.supplyAsync(() ->
                database.qualityBatchDao().getTopQualityBatches(farmId), executor);
    }

    // ========================================
    // SYNC FROM FIRESTORE
    // ========================================

    public void syncBatchFromFirestore(DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                QualityBatchEntity batch = EntityMapper.mapToQualityBatch(document);
                if (batch != null) {
                    QualityBatchEntity existing = database.qualityBatchDao().getBatchById(batch.getId());
                    if (existing == null) {
                        database.qualityBatchDao().insert(batch);
                        Log.d(TAG, "Synced new quality batch: " + batch.getBatchNumber());
                    } else {
                        if (batch.getUpdatedAt() > existing.getUpdatedAt()) {
                            database.qualityBatchDao().update(batch);
                            Log.d(TAG, "Synced updated quality batch: " + batch.getBatchNumber());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing quality batch from Firestore", e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}