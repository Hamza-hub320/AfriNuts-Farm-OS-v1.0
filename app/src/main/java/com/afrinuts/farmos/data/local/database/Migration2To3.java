package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migration from version 2 to 3
 * - Replace plantingDate with clearedDate, plowedDate, plantedDate
 */
public class Migration2To3 extends Migration {

    public Migration2To3() {
        super(2, 3);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create temporary table with new schema - matching Room expectations
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `blocks_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`blockName` TEXT NOT NULL, " +
                        "`hectareSize` REAL NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`clearedDate` INTEGER, " +
                        "`plowedDate` INTEGER, " +
                        "`plantedDate` INTEGER, " +
                        "`survivalRate` REAL NOT NULL, " +
                        "`replacementCount` INTEGER NOT NULL, " +
                        "`notes` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE" +
                        ")"
        );

        // Copy data from old table to new table
        database.execSQL(
                "INSERT INTO `blocks_new` (" +
                        "`id`, `farmId`, `blockName`, `hectareSize`, `status`, " +
                        "`clearedDate`, `plowedDate`, `plantedDate`, " +
                        "`survivalRate`, `replacementCount`, `notes`, `createdAt`, `updatedAt`" +
                        ") SELECT " +
                        "`id`, `farmId`, `blockName`, `hectareSize`, `status`, " +
                        "CASE " +
                        "    WHEN `status` = 'CLEARED' THEN `plantingDate` " +
                        "    ELSE NULL " +
                        "END, " +
                        "CASE " +
                        "    WHEN `status` = 'PLOWED' THEN `plantingDate` " +
                        "    ELSE NULL " +
                        "END, " +
                        "CASE " +
                        "    WHEN `status` = 'PLANTED' THEN `plantingDate` " +
                        "    ELSE NULL " +
                        "END, " +
                        "`survivalRate`, `replacementCount`, `notes`, `createdAt`, `updatedAt` " +
                        "FROM `blocks`"
        );

        // Drop old table
        database.execSQL("DROP TABLE `blocks`");

        // Rename new table
        database.execSQL("ALTER TABLE `blocks_new` RENAME TO `blocks`");

        // Create index for performance
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_blocks_farmId` ON `blocks` (`farmId`)"
        );
    }
}