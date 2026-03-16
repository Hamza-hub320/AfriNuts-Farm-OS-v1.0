package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration10To11 extends Migration {
    public Migration10To11() {
        super(10, 11);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // No changes needed - this is a placeholder
        // This migration exists just to handle version increments without schema changes
    }
}