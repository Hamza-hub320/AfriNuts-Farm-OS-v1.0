package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration13To14 extends Migration {
    public Migration13To14() {
        super(13, 14);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        Log.d("Migration", "Starting Migration 13 → 14: Adding harvest cycle fields to revenues");

        // Add harvest cycle columns to revenues table with NOT NULL constraints
        try {
            database.execSQL("ALTER TABLE revenues ADD COLUMN harvestCycleId TEXT");
            Log.d("Migration", "✓ Added harvestCycleId column");
        } catch (Exception e) {
            Log.e("Migration", "Error adding harvestCycleId: " + e.getMessage());
        }

        try {
            // Add with NOT NULL constraint and default value 0
            database.execSQL("ALTER TABLE revenues ADD COLUMN harvestYear INTEGER NOT NULL DEFAULT 0");
            Log.d("Migration", "✓ Added harvestYear column (NOT NULL)");
        } catch (Exception e) {
            Log.e("Migration", "Error adding harvestYear: " + e.getMessage());
        }

        try {
            // Add with NOT NULL constraint and default value 1
            database.execSQL("ALTER TABLE revenues ADD COLUMN harvestSeason INTEGER NOT NULL DEFAULT 1");
            Log.d("Migration", "✓ Added harvestSeason column (NOT NULL)");
        } catch (Exception e) {
            Log.e("Migration", "Error adding harvestSeason: " + e.getMessage());
        }

        // Update existing revenues with default harvest cycle values
        try {
            // SQLite doesn't have strftime for timestamps, use this approach
            database.execSQL(
                    "UPDATE revenues SET " +
                            "harvestYear = CAST((harvestDate / 1000 / 86400 / 365.2425) + 1970 AS INTEGER), " +
                            "harvestCycleId = 'BLOCK_' || COALESCE(blockId, 0) || '_YEAR_' || " +
                            "CAST((harvestDate / 1000 / 86400 / 365.2425) + 1970 AS INTEGER) " +
                            "WHERE harvestYear = 0"
            );
            Log.d("Migration", "✓ Updated existing revenues with harvest cycle values");
        } catch (Exception e) {
            Log.e("Migration", "Error updating existing revenues: " + e.getMessage());
        }

        // Create indexes for performance
        try {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_revenues_harvestYear` ON `revenues` (`harvestYear`)");
            Log.d("Migration", "✓ Created index on harvestYear");
        } catch (Exception e) {
            Log.e("Migration", "Error creating harvestYear index: " + e.getMessage());
        }

        try {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_revenues_harvestCycleId` ON `revenues` (`harvestCycleId`)");
            Log.d("Migration", "✓ Created index on harvestCycleId");
        } catch (Exception e) {
            Log.e("Migration", "Error creating harvestCycleId index: " + e.getMessage());
        }

        Log.d("Migration", "Migration 13 → 14 completed successfully");
    }
}