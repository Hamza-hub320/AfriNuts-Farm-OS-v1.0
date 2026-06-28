package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration16To17 extends Migration {

    public Migration16To17() {
        super(16, 17);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Step 1: Create new suppliers table with GPS fields
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `suppliers_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`name` TEXT, " +
                        "`type` TEXT, " +
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
                        "`certificationExpiryDate` INTEGER NOT NULL DEFAULT 0, " +
                        "`isCertified` INTEGER NOT NULL DEFAULT 0, " +
                        "`status` TEXT, " +
                        "`isActive` INTEGER NOT NULL DEFAULT 1, " +
                        "`notes` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                        "`latitude` REAL NOT NULL DEFAULT 0, " +
                        "`longitude` REAL NOT NULL DEFAULT 0, " +
                        "`hasLocation` INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE" +
                        ")"
        );

        // Step 2: Copy data from old table to new table
        database.execSQL(
                "INSERT INTO `suppliers_new` (" +
                        "`id`, `farmId`, `name`, `type`, `contactPerson`, `phone`, `email`, " +
                        "`location`, `region`, `country`, `registrationNumber`, `taxId`, " +
                        "`annualVolumeKg`, `averageQualityScore`, `rating`, `certificationType`, " +
                        "`certificationNumber`, `certificationExpiryDate`, `isCertified`, " +
                        "`status`, `isActive`, `notes`, `createdAt`, `updatedAt`, `isSynced`, " +
                        "`latitude`, `longitude`, `hasLocation`" +
                        ") SELECT " +
                        "`id`, `farmId`, `name`, `type`, `contactPerson`, `phone`, `email`, " +
                        "`location`, `region`, `country`, `registrationNumber`, `taxId`, " +
                        "`annualVolumeKg`, `averageQualityScore`, `rating`, `certificationType`, " +
                        "`certificationNumber`, `certificationExpiryDate`, `isCertified`, " +
                        "`status`, `isActive`, `notes`, `createdAt`, `updatedAt`, `isSynced`, " +
                        "0, 0, 0 " +  // Default values for new columns
                        "FROM `suppliers`"
        );

        // Step 3: Drop old table
        database.execSQL("DROP TABLE `suppliers`");

        // Step 4: Rename new table
        database.execSQL("ALTER TABLE `suppliers_new` RENAME TO `suppliers`");

        // Step 5: Update quality_batches table to add supplierId and sourceType
        database.execSQL(
                "ALTER TABLE `quality_batches` ADD COLUMN `supplierId` INTEGER"
        );
        database.execSQL(
                "ALTER TABLE `quality_batches` ADD COLUMN `sourceType` TEXT"
        );

        // Step 6: Create indexes
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
                "CREATE INDEX IF NOT EXISTS `index_suppliers_hasLocation` ON `suppliers` (`hasLocation`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_supplierId` ON `quality_batches` (`supplierId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_sourceType` ON `quality_batches` (`sourceType`)"
        );
    }
}