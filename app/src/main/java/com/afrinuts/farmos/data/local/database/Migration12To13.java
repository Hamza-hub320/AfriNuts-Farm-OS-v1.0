package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration12To13 extends Migration {
    public Migration12To13() {
        super(12, 13);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Add createdAt and updatedAt columns to task_assignments table
        database.execSQL("ALTER TABLE task_assignments ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE task_assignments ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0");

        // Update existing rows to have current timestamp
        long now = System.currentTimeMillis();
        database.execSQL("UPDATE task_assignments SET createdAt = " + now + ", updatedAt = " + now);

        Log.d("Migration", "Added createdAt and updatedAt to task_assignments");
    }
}