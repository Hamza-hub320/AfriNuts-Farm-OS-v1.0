package com.afrinuts.farmos.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Adds the optional harvest date to quality batches. */
public class Migration48To49 extends Migration {
    public Migration48To49() { super(48, 49); }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE quality_batches ADD COLUMN harvestDate INTEGER NOT NULL DEFAULT 0");
    }
}
