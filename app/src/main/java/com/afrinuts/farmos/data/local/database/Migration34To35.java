package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migration 34 -> 35: rename PROCESSING -> REJECT in quality_batches.
 *
 * The QualityGrade enum previously had PROCESSING as the third tier.
 * Per SOP §4.3, the third tier is REJECT — the batch is rejected at inspection,
 * not sent for processing. Rename all existing rows.
 */
public class Migration34To35 extends Migration {

    private static final String TAG = "Migration34To35";

    public Migration34To35() { super(34, 35); }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Migration 34 -> 35: rename PROCESSING → REJECT in quality_batches");

        try {
            int updated = countRowsWithGrade(database, "PROCESSING");

            database.execSQL(
                    "UPDATE quality_batches SET qualityGrade = 'REJECT' " +
                            "WHERE qualityGrade = 'PROCESSING'"
            );

            Log.d(TAG, "✅ Renamed " + updated + " PROCESSING rows to REJECT");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 34 -> 35 failed", e);
            throw e;
        }
    }

    private int countRowsWithGrade(SupportSQLiteDatabase db, String grade) {
        android.database.Cursor c = db.query(
                "SELECT COUNT(*) FROM quality_batches WHERE qualityGrade = ?", new Object[]{grade});
        try {
            return c != null && c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }
}