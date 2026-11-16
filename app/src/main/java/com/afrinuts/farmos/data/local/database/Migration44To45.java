package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * 44 -> 45 (Stage 1, step 5 status history):
 * statusHistoryJson on certifications only — a JSON event log.
 * Nullable; null = no events yet (a pre-step-5 record).
 */
public class Migration44To45 extends Migration {

    private static final String TAG = "Migration44To45";

    public Migration44To45() {
        super(44, 45);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        try {
            database.execSQL("ALTER TABLE certifications ADD COLUMN statusHistoryJson TEXT;");
            Log.d(TAG, "Added statusHistoryJson to certifications");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add statusHistoryJson to certifications", e);
            throw e;
        }
    }
}