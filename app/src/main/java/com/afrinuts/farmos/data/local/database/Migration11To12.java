package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration11To12 extends Migration {
    public Migration11To12() {
        super(11, 12);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Step 1: Create temporary table with the EXACT schema Room expects
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `users_new` (" +
                        "`uid` TEXT, " +
                        "`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, " +
                        "`phone` TEXT, " +
                        "`email` TEXT, " +
                        "`passwordHash` TEXT, " +
                        "`pinHash` TEXT, " +
                        "`authMethod` TEXT NOT NULL, " +
                        "`lastLoginAt` INTEGER NOT NULL DEFAULT 0, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`isActive` INTEGER NOT NULL DEFAULT 1, " +
                        "`farmId` INTEGER NOT NULL DEFAULT 1" +
                        ")"
        );

        // Step 2: Copy data from old table - use COALESCE with explicit defaults
        database.execSQL(
                "INSERT INTO `users_new` (" +
                        "`localId`, `name`, `role`, `phone`, `email`, " +
                        "`passwordHash`, `pinHash`, `authMethod`, `lastLoginAt`, " +
                        "`createdAt`, `updatedAt`, `isActive`, `farmId`, `uid`" +
                        ") SELECT " +
                        "`id`, " +
                        "COALESCE(`name`, ''), " +
                        "COALESCE(`role`, 'WORKER'), " +
                        "`phone`, " +
                        "`email`, " +
                        "`passwordHash`, " +
                        "`pinHash`, " +
                        "COALESCE(`authMethod`, 'PIN'), " +
                        "COALESCE(`lastLoginAt`, 0), " +
                        "COALESCE(`createdAt`, 0), " +
                        "COALESCE(`updatedAt`, 0), " +
                        "COALESCE(`isActive`, 1), " +
                        "1, " +  // Default farmId = 1
                        "NULL " +
                        "FROM `users`"
        );

        // Step 3: Drop old table
        database.execSQL("DROP TABLE `users`");

        // Step 4: Rename new table
        database.execSQL("ALTER TABLE `users_new` RENAME TO `users`");

        // Step 5: Create all indexes
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_users_uid` ON `users` (`uid`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_users_role` ON `users` (`role`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_users_farmId` ON `users` (`farmId`)"
        );
    }
}