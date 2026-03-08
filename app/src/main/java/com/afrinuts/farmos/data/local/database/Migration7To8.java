package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration7To8 extends Migration {

    public Migration7To8() {
        super(7, 8);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create users table
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `users` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, " +
                        "`phone` TEXT, " +
                        "`email` TEXT UNIQUE, " +
                        "`passwordHash` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`isActive` INTEGER NOT NULL DEFAULT 1" +
                        ")"
        );

        // Create index for email lookups
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_users_role` ON `users` (`role`)"
        );
    }
}