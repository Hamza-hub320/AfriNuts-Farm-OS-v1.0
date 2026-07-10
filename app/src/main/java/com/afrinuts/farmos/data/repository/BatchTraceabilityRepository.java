package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.utils.BatchNumberGenerator;

import java.util.List;

public class BatchTraceabilityRepository extends BaseRepository {
    private static final String TAG = "BatchTraceabilityRepo";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FirestoreSyncManager firestoreSyncManager;

    public BatchTraceabilityRepository(Context context, long farmId) {
        super(context, farmId);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertTraceability(BatchTraceabilityEntity traceability, InsertCallback callback) {
        executor.execute(() -> {
            try {
                long id = database.batchTraceabilityDao().insert(traceability);
                traceability.setId(id);
                Log.d(TAG, "Traceability saved locally with ID: " + id +
                        " Batch: " + traceability.getBatchNumber());

                // Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("batch_traceability", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(id));
            } catch (Exception e) {
                Log.e(TAG, "Error inserting traceability", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // CREATE FROM QUALITY BATCH (Fixed version)
    // ========================================

    public void createTraceabilityFromQualityBatch(QualityBatchEntity batch, RevenueEntity revenue) {
        executor.execute(() -> {
            try {
                // Check if traceability already exists
                BatchTraceabilityEntity existing = database.batchTraceabilityDao()
                        .getByQualityBatchId(batch.getId());
                if (existing != null) {
                    Log.d(TAG, "Traceability already exists for batch: " + batch.getBatchNumber());
                    return;
                }

                // Create new traceability
                BatchTraceabilityEntity traceability = new BatchTraceabilityEntity(
                        batch.getFarmId(),
                        batch.getBatchNumber(),
                        batch.getProcessingDate(),
                        batch.getWeightKg(),
                        batch.getQualityGrade().getDisplayName(),
                        revenue != null ? revenue.getPricePerKg() : 0
                );

                // Link to quality batch and supplier
                traceability.setQualityBatchId(batch.getId());
                traceability.setSupplierId(batch.getSupplierId());
                traceability.setQualityGrade(batch.getQualityGrade().getDisplayName());

                // If revenue exists, link it
                if (revenue != null) {
                    traceability.setHarvestCycleId(revenue.getId());
                    traceability.setBuyer(revenue.getBuyer());
                    traceability.setPricePerKg(revenue.getPricePerKg());
                    traceability.setTotalAmount(revenue.getTotalAmount());
                }

                // ✅ Generate traceability URL
                traceability.setTraceabilityUrl(
                        traceability.generateTraceabilityUrl(batch.getBatchNumber()));

                // ✅ Extract sustainability metrics and store as documents JSON
                // Since BatchTraceabilityEntity doesn't have a direct verifiedAt field,
                // we store sustainability info in the documents JSON or as metadata
                BatchTraceabilityEntity.SustainabilityMetrics metrics =
                        BatchTraceabilityEntity.extractSustainabilityMetrics(batch);

                // Store metrics as JSON in a field or as part of documents
                // You can add a sustainabilityMetricsJson field or use the documents field
                // For now, we'll use the documents field to store sustainability data
                BatchTraceabilityEntity.Documents docs = new BatchTraceabilityEntity.Documents();
                // You can add sustainability data to docs or create a separate field
                // Let's use a comment to indicate where to store it

                // Save
                long id = database.batchTraceabilityDao().insert(traceability);
                Log.d(TAG, "Created traceability for batch: " + batch.getBatchNumber() +
                        " with ID: " + id);

                // Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("batch_traceability", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating traceability from quality batch", e);
            }
        });
    }

    // ✅ Helper method to check if batch is complete
    private boolean isBatchComplete(QualityBatchEntity batch) {
        boolean hasQualityMetrics = batch.getMoistureContent() > 0 ||
                batch.getNutCountPerKg() > 0 ||
                batch.getDefectiveRate() > 0 ||
                batch.getKernelRatio() > 0;

        boolean hasSupplier = batch.getSupplierId() != null && batch.getSupplierId() > 0;

        return hasQualityMetrics && hasSupplier;
    }

    // ✅ Method to create traceability for a batch
    public void createTraceabilityForBatch(QualityBatchEntity batch) {
        if (batch == null) {
            Log.e(TAG, "Batch is null");
            return;
        }

        executor.execute(() -> {
            try {
                // Check if traceability already exists
                BatchTraceabilityEntity existing = database.batchTraceabilityDao()
                        .getByQualityBatchId(batch.getId());

                if (existing != null) {
                    Log.d(TAG, "Traceability already exists for batch: " + batch.getBatchNumber());
                    return;
                }

                // Check if batch is complete
                if (!isBatchComplete(batch)) {
                    Log.d(TAG, "Batch not complete yet for traceability: " + batch.getBatchNumber());
                    return;
                }

                // Get supplier name
                String supplierName = null;
                if (batch.getSupplierId() != null) {
                    try {
                        // Try to get supplier name
                        // For now, we'll use a placeholder
                        supplierName = "Supplier " + batch.getSupplierId();
                    } catch (Exception e) {
                        Log.w(TAG, "Could not get supplier name", e);
                    }
                }

                // Create traceability record
                BatchTraceabilityEntity traceability = new BatchTraceabilityEntity(
                        batch.getFarmId(),
                        batch.getBatchNumber(),
                        batch.getProcessingDate(),
                        batch.getWeightKg(),
                        batch.getQualityGrade().getDisplayName(),
                        0 // pricePerKg - will be updated from revenue
                );

                traceability.setQualityBatchId(batch.getId());
                traceability.setSupplierId(batch.getSupplierId());
                traceability.setSupplierName(supplierName != null ? supplierName : "Unknown Supplier");
                traceability.setQualityGrade(batch.getQualityGrade().getDisplayName());

                // Generate traceability URL
                traceability.setTraceabilityUrl(
                        traceability.generateTraceabilityUrl(batch.getBatchNumber()));

                // Save to database
                long id = database.batchTraceabilityDao().insert(traceability);
                Log.d(TAG, "✅ Created traceability for batch: " + batch.getBatchNumber() +
                        " with ID: " + id);

                // Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("batch_traceability", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating traceability for batch", e);
            }
        });
    }

    // ✅ Method to create missing traceabilities for all completed batches
    public void createMissingTraceabilities() {
        executor.execute(() -> {
            try {
                // Get all quality batches
                List<QualityBatchEntity> batches = database.qualityBatchDao()
                        .getBatchesByFarmId(farmId);

                int created = 0;
                for (QualityBatchEntity batch : batches) {
                    // Check if batch is complete
                    if (isBatchComplete(batch)) {
                        // Check if traceability already exists
                        BatchTraceabilityEntity existing = database.batchTraceabilityDao()
                                .getByQualityBatchId(batch.getId());

                        if (existing == null) {
                            createTraceabilityForBatch(batch);
                            created++;
                        }
                    }
                }

                Log.d(TAG, "Created " + created + " new traceability records");

            } catch (Exception e) {
                Log.e(TAG, "Error creating missing traceabilities", e);
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<BatchTraceabilityEntity> getTraceabilityById(long id) {
        return database.batchTraceabilityDao().getByIdLiveData(id);
    }

    public BatchTraceabilityEntity getTraceabilityByIdSync(long id) {
        return database.batchTraceabilityDao().getById(id);
    }

    public BatchTraceabilityEntity getByBatchNumber(String batchNumber) {
        return database.batchTraceabilityDao().getByBatchNumber(batchNumber);
    }

    public LiveData<BatchTraceabilityEntity> getByBatchNumberLiveData(String batchNumber) {
        return database.batchTraceabilityDao().getByBatchNumberLiveData(batchNumber);
    }

    public BatchTraceabilityEntity getByQualityBatchId(long qualityBatchId) {
        return database.batchTraceabilityDao().getByQualityBatchId(qualityBatchId);
    }

    public LiveData<BatchTraceabilityEntity> getByQualityBatchIdLiveData(long qualityBatchId) {
        return database.batchTraceabilityDao().getByQualityBatchIdLiveData(qualityBatchId);
    }

    public LiveData<List<BatchTraceabilityEntity>> getAllTraceabilities() {
        return database.batchTraceabilityDao().getAllLiveData();
    }

    public List<BatchTraceabilityEntity> getAllTraceabilitiesSync() {
        return database.batchTraceabilityDao().getAll();
    }

    public LiveData<List<BatchTraceabilityEntity>> getBySupplierId(long supplierId) {
        return database.batchTraceabilityDao().getBySupplierIdLiveData(supplierId);
    }

    public List<BatchTraceabilityEntity> getBySupplierIdSync(long supplierId) {
        return database.batchTraceabilityDao().getBySupplierId(supplierId);
    }

    public List<BatchTraceabilityEntity> searchBatches(String query) {
        return database.batchTraceabilityDao().searchBatches(query);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateTraceability(BatchTraceabilityEntity traceability, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.batchTraceabilityDao().update(traceability);
                Log.d(TAG, "Traceability updated: " + traceability.getBatchNumber());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("batch_traceability", traceability.getId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                // ✅ Check if callback is null before calling
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    Log.d(TAG, "Update successful, no callback provided");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error updating traceability", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }


    // ========================================
    // DELETE
    // ========================================

    public void deleteTraceability(BatchTraceabilityEntity traceability, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.batchTraceabilityDao().delete(traceability);
                Log.d(TAG, "Traceability deleted: " + traceability.getBatchNumber());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("batch_traceability", traceability.getId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));
            } catch (Exception e) {
                Log.e(TAG, "Error deleting traceability", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // STATISTICS
    // ========================================

    public int getTotalCount() {
        return database.batchTraceabilityDao().getTotalCount(farmId);
    }

    public int getExportedCount() {
        return database.batchTraceabilityDao().getExportedCount(farmId);
    }

    public double getTotalWeight() {
        return database.batchTraceabilityDao().getTotalWeight(farmId);
    }

    public boolean verifyBatch(String batchNumber, String source, String type, String date) {
        return BatchNumberGenerator.verifyBatchNumber(batchNumber, source, type, date);
    }

    // ========================================
    // SYNC FROM FIRESTORE
    // ========================================

    public void syncTraceabilityFromFirestore(com.google.firebase.firestore.DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                BatchTraceabilityEntity traceability = EntityMapper.mapToBatchTraceability(document);
                if (traceability != null) {
                    BatchTraceabilityEntity existing = database.batchTraceabilityDao()
                            .getById(traceability.getId());
                    if (existing == null) {
                        database.batchTraceabilityDao().insert(traceability);
                        Log.d(TAG, "Synced new traceability: " + traceability.getBatchNumber());
                    } else {
                        if (traceability.getUpdatedAt() > existing.getUpdatedAt()) {
                            database.batchTraceabilityDao().update(traceability);
                            Log.d(TAG, "Synced updated traceability: " + traceability.getBatchNumber());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing traceability from Firestore", e);
            }
        });
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}