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
                    // ✅ SOP §4.5 + §4.6: skip uninspected and rejected batches
                    if (!batch.isSaleable()) {
                        Log.d(TAG, "Skipping traceability for " + batch.getBatchNumber()
                                + ": " + batch.getSaleBlockReason());
                        continue;
                    }
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

                // ✅ SOP §4.5 + §4.6
                if (!batch.isSaleable()) {
                    Log.d(TAG, "Cannot create traceability for " + batch.getBatchNumber()
                            + ": " + batch.getSaleBlockReason());
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

            if (!batch.isSaleable()) {
                Log.w(TAG, "createTraceability called on non-saleable batch "
                        + batch.getBatchNumber() + ": " + batch.getSaleBlockReason());
                return;
            }

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
            double revenueWeight = 0;
            String revenueBatchNumber = null;
            try {
                List<RevenueEntity> revenues = database.revenueDao()
                        .getRevenuesByBatchId(batch.getId());
                if (revenues != null && !revenues.isEmpty()) {
                    RevenueEntity revenue = revenues.get(0);
                    pricePerKg = revenue.getPricePerKg();
                    totalAmount = revenue.getTotalAmount();
                    buyer = revenue.getBuyer();
                    harvestCycleId = revenue.getId();
                    revenueWeight = revenue.getQuantityKg();
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not get revenue data", e);
            }

            // 3. ✅ Set processing details
            double rawWeight = batch.getWeightKg();  // Raw input weight
            double kernelWeight = revenueWeight > 0 ? revenueWeight : 0;  // Kernel output

            // 4. ✅ Calculate traceability weight (DECLARE HERE BEFORE USING)
            double traceabilityWeight = revenueWeight > 0 ? revenueWeight : batch.getWeightKg();
            double traceabilityPrice = pricePerKg > 0 ? pricePerKg : 0;
            double traceabilityTotal = totalAmount > 0 ? totalAmount : 0;

            // 5. Create traceability record
            BatchTraceabilityEntity traceability = new BatchTraceabilityEntity(
                    batch.getFarmId(),
                    batch.getBatchNumber(),
                    batch.getProcessingDate(),
                    kernelWeight > 0 ? kernelWeight : rawWeight,  // Use kernel weight if available
                    batch.getQualityGrade().getDisplayName(),
                    pricePerKg
            );

            // Set processing fields
            traceability.setRawWeightKg(rawWeight);
            traceability.setKernelWeightKg(kernelWeight);

            // ✅ Generate digital signature
            java.util.Map<String, Object> dataMap = new java.util.HashMap<>();
            dataMap.put("batchNumber", batch.getBatchNumber());
            dataMap.put("weightKg", traceabilityWeight > 0 ? traceabilityWeight : batch.getWeightKg());
            dataMap.put("supplierId", batch.getSupplierId());
            dataMap.put("qualityGrade", batch.getQualityGrade().name());
            dataMap.put("processingDate", batch.getProcessingDate());
            dataMap.put("buyer", buyer != null ? buyer : "");
            dataMap.put("destination", traceability.getDestination() != null ? traceability.getDestination() : "");
            dataMap.put("farmId", batch.getFarmId());


            // ✅ Set processing fields
            traceability.setRawWeightKg(rawWeight);
            traceability.setKernelWeightKg(kernelWeight);
            // Yield is auto-calculated

            // Generate signature
            String signature = DigitalSignatureGenerator.generateSignature(
                    batch.getBatchNumber(), dataMap);
            String verificationCode = DigitalSignatureGenerator.generateVerificationCode(
                    batch.getBatchNumber());
            String verificationUrl = DigitalSignatureGenerator.getVerificationUrl(
                    batch.getBatchNumber(), signature);

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
                existing.setWeightKg(traceabilityWeight);
                existing.setRawWeightKg(batch.getWeightKg());
                existing.setKernelWeightKg(revenueWeight);
                existing.setYieldPercentage(revenueWeight > 0 ? (revenueWeight / batch.getWeightKg()) * 100 : 0);
                existing.setTotalAmount(totalAmount);
                existing.setSustainabilityMetrics(sustainabilityMetrics);
                existing.setUpdatedAt(System.currentTimeMillis());
                database.batchTraceabilityDao().update(existing);
                Log.d(TAG, "Updated existing traceability with latest data");
                return;
            }

            // Set all fields
            traceability.setQualityBatchId(batch.getId());
            traceability.setSupplierId(batch.getSupplierId());
            traceability.setSupplierName(supplierName != null ? supplierName : "Unknown Supplier");
            traceability.setQualityGrade(batch.getQualityGrade().getDisplayName());

            // Set signature fields
            traceability.setDigitalSignature(signature);
            traceability.setVerificationCode(verificationCode);
            traceability.setVerificationUrl(verificationUrl);
            traceability.setSignedAt(System.currentTimeMillis());

            // ✅ Set revenue fields
            traceability.setHarvestCycleId(harvestCycleId);
            traceability.setBuyer(buyer);
            traceability.setPricePerKg(pricePerKg);
            traceability.setTotalAmount(totalAmount);
            traceability.setGrade(batch.getQualityGrade().getDisplayName());

            // ✅ Set processing fields
            traceability.setRawWeightKg(batch.getWeightKg());
            traceability.setKernelWeightKg(revenueWeight);
            traceability.setYieldPercentage(revenueWeight > 0 ? (revenueWeight / batch.getWeightKg()) * 100 : 0);

            // ✅ Set processing details from batch or defaults
            traceability.setProcessingMethod("Steam roasting — zero chemical additives");
            traceability.setProcessingFacility("AfriNuts NDC (Nuts Distribution Center)");
            traceability.setProcessingLocation("Odienné, Côte d'Ivoire");
            traceability.setShellingMethod("Semi-automated — mechanical and manual");
            traceability.setGradingMethod("Manual inspection + electronic sorting");

            // ✅ Set sustainability metrics
            traceability.setSustainabilityMetrics(sustainabilityMetrics);

            // ✅ Generate traceability URL
            traceability.setTraceabilityUrl(
                    traceability.generateTraceabilityUrl(batch.getBatchNumber()));

            // Save to database
            long id = database.batchTraceabilityDao().insert(traceability);
            Log.d(TAG, "✅ Created traceability with ID: " + id +
                    " and signature: " + verificationCode);
            Log.d(TAG, "Created traceability with ID: " + id +
                    " | Raw: " + batch.getWeightKg() + " kg" +
                    " | Kernel: " + revenueWeight + " kg" +
                    " | Yield: " + String.format("%.1f%%", traceability.getYieldPercentage()));

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