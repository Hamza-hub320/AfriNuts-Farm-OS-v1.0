package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration23To24 extends Migration {

    private static final String TAG = "Migration23To24";

    public Migration23To24() {
        super(23, 24);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 23 to 24 - Adding harvest tracking to blocks");

        try {
            // ✅ Add harvest tracking columns to blocks table
            database.execSQL("ALTER TABLE blocks ADD COLUMN harvestHistory TEXT");
            database.execSQL("ALTER TABLE blocks ADD COLUMN lastHarvestDate INTEGER");

            // ✅ FIX: Use REAL (not NOT NULL) to allow null values
            database.execSQL("ALTER TABLE blocks ADD COLUMN lastHarvestQuantity REAL");
            database.execSQL("ALTER TABLE blocks ADD COLUMN totalHarvestQuantity REAL");
            database.execSQL("ALTER TABLE blocks ADD COLUMN harvestCount INTEGER");

            Log.d(TAG, "✅ Added harvest tracking columns to blocks table");

            // ✅ Initialize existing blocks with default values
            database.execSQL("UPDATE blocks SET " +
                    "harvestHistory = '[]', " +
                    "lastHarvestDate = NULL, " +
                    "lastHarvestQuantity = 0, " +
                    "totalHarvestQuantity = 0, " +
                    "harvestCount = 0 " +
                    "WHERE harvestHistory IS NULL");

            Log.d(TAG, "✅ Initialized harvest fields for existing blocks");

            // ✅ Create index for faster harvest queries
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_blocks_last_harvest_date ON blocks(lastHarvestDate)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_blocks_harvest_count ON blocks(harvestCount)");

            Log.d(TAG, "✅ Created indexes for harvest tracking");

            Log.d(TAG, "✅ Migration 23 -> 24 completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 23 -> 24 failed", e);
            throw e;
        }
    }
}