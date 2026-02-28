package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration4To5 extends Migration {

    public Migration4To5() {
        super(4, 5);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create revenues table
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `revenues` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`blockId` INTEGER, " +
                        "`harvestDate` INTEGER NOT NULL, " +
                        "`quantityKg` REAL NOT NULL, " +
                        "`pricePerKg` REAL NOT NULL, " +
                        "`totalAmount` REAL NOT NULL, " +
                        "`buyer` TEXT, " +
                        "`quality` TEXT NOT NULL, " +
                        "`notes` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE, " +
                        "FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) ON DELETE SET NULL" +
                        ")"
        );

        // Create indexes for performance
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_revenues_farmId` ON `revenues` (`farmId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_revenues_blockId` ON `revenues` (`blockId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_revenues_harvestDate` ON `revenues` (`harvestDate`)"
        );
    }
}