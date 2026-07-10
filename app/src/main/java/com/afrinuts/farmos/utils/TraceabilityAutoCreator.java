package com.afrinuts.farmos.utils;

import android.content.Context;
import android.util.Log;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceabilityAutoCreator {

    private static final String TAG = "TraceabilityAutoCreator";
    private final Context context;
    private final AppDatabase database;
    private final SyncManager syncManager;
    private final FirestoreSyncManager firestoreSyncManager;
    private final long farmId;

    public TraceabilityAutoCreator(Context context, long farmId) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    /**
     * ✅ Check all completed batches and create traceability for those missing it
     */
    public void createMissingTraceabilities() {
        new Thread(() -> {
            try {
                Log.d(TAG, "Checking for missing traceabilities...");

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
                            createTraceability(batch);
                            created++;
                        }
                    }
                }

                Log.d(TAG, "✅ Created " + created + " new traceability records");

            } catch (Exception e) {
                Log.e(TAG, "Error creating missing traceabilities", e);
            }
        }).start();
    }

    /**
     * ✅ Create traceability for a specific batch
     */
    public void createTraceabilityForBatch(long batchId) {
        new Thread(() -> {
            try {
                QualityBatchEntity batch = database.qualityBatchDao().getBatchById(batchId);
                if (batch == null) {
                    Log.e(TAG, "Batch not found with ID: " + batchId);
                    return;
                }

                if (isBatchComplete(batch)) {
                    // Check if traceability already exists
                    BatchTraceabilityEntity existing = database.batchTraceabilityDao()
                            .getByQualityBatchId(batch.getId());

                    if (existing == null) {
                        createTraceability(batch);
                    } else {
                        Log.d(TAG, "Traceability already exists for batch: " + batch.getBatchNumber());
                    }
                } else {
                    Log.d(TAG, "Batch not complete for traceability: " + batch.getBatchNumber());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating traceability for batch", e);
            }
        }).start();
    }

    private boolean isBatchComplete(QualityBatchEntity batch) {
        // Check if batch has quality metrics
        boolean hasQualityMetrics = batch.getMoistureContent() > 0 ||
                batch.getNutCountPerKg() > 0 ||
                batch.getDefectiveRate() > 0 ||
                batch.getKernelRatio() > 0;

        // Check if batch has supplier
        boolean hasSupplier = batch.getSupplierId() != null && batch.getSupplierId() > 0;

        // Check if batch has quality grade
        boolean hasGrade = batch.getQualityGrade() != null;

        Log.d(TAG, "Batch complete check for " + batch.getBatchNumber() + ": " +
                "hasQualityMetrics=" + hasQualityMetrics +
                ", hasSupplier=" + hasSupplier +
                ", hasGrade=" + hasGrade);

        return hasQualityMetrics && hasSupplier && hasGrade;
    }

    private void createTraceability(QualityBatchEntity batch) {
        try {
            Log.d(TAG, "Creating traceability for batch: " + batch.getBatchNumber());

            // 1. Get supplier data
            String supplierName = null;
            if (batch.getSupplierId() != null) {
                try {
                    SupplierEntity supplier = database.supplierDao()
                            .getSupplierById(batch.getSupplierId());
                    if (supplier != null) {
                        supplierName = supplier.getName();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not get supplier name", e);
                }
            }

            // 2. ✅ Get revenue data (important fields)
            double pricePerKg = 0;
            double totalAmount = 0;
            String buyer = null;
            Long harvestCycleId = null;
            try {
                List<RevenueEntity> revenues = database.revenueDao()
                        .getRevenuesByBatchId(batch.getId());
                if (revenues != null && !revenues.isEmpty()) {
                    RevenueEntity revenue = revenues.get(0);
                    pricePerKg = revenue.getPricePerKg();
                    totalAmount = revenue.getTotalAmount();
                    buyer = revenue.getBuyer();
                    harvestCycleId = revenue.getId();
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not get revenue data", e);
            }

            // 3. ✅ Create sustainability metrics JSON
            String sustainabilityMetrics = createSustainabilityMetricsJson(batch);

            // 4. ✅ Check if traceability already exists
            BatchTraceabilityEntity existing = database.batchTraceabilityDao()
                    .getByQualityBatchId(batch.getId());

            if (existing != null) {
                Log.d(TAG, "Traceability already exists for batch: " + batch.getBatchNumber());
                // ✅ Update existing with latest data
                existing.setBuyer(buyer);
                existing.setHarvestCycleId(harvestCycleId);
                existing.setPricePerKg(pricePerKg);
                existing.setTotalAmount(totalAmount);
                existing.setSustainabilityMetrics(sustainabilityMetrics);
                existing.setUpdatedAt(System.currentTimeMillis());
                database.batchTraceabilityDao().update(existing);
                Log.d(TAG, "✅ Updated existing traceability with latest data");
                return;
            }

            // 5. Create traceability record
            BatchTraceabilityEntity traceability = new BatchTraceabilityEntity(
                    batch.getFarmId(),
                    batch.getBatchNumber(),
                    batch.getProcessingDate(),
                    batch.getWeightKg(),
                    batch.getQualityGrade().getDisplayName(),
                    pricePerKg
            );

            // Set all fields
            traceability.setQualityBatchId(batch.getId());
            traceability.setSupplierId(batch.getSupplierId());
            traceability.setSupplierName(supplierName != null ? supplierName : "Unknown Supplier");
            traceability.setQualityGrade(batch.getQualityGrade().getDisplayName());

            // ✅ Set revenue fields
            traceability.setHarvestCycleId(harvestCycleId);
            traceability.setBuyer(buyer);
            traceability.setPricePerKg(pricePerKg);
            traceability.setTotalAmount(totalAmount);
            traceability.setGrade(batch.getQualityGrade().getDisplayName());

            // ✅ Set sustainability metrics
            traceability.setSustainabilityMetrics(sustainabilityMetrics);

            // ✅ Generate traceability URL
            traceability.setTraceabilityUrl(
                    traceability.generateTraceabilityUrl(batch.getBatchNumber()));

            // Save to database
            long id = database.batchTraceabilityDao().insert(traceability);
            Log.d(TAG, "✅ Created traceability with ID: " + id + " for batch: " + batch.getBatchNumber());

            // Queue for sync
            if (!firestoreSyncManager.isRemoteUpdate()) {
                syncManager.queueForSync("batch_traceability", id, SyncEntity.SyncAction.CREATE);
                syncManager.triggerAutoSync();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error creating traceability", e);
        }
    }

    private String createSustainabilityMetricsJson(QualityBatchEntity batch) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("noChildLabor", batch.isNoChildLabor());
        metrics.put("farmOsMonitored", batch.isFarmOsMonitored());
        metrics.put("firebreakProtection", batch.isFirebreakProtection());
        metrics.put("sustainableHarvesting", batch.isSustainableHarvesting());
        metrics.put("verifiedAt", batch.getVerifiedAt());
        metrics.put("qualityScore", batch.getQualityScore());
        return new Gson().toJson(metrics);
    }

    /**
     * ✅ Update existing traceability with latest data
     */
    public void updateExistingTraceability(long traceabilityId) {
        new Thread(() -> {
            try {
                BatchTraceabilityEntity traceability = database.batchTraceabilityDao()
                        .getById(traceabilityId);
                if (traceability == null) {
                    Log.e(TAG, "Traceability not found: " + traceabilityId);
                    return;
                }

                // Get quality batch
                QualityBatchEntity batch = database.qualityBatchDao()
                        .getBatchById(traceability.getQualityBatchId());
                if (batch == null) {
                    Log.e(TAG, "Quality batch not found");
                    return;
                }

                // Get revenue data
                double pricePerKg = 0;
                double totalAmount = 0;
                String buyer = null;
                Long harvestCycleId = null;
                try {
                    List<RevenueEntity> revenues = database.revenueDao()
                            .getRevenuesByBatchId(batch.getId());
                    if (revenues != null && !revenues.isEmpty()) {
                        RevenueEntity revenue = revenues.get(0);
                        pricePerKg = revenue.getPricePerKg();
                        totalAmount = revenue.getTotalAmount();
                        buyer = revenue.getBuyer();
                        harvestCycleId = revenue.getId();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not get revenue data", e);
                }

                // Update fields
                traceability.setPricePerKg(pricePerKg);
                traceability.setTotalAmount(totalAmount);
                traceability.setBuyer(buyer);
                traceability.setHarvestCycleId(harvestCycleId);
                traceability.setUpdatedAt(System.currentTimeMillis());

                database.batchTraceabilityDao().update(traceability);
                Log.d(TAG, "Updated existing traceability with revenue data");

                // Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("batch_traceability", traceability.getId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error updating existing traceability", e);
            }
        }).start();
    }
}