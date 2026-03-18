package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration10To11 extends Migration {
    public Migration10To11() {
        super(10, 11);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create task_assignments table (this was missing!)
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `task_assignments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER NOT NULL, " +
                        "`assignedTo` TEXT NOT NULL, " +
                        "`assignedAt` INTEGER NOT NULL, " +
                        "`unassignedAt` INTEGER, " +
                        "`farmId` INTEGER NOT NULL" +
                        ")"
        );

        // Create indexes for performance
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_task_assignments_taskId` ON `task_assignments` (`taskId`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_task_assignments_assignedTo` ON `task_assignments` (`assignedTo`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_task_assignments_farmId` ON `task_assignments` (`farmId`)"
        );

        // Existing indexes (these are fine to keep)
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tasks_farmId` ON `tasks` (`farmId`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tasks_status` ON `tasks` (`status`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_farmId` ON `expenses` (`farmId`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_revenues_farmId` ON `revenues` (`farmId`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_photos_blockId` ON `photos` (`blockId`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sync_queue_status` ON `sync_queue` (`status`)"
        );
    }
}