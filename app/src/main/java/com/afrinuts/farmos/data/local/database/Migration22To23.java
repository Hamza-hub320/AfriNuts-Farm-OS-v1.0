package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration22To23 extends Migration {

    private static final String TAG = "Migration21To22";

    public Migration22To23() {
        super(21, 22);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 22 to 23 - Adding processing fields to batch traceability");

        try {
            // Add new columns to batch_traceability table
            database.execSQL("ALTER TABLE batch_traceability ADD COLUMN processingLocation TEXT");
            database.execSQL("ALTER TABLE batch_traceability ADD COLUMN processingMethod TEXT");
            database.execSQL("ALTER TABLE batch_traceability ADD COLUMN processingFacility TEXT");
            database.execSQL("ALTER TABLE batch_traceability ADD COLUMN kernelWeightKg REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE batch_traceability ADD COLUMN yieldPercentage REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE batch_traceability ADD COLUMN shellingMethod TEXT");
            database.execSQL("ALTER TABLE batch_traceability ADD COLUMN gradingMethod TEXT");

            Log.d(TAG, "✅ Added processing fields to batch_traceability table");
            Log.d(TAG, "✅ Migration 21 -> 22 completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 22 -> 23 failed", e);
            throw e;
        }
    }
}