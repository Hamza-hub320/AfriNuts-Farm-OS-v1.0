package com.afrinuts.farmos.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Stores the latest failure reason alongside a queued sync item. */
public class Migration47To48 extends Migration {
    public Migration47To48() { super(47, 48); }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE sync_queue ADD COLUMN errorMessage TEXT");
    }
}
