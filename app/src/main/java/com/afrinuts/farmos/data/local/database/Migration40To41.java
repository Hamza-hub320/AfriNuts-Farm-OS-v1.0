package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migration 40 → 41.
 *
 * Part A5 of the plan: four table rebuilds carrying A1 (revenue stops
 * modelling harvest), A2 (BatchTraceabilityEntity.harvestCycleId → revenueId),
 * A3 (QualityBatchEntity.harvestCycleId dropped), F2 (masterBatchId on
 * batch_traceability; farmId on master_batch_traceability), and R2
 * (QualityBatchEntity.supplierId keeps SET_NULL only — CASCADE removed).
 *
 * No fallbackToDestructiveMigration. Row counts preserved on all four tables.
 */
public class Migration40To41 extends Migration {

    private static final String TAG = "Migration40To41";

    public Migration40To41() {
        super(40, 41);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Migration 40 -> 41: revenue/quality/traceability/master rebuilds");

        try {
            rebuildRevenues(database);
            rebuildQualityBatches(database);
            rebuildBatchTraceability(database);
            rebuildMasterBatchTraceability(database);

            Log.d(TAG, "✅ Migration 40 -> 41 completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 40 -> 41 failed", e);
            throw e;
        }
    }

    // ============================================================
    // 1. revenues — drop blockId, harvestDate, harvestCycleId,
    //    harvestYear, harvestSeason.
    //
    //    Data rule: createdAt takes the old harvestDate value where
    //    harvestDate > 0. Rows without a picked date keep their
    //    existing createdAt. updatedAt untouched.
    //
    //    FKs as the entity declares: farm CASCADE, batch SET_NULL,
    //    supplier SET_NULL. No blockId FK (blockId column is gone).
    // ============================================================
    private void rebuildRevenues(SupportSQLiteDatabase db) {
        Log.d(TAG, "  rebuilding revenues");

        db.execSQL("CREATE TABLE IF NOT EXISTS `revenues_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`farmId` INTEGER NOT NULL, " +
                "`batchId` INTEGER, " +
                "`supplierId` INTEGER, " +
                "`quantityKg` REAL NOT NULL, " +
                "`pricePerKg` REAL NOT NULL, " +
                "`totalAmount` REAL NOT NULL, " +
                "`buyer` TEXT, " +
                "`quality` TEXT, " +
                "`notes` TEXT, " +
                "`batchNumber` TEXT, " +
                "`supplierName` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`batchId`) REFERENCES `quality_batches`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                "FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL" +
                ")");

        // Data rule: createdAt takes harvestDate where harvestDate > 0.
        // Note that updatedAt is untouched — preserved from the old table.
        db.execSQL(
                "INSERT INTO `revenues_new` " +
                        "(`id`, `farmId`, `batchId`, `supplierId`, `quantityKg`, " +
                        " `pricePerKg`, `totalAmount`, `buyer`, `quality`, `notes`, " +
                        " `batchNumber`, `supplierName`, `createdAt`, `updatedAt`) " +
                        "SELECT " +
                        "  `id`, `farmId`, `batchId`, `supplierId`, `quantityKg`, " +
                        "  `pricePerKg`, `totalAmount`, `buyer`, `quality`, `notes`, " +
                        "  `batchNumber`, `supplierName`, " +
                        "  CASE WHEN `harvestDate` > 0 THEN `harvestDate` " +
                        "       ELSE `createdAt` END, " +
                        "  `updatedAt` " +
                        "FROM `revenues`"
        );

        db.execSQL("DROP TABLE `revenues`");
        db.execSQL("ALTER TABLE `revenues_new` RENAME TO `revenues`");

        // Indexes for the entity's declared indexes (if any) — recreate the
        // ones we know about from other migrations. If the entity declares
        // no extra indexes on revenues beyond the PK and FKs, this is a no-op.
        // Add them below if you add @Index annotations in the future.
    }

    // ============================================================
    // 2. quality_batches — drop harvestCycleId.
    //    Rebuilt table carries exactly one supplierId FK (SET_NULL).
    //    No data mapping — the column simply goes.
    // ============================================================
    private void rebuildQualityBatches(SupportSQLiteDatabase db) {
        Log.d(TAG, "  rebuilding quality_batches");

        db.execSQL("CREATE TABLE IF NOT EXISTS `quality_batches_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`farmId` INTEGER NOT NULL, " +
                "`blockId` INTEGER, " +
                "`supplierId` INTEGER, " +
                "`sourceType` TEXT, " +
                "`batchNumber` TEXT, " +
                "`processingDate` INTEGER NOT NULL, " +
                "`moistureContent` REAL NOT NULL, " +
                "`nutCountPerKg` INTEGER NOT NULL, " +
                "`defectiveRate` REAL NOT NULL, " +
                "`kernelRatio` REAL NOT NULL, " +
                "`weightKg` REAL NOT NULL, " +
                "`qualityGrade` TEXT, " +
                "`testResults` TEXT, " +
                "`qualityScore` REAL NOT NULL, " +
                "`notes` TEXT, " +
                "`verificationCode` TEXT, " +
                "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                "`noChildLabor` INTEGER NOT NULL DEFAULT 0, " +
                "`farmOsMonitored` INTEGER NOT NULL DEFAULT 0, " +
                "`firebreakProtection` INTEGER NOT NULL DEFAULT 0, " +
                "`sustainableHarvesting` INTEGER NOT NULL DEFAULT 0, " +
                "`verifiedAt` INTEGER NOT NULL DEFAULT 0, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "`productType` TEXT, " +
                "`mouldyRate` REAL NOT NULL DEFAULT 0, " +
                "`immatureRate` REAL NOT NULL DEFAULT 0, " +
                "`insectDamagedRate` REAL NOT NULL DEFAULT 0, " +
                "`shrivelledRate` REAL NOT NULL DEFAULT 0, " +
                "`foreignMatter` REAL NOT NULL DEFAULT 0, " +
                "`dryingMethod` TEXT, " +
                "`rcnQualityScore` REAL NOT NULL DEFAULT 0, " +
                "`brokenKernels` REAL NOT NULL DEFAULT 0, " +
                "`aflatoxinPpb` REAL NOT NULL DEFAULT 0, " +
                "`heavyMetalsPpb` REAL NOT NULL DEFAULT 0, " +
                "`pesticideResiduePpb` REAL NOT NULL DEFAULT 0, " +
                "`kernelQualityScore` REAL NOT NULL DEFAULT 0, " +
                "`korGPerKg` REAL NOT NULL DEFAULT 0, " +
                "`korSampleGrams` REAL NOT NULL DEFAULT 0, " +
                "`korKernelGrams` REAL NOT NULL DEFAULT 0, " +
                "`inspectionDate` INTEGER NOT NULL DEFAULT 0, " +
                "`inspectedBy` TEXT, " +
                "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                // R2: exactly one supplierId FK, SET_NULL only.
                "FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL" +
                ")");

        db.execSQL(
                "INSERT INTO `quality_batches_new` " +
                        "(`id`, `farmId`, `blockId`, `supplierId`, `sourceType`, " +
                        " `batchNumber`, `processingDate`, `moistureContent`, `nutCountPerKg`, " +
                        " `defectiveRate`, `kernelRatio`, `weightKg`, `qualityGrade`, " +
                        " `testResults`, `qualityScore`, `notes`, `verificationCode`, " +
                        " `isSynced`, `noChildLabor`, `farmOsMonitored`, `firebreakProtection`, " +
                        " `sustainableHarvesting`, `verifiedAt`, `createdAt`, `updatedAt`, " +
                        " `productType`, `mouldyRate`, `immatureRate`, `insectDamagedRate`, " +
                        " `shrivelledRate`, `foreignMatter`, `dryingMethod`, `rcnQualityScore`, " +
                        " `brokenKernels`, `aflatoxinPpb`, `heavyMetalsPpb`, `pesticideResiduePpb`, " +
                        " `kernelQualityScore`, `korGPerKg`, `korSampleGrams`, `korKernelGrams`, " +
                        " `inspectionDate`, `inspectedBy`) " +
                        "SELECT " +
                        "  `id`, `farmId`, `blockId`, `supplierId`, `sourceType`, " +
                        "  `batchNumber`, `processingDate`, `moistureContent`, `nutCountPerKg`, " +
                        "  `defectiveRate`, `kernelRatio`, `weightKg`, `qualityGrade`, " +
                        "  `testResults`, `qualityScore`, `notes`, `verificationCode`, " +
                        "  `isSynced`, `noChildLabor`, `farmOsMonitored`, `firebreakProtection`, " +
                        "  `sustainableHarvesting`, `verifiedAt`, `createdAt`, `updatedAt`, " +
                        "  `productType`, `mouldyRate`, `immatureRate`, `insectDamagedRate`, " +
                        "  `shrivelledRate`, `foreignMatter`, `dryingMethod`, `rcnQualityScore`, " +
                        "  `brokenKernels`, `aflatoxinPpb`, `heavyMetalsPpb`, `pesticideResiduePpb`, " +
                        "  `kernelQualityScore`, `korGPerKg`, `korSampleGrams`, `korKernelGrams`, " +
                        "  `inspectionDate`, `inspectedBy` " +
                        "FROM `quality_batches`"
        );

        db.execSQL("DROP TABLE `quality_batches`");
        db.execSQL("ALTER TABLE `quality_batches_new` RENAME TO `quality_batches`");

        // Indexes on quality_batches if the entity declares any (beyond FK
        // implicit indexes). Preserve here if you have @Index annotations.
    }

    // ============================================================
    // 3. batch_traceability — rename harvestCycleId → revenueId.
    //    Column rename only; values carried across unchanged (they are
    //    revenue row ids). Add masterBatchId (nullable TEXT), null for
    //    every existing row. harvestYear and every other column
    //    untouched.
    // ============================================================
    private void rebuildBatchTraceability(SupportSQLiteDatabase db) {
        Log.d(TAG, "  rebuilding batch_traceability");

        db.execSQL("CREATE TABLE IF NOT EXISTS `batch_traceability_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`farmId` INTEGER NOT NULL, " +
                "`batchNumber` TEXT, " +
                "`processingDate` INTEGER NOT NULL, " +
                "`weightKg` REAL NOT NULL, " +
                "`grade` TEXT, " +
                "`pricePerKg` REAL NOT NULL, " +
                "`revenueId` INTEGER, " +                  // renamed from harvestCycleId
                "`masterBatchId` TEXT, " +                 // NEW (F2)
                "`qualityBatchId` INTEGER, " +
                "`supplierId` INTEGER, " +
                "`supplierName` TEXT, " +
                "`qualityGrade` TEXT, " +
                "`certificationIds` TEXT, " +
                "`verificationCode` TEXT, " +
                "`sustainabilityMetrics` TEXT, " +
                "`harvestYear` INTEGER NOT NULL DEFAULT 0, " +  // untouched
                "`exportDate` INTEGER, " +
                "`buyer` TEXT, " +
                "`destination` TEXT, " +
                "`totalAmount` REAL NOT NULL, " +
                "`processingLocation` TEXT, " +
                "`processingMethod` TEXT, " +
                "`processingFacility` TEXT, " +
                "`rawWeightKg` REAL NOT NULL DEFAULT 0, " +
                "`kernelWeightKg` REAL NOT NULL DEFAULT 0, " +
                "`yieldPercentage` REAL NOT NULL DEFAULT 0, " +
                "`shellingMethod` TEXT, " +
                "`gradingMethod` TEXT, " +
                "`documents` TEXT, " +
                "`traceabilityUrl` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`revenueId`) REFERENCES `revenues`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL" +
                ")");

        db.execSQL(
                "INSERT INTO `batch_traceability_new` " +
                        "(`id`, `farmId`, `batchNumber`, `processingDate`, `weightKg`, " +
                        " `grade`, `pricePerKg`, `revenueId`, `masterBatchId`, " +
                        " `qualityBatchId`, `supplierId`, `supplierName`, `qualityGrade`, " +
                        " `certificationIds`, `verificationCode`, `sustainabilityMetrics`, " +
                        " `harvestYear`, `exportDate`, `buyer`, `destination`, `totalAmount`, " +
                        " `processingLocation`, `processingMethod`, `processingFacility`, " +
                        " `rawWeightKg`, `kernelWeightKg`, `yieldPercentage`, " +
                        " `shellingMethod`, `gradingMethod`, `documents`, `traceabilityUrl`, " +
                        " `createdAt`, `updatedAt`, `isSynced`) " +
                        "SELECT " +
                        "  `id`, `farmId`, `batchNumber`, `processingDate`, `weightKg`, " +
                        "  `grade`, `pricePerKg`, `harvestCycleId`, NULL, " +  // rename + new column
                        "  `qualityBatchId`, `supplierId`, `supplierName`, `qualityGrade`, " +
                        "  `certificationIds`, `verificationCode`, `sustainabilityMetrics`, " +
                        "  `harvestYear`, `exportDate`, `buyer`, `destination`, `totalAmount`, " +
                        "  `processingLocation`, `processingMethod`, `processingFacility`, " +
                        "  `rawWeightKg`, `kernelWeightKg`, `yieldPercentage`, " +
                        "  `shellingMethod`, `gradingMethod`, `documents`, `traceabilityUrl`, " +
                        "  `createdAt`, `updatedAt`, `isSynced` " +
                        "FROM `batch_traceability`"
        );

        db.execSQL("DROP TABLE `batch_traceability`");
        db.execSQL("ALTER TABLE `batch_traceability_new` RENAME TO `batch_traceability`");

        // Index for the F2 DAO query
        db.execSQL("CREATE INDEX IF NOT EXISTS " +
                "`index_batch_traceability_masterBatchId` " +
                "ON `batch_traceability` (`masterBatchId`)");
    }

    // ============================================================
    // 4. master_batch_traceability — add farmId INTEGER NOT NULL.
    //    Backfill: every existing master takes the id of the farm row
    //    (single-farm deployment, all demo data). No master may be
    //    left with a placeholder once the app next writes it.
    // ============================================================
    private void rebuildMasterBatchTraceability(SupportSQLiteDatabase db) {
        Log.d(TAG, "  rebuilding master_batch_traceability");

        db.execSQL("CREATE TABLE IF NOT EXISTS `master_batch_traceability_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`farmId` INTEGER NOT NULL, " +            // NEW
                "`masterBatchNumber` TEXT, " +
                "`shipmentName` TEXT, " +
                "`shipmentId` TEXT, " +
                "`totalWeightKg` REAL NOT NULL, " +
                "`totalBatches` INTEGER NOT NULL, " +
                "`grades` TEXT, " +
                "`certifications` TEXT, " +
                "`batchNumbers` TEXT, " +
                "`batchTraceabilityIds` TEXT, " +
                "`buyer` TEXT, " +
                "`destination` TEXT, " +
                "`exportDate` INTEGER, " +
                "`traceabilityUrl` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "`isSynced` INTEGER NOT NULL DEFAULT 0" +
                ")");

        // Backfill farmId from the single farms row. Subquery used because
        // master_batch_traceability currently has no farmId to join on.
        db.execSQL(
                "INSERT INTO `master_batch_traceability_new` " +
                        "(`id`, `farmId`, `masterBatchNumber`, `shipmentName`, `shipmentId`, " +
                        " `totalWeightKg`, `totalBatches`, `grades`, `certifications`, " +
                        " `batchNumbers`, `batchTraceabilityIds`, `buyer`, `destination`, " +
                        " `exportDate`, `traceabilityUrl`, `createdAt`, `updatedAt`, `isSynced`) " +
                        "SELECT " +
                        "  `id`, " +
                        "  (SELECT `id` FROM `farms` LIMIT 1), " +   // backfill farmId
                        "  `masterBatchNumber`, `shipmentName`, `shipmentId`, " +
                        "  `totalWeightKg`, `totalBatches`, `grades`, `certifications`, " +
                        "  `batchNumbers`, `batchTraceabilityIds`, `buyer`, `destination`, " +
                        "  `exportDate`, `traceabilityUrl`, `createdAt`, `updatedAt`, `isSynced` " +
                        "FROM `master_batch_traceability`"
        );

        db.execSQL("DROP TABLE `master_batch_traceability`");
        db.execSQL("ALTER TABLE `master_batch_traceability_new` RENAME TO `master_batch_traceability`");
    }
}