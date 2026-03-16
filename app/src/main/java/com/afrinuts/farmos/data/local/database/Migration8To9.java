package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration8To9 extends Migration {

    public Migration8To9() {
        super(8, 9);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create photos table with ALL columns including photoType
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `photos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`blockId` INTEGER, " +
                        "`taskId` INTEGER, " +
                        "`expenseId` INTEGER, " +
                        "`filePath` TEXT NOT NULL, " +
                        "`thumbnailPath` TEXT, " +
                        "`caption` TEXT, " +
                        "`takenAt` INTEGER NOT NULL, " +
                        "`latitude` REAL NOT NULL DEFAULT 0, " +
                        "`longitude` REAL NOT NULL DEFAULT 0, " +
                        "`photoType` TEXT, " +  // ADD THIS LINE
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE, " +
                        "FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) ON DELETE SET NULL, " +
                        "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON DELETE SET NULL, " +
                        "FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON DELETE SET NULL" +
                        ")"
        );

        // Create indexes for performance
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_photos_farmId` ON `photos` (`farmId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_photos_blockId` ON `photos` (`blockId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_photos_taskId` ON `photos` (`taskId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_photos_expenseId` ON `photos` (`expenseId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_photos_takenAt` ON `photos` (`takenAt`)"
        );
    }
}