package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration9To10 extends Migration {

    public Migration9To10() {
        super(9, 10);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create sync_queue table
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `sync_queue` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`entityType` TEXT NOT NULL, " +
                        "`entityId` INTEGER NOT NULL, " +
                        "`farmId` TEXT NOT NULL, " +
                        "`action` TEXT NOT NULL, " +
                        "`lastAttemptAt` INTEGER NOT NULL, " +
                        "`attemptCount` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL" +
                        ")"
        );

        // Create indexes
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sync_queue_status` ON `sync_queue` (`status`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sync_queue_farmId` ON `sync_queue` (`farmId`)"
        );
    }
}