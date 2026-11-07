package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migration 40 → 41.
 *
 * Four table rebuilds. CREATE TABLE statements are copied verbatim from the
 * committed 41.json createSql for each table — that is the only way to
 * guarantee Room's runtime schema validator accepts the post-migration
 * structure. The INSERT ... SELECT lists are the shared columns between 40
 * and 41; any column that exists only in 41 (masterBatchId) is set to NULL
 * (or an explicit default) during the copy. Any column that exists only in
 * 40 is dropped with the table.
 *
 * Data-preservation rules:
 *   - revenues.createdAt takes harvestDate where harvestDate > 0, else its
 *     existing createdAt. Preserves the picked sale date on historical rows.
 *     (Explicitly retained, per the plan — it costs nothing and it's not
 *     demo-data-sensitive; the instruction's "may stay or go" allows it
 *     and keeping it is strictly safer.)
 *   - batch_traceability.harvestCycleId → revenueId: values carried across
 *     unchanged (they are revenue row ids; this is a rename, not a re-key).
 *   - master_batch_traceability.farmId: backfilled from the single farms
 *     row (single-farm deployment, all demo data).
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
    //    CREATE TABLE copied verbatim from 41.json createSql.
    //    Data rule: createdAt takes harvestDate where harvestDate > 0,
    //    else existing createdAt. updatedAt untouched.
    // ============================================================
    private void rebuildRevenues(SupportSQLiteDatabase db) {
        Log.d(TAG, "  rebuilding revenues");

        // Verbatim from 41.json
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

        // Copy shared columns; createdAt collapses harvestDate > 0 ? harvestDate : createdAt.
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
    }

    // ============================================================
    // 2. quality_batches — drop harvestCycleId.
    //
    //    CREATE TABLE copied verbatim from 41.json createSql.
    //    Three FKs: farm CASCADE, block SET_NULL, supplier SET_NULL.
    //    R2: exactly one supplierId FK, SET_NULL only.
    // ============================================================
    private void rebuildQualityBatches(SupportSQLiteDatabase db) {
        Log.d(TAG, "  rebuilding quality_batches");

        // Verbatim from 41.json
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
                "FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                "FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL" +
                ")");

        // Copy shared columns; harvestCycleId simply drops (not in the SELECT).
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
    }

    // ============================================================
    // 3. batch_traceability — rename harvestCycleId → revenueId.
    //    Add masterBatchId TEXT nullable, null for existing rows.
    //    Four FKs: farm CASCADE, qualityBatchId CASCADE,
    //    supplierId SET_NULL, revenueId SET_NULL.
    //    All signature columns preserved: digitalSignature,
    //    verificationHash, verificationCode, verificationUrl, signedAt.
    //    harvestYear untouched.
    //    CREATE TABLE copied verbatim from 41.json createSql.
    // ============================================================
    private void rebuildBatchTraceability(SupportSQLiteDatabase db) {
        Log.d(TAG, "  rebuilding batch_traceability");

        // Verbatim from 41.json
        db.execSQL("CREATE TABLE IF NOT EXISTS `batch_traceability_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`farmId` INTEGER NOT NULL, " +
                "`batchNumber` TEXT, " +
                "`processingDate` INTEGER NOT NULL, " +
                "`weightKg` REAL NOT NULL, " +
                "`grade` TEXT, " +
                "`pricePerKg` REAL NOT NULL, " +
                "`revenueId` INTEGER, " +
                "`masterBatchId` TEXT, " +
                "`qualityBatchId` INTEGER, " +
                "`supplierId` INTEGER, " +
                "`supplierName` TEXT, " +
                "`qualityGrade` TEXT, " +
                "`certificationIds` TEXT, " +
                "`verificationCode` TEXT, " +
                "`sustainabilityMetrics` TEXT, " +
                "`harvestYear` INTEGER NOT NULL DEFAULT 0, " +
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
                "`digitalSignature` TEXT, " +
                "`verificationHash` TEXT, " +
                "`verificationUrl` TEXT, " +
                "`signedAt` INTEGER NOT NULL DEFAULT 0, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`qualityBatchId`) REFERENCES `quality_batches`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                "FOREIGN KEY(`revenueId`) REFERENCES `revenues`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL" +
                ")");

        // Copy shared columns. revenueId takes harvestCycleId.
        // masterBatchId → NULL for every existing row.
        // Signature columns carried across unchanged — no data loss.
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
                        " `digitalSignature`, `verificationHash`, `verificationUrl`, `signedAt`, " +
                        " `createdAt`, `updatedAt`, `isSynced`) " +
                        "SELECT " +
                        "  `id`, `farmId`, `batchNumber`, `processingDate`, `weightKg`, " +
                        "  `grade`, `pricePerKg`, `harvestCycleId`, NULL, " +
                        "  `qualityBatchId`, `supplierId`, `supplierName`, `qualityGrade`, " +
                        "  `certificationIds`, `verificationCode`, `sustainabilityMetrics`, " +
                        "  `harvestYear`, `exportDate`, `buyer`, `destination`, `totalAmount`, " +
                        "  `processingLocation`, `processingMethod`, `processingFacility`, " +
                        "  `rawWeightKg`, `kernelWeightKg`, `yieldPercentage`, " +
                        "  `shellingMethod`, `gradingMethod`, `documents`, `traceabilityUrl`, " +
                        "  `digitalSignature`, `verificationHash`, `verificationUrl`, `signedAt`, " +
                        "  `createdAt`, `updatedAt`, `isSynced` " +
                        "FROM `batch_traceability`"
        );

        db.execSQL("DROP TABLE `batch_traceability`");
        db.execSQL("ALTER TABLE `batch_traceability_new` RENAME TO `batch_traceability`");

        // Index for the F2 DAO query — matches the @Index on the entity.
        db.execSQL("CREATE INDEX IF NOT EXISTS " +
                "`index_batch_traceability_masterBatchId` " +
                "ON `batch_traceability` (`masterBatchId`)");
    }

    // ============================================================
    // 4. master_batch_traceability — add farmId INTEGER NOT NULL.
    //    Column names match the entity (batchNumbersJson,
    //    batchTraceabilityIdsJson). exportDate INTEGER NOT NULL
    //    (entity field is primitive long). verificationCode present.
    //    Backfill farmId from the single farms row.
    //    CREATE TABLE copied verbatim from 41.json createSql.
    // ============================================================
    private void rebuildMasterBatchTraceability(SupportSQLiteDatabase db) {
        Log.d(TAG, "  rebuilding master_batch_traceability");

        // Verbatim from 41.json
        db.execSQL("CREATE TABLE IF NOT EXISTS `master_batch_traceability_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`farmId` INTEGER NOT NULL, " +
                "`masterBatchNumber` TEXT, " +
                "`shipmentName` TEXT, " +
                "`shipmentId` TEXT, " +
                "`totalWeightKg` REAL NOT NULL, " +
                "`totalBatches` INTEGER NOT NULL, " +
                "`grades` TEXT, " +
                "`certifications` TEXT, " +
                "`batchNumbersJson` TEXT, " +
                "`batchTraceabilityIdsJson` TEXT, " +
                "`buyer` TEXT, " +
                "`destination` TEXT, " +
                "`exportDate` INTEGER NOT NULL, " +
                "`verificationCode` TEXT, " +
                "`traceabilityUrl` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "`isSynced` INTEGER NOT NULL DEFAULT 0" +
                ")");

        // Backfill farmId from the single farms row. Column names in the
        // 40 schema are batchNumbers / batchTraceabilityIds (matching the
        // entity's pre-41 field names). exportDate is nullable in 40
        // (entity was Integer); coerce to 0 where it was NULL, since 41
        // requires NOT NULL.
        db.execSQL(
                "INSERT INTO `master_batch_traceability_new` " +
                        "(`id`, `farmId`, `masterBatchNumber`, `shipmentName`, `shipmentId`, " +
                        " `totalWeightKg`, `totalBatches`, `grades`, `certifications`, " +
                        " `batchNumbersJson`, `batchTraceabilityIdsJson`, `buyer`, `destination`, " +
                        " `exportDate`, `verificationCode`, `traceabilityUrl`, " +
                        " `createdAt`, `updatedAt`, `isSynced`) " +
                        "SELECT " +
                        "  `id`, " +
                        "  (SELECT `id` FROM `farms` LIMIT 1), " +
                        "  `masterBatchNumber`, `shipmentName`, `shipmentId`, " +
                        "  `totalWeightKg`, `totalBatches`, `grades`, `certifications`, " +
                        "  `batchNumbers`, `batchTraceabilityIds`, `buyer`, `destination`, " +
                        "  COALESCE(`exportDate`, 0), " +
                        "  `verificationCode`, `traceabilityUrl`, " +
                        "  `createdAt`, `updatedAt`, `isSynced` " +
                        "FROM `master_batch_traceability`"
        );

        db.execSQL("DROP TABLE `master_batch_traceability`");
        db.execSQL("ALTER TABLE `master_batch_traceability_new` RENAME TO `master_batch_traceability`");
    }
}