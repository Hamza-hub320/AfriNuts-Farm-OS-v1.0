package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migration from version 1 to 2
 * - Adds createdAt and updatedAt columns to farms table
 * - Creates blocks table
 */
public class Migration1To2 extends Migration {

    public Migration1To2() {
        super(1, 2);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {

        // === PART 1: Update farms table with new columns ===

        // Create temporary table with new schema
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `farms_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT, " +
                        "`location` TEXT, " +
                        "`totalHectares` REAL NOT NULL, " +
                        "`cashewHectares` REAL NOT NULL, " +
                        "`treesPerHectare` INTEGER NOT NULL, " +
                        "`plantingYear` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL DEFAULT 0, " +
                        "`updatedAt` INTEGER NOT NULL DEFAULT 0" +
                        ")"
        );

        // Copy existing data with default timestamps
        database.execSQL(
                "INSERT INTO `farms_new` (" +
                        "`id`, `name`, `location`, `totalHectares`, `cashewHectares`, " +
                        "`treesPerHectare`, `plantingYear`, `createdAt`, `updatedAt`" +
                        ") SELECT " +
                        "`id`, `name`, `location`, `totalHectares`, `cashewHectares`, " +
                        "`treesPerHectare`, `plantingYear`, 0, 0 " +
                        "FROM `farms`"
        );

        // Drop old table
        database.execSQL("DROP TABLE `farms`");

        // Rename new table to original name
        database.execSQL("ALTER TABLE `farms_new` RENAME TO `farms`");

        // === PART 2: Create blocks table ===

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `blocks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`blockName` TEXT NOT NULL, " +
                        "`hectareSize` REAL NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`plantingDate` INTEGER NOT NULL, " +
                        "`survivalRate` REAL NOT NULL, " +
                        "`replacementCount` INTEGER NOT NULL, " +
                        "`notes` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE" +
                        ")"
        );

        // Create index for performance
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_blocks_farmId` ON `blocks` (`farmId`)"
        );
    }
}
