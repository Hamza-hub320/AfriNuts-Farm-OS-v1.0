package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Adds the optional inspection link to photos. */
public class Migration45To46 extends Migration {

    private static final String TAG = "Migration45To46";

    public Migration45To46() {
        super(45, 46);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        try {
            database.execSQL("ALTER TABLE photos ADD COLUMN inspectionId TEXT REFERENCES inspections(inspectionId) ON DELETE SET NULL;");
            Log.d(TAG, "Added inspectionId to photos");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add inspectionId to photos", e);
            throw e;
        }
    }
}
