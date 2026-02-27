package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration3To4 extends Migration {

    public Migration3To4() {
        super(3, 4);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create expenses table
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `expenses` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`blockId` INTEGER, " +
                        "`category` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`description` TEXT, " +
                        "`receiptPath` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE, " +
                        "FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) ON DELETE SET NULL" +
                        ")"
        );

        // Create indexes for performance
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_farmId` ON `expenses` (`farmId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_blockId` ON `expenses` (`blockId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)"
        );
    }
}