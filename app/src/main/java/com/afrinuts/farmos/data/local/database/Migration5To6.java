package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration5To6 extends Migration {

    public Migration5To6() {
        super(5, 6);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create tasks table
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `tasks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`blockId` INTEGER, " +
                        "`title` TEXT NOT NULL, " +
                        "`description` TEXT, " +
                        "`status` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`dueDate` INTEGER NOT NULL, " +
                        "`completedDate` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE, " +
                        "FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) ON DELETE SET NULL" +
                        ")"
        );

        // Create task_assignments table
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `task_assignments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER NOT NULL, " +
                        "`assignedTo` TEXT NOT NULL, " +
                        "`assignedAt` INTEGER NOT NULL, " +
                        "`unassignedAt` INTEGER, " +
                        "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON DELETE CASCADE" +
                        ")"
        );

        // Create indexes
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tasks_farmId` ON `tasks` (`farmId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tasks_blockId` ON `tasks` (`blockId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tasks_status` ON `tasks` (`status`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tasks_dueDate` ON `tasks` (`dueDate`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_task_assignments_taskId` ON `task_assignments` (`taskId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_task_assignments_assignedTo` ON `task_assignments` (`assignedTo`)"
        );
    }
}