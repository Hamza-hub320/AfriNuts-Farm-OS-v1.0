package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration15To16 extends Migration {

    public Migration15To16() {
        super(15, 16);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Step 1: Check if quality_batches table exists and has supplierId
        boolean hasSupplierId = false;
        boolean hasSourceType = false;

        try {
            database.query("SELECT supplierId FROM quality_batches LIMIT 1").close();
            hasSupplierId = true;
        } catch (Exception e) {
            // Column doesn't exist
        }

        try {
            database.query("SELECT sourceType FROM quality_batches LIMIT 1").close();
            hasSourceType = true;
        } catch (Exception e) {
            // Column doesn't exist
        }

        // Step 2: Create suppliers table (if not exists)
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `suppliers` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`contactPerson` TEXT, " +
                        "`phone` TEXT, " +
                        "`email` TEXT, " +
                        "`location` TEXT, " +
                        "`region` TEXT, " +
                        "`country` TEXT, " +
                        "`registrationNumber` TEXT, " +
                        "`taxId` TEXT, " +
                        "`annualVolumeKg` REAL NOT NULL DEFAULT 0, " +
                        "`averageQualityScore` REAL NOT NULL DEFAULT 0, " +
                        "`rating` INTEGER NOT NULL DEFAULT 3, " +
                        "`certificationType` TEXT, " +
                        "`certificationNumber` TEXT, " +
                        "`certificationExpiryDate` INTEGER, " +
                        "`isCertified` INTEGER NOT NULL DEFAULT 0, " +
                        "`status` TEXT NOT NULL, " +
                        "`isActive` INTEGER NOT NULL DEFAULT 1, " +
                        "`notes` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE" +
                        ")"
        );

        // Step 3: Only add columns if they don't exist
        if (!hasSupplierId) {
            database.execSQL("ALTER TABLE `quality_batches` ADD COLUMN `supplierId` INTEGER");
        }

        if (!hasSourceType) {
            database.execSQL("ALTER TABLE `quality_batches` ADD COLUMN `sourceType` TEXT");
        }

        // Step 4: Create indexes
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_suppliers_farmId` ON `suppliers` (`farmId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_suppliers_type` ON `suppliers` (`type`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_suppliers_status` ON `suppliers` (`status`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_suppliers_rating` ON `suppliers` (`rating`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_suppliers_isActive` ON `suppliers` (`isActive`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_supplierId` ON `quality_batches` (`supplierId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_sourceType` ON `quality_batches` (`sourceType`)"
        );
    }
}