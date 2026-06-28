package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration17To18 extends Migration {

    public Migration17To18() {
        super(17, 18);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Step 1: Create new table with all columns including firestoreId
        database.execSQL(
                "CREATE TABLE `expenses_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`blockId` INTEGER, " +
                        "`batchId` INTEGER, " +
                        "`supplierId` INTEGER, " +
                        "`category` TEXT, " +
                        "`amount` REAL NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`description` TEXT, " +
                        "`receiptPath` TEXT, " +
                        "`batchNumber` TEXT, " +
                        "`supplierName` TEXT, " +
                        "`firestoreId` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE, " +
                        "FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) ON DELETE SET NULL, " +
                        "FOREIGN KEY(`batchId`) REFERENCES `quality_batches`(`id`) ON DELETE SET NULL, " +
                        "FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON DELETE SET NULL" +
                        ")"
        );

        // Step 2: Copy data from old table to new table
        database.execSQL(
                "INSERT INTO `expenses_new` (" +
                        "`id`, `farmId`, `blockId`, `batchId`, `supplierId`, `category`, " +
                        "`amount`, `date`, `description`, `receiptPath`, `batchNumber`, " +
                        "`supplierName`, `firestoreId`, `createdAt`, `updatedAt`" +
                        ") SELECT " +
                        "`id`, `farmId`, `blockId`, `batchId`, `supplierId`, `category`, " +
                        "`amount`, `date`, `description`, `receiptPath`, `batchNumber`, " +
                        "`supplierName`, `firestoreId`, `createdAt`, `updatedAt` " +
                        "FROM `expenses`"
        );

        // Step 3: Drop old table
        database.execSQL("DROP TABLE `expenses`");

        // Step 4: Rename new table
        database.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`");

        // Step 5: Create indexes
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_batchId` ON `expenses` (`batchId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_supplierId` ON `expenses` (`supplierId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_farmId` ON `expenses` (`farmId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_blockId` ON `expenses` (`blockId`)"
        );
    }
}