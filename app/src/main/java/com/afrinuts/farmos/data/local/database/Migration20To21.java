package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration20To21 extends Migration {

    private static final String TAG = "Migration20To21";

    public Migration20To21() {
        super(20, 21);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 20 to 21 - Adding Master Traceability");

        try {
            // 1. Create master_batch_traceability table
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS master_batch_traceability (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "masterBatchNumber TEXT NOT NULL UNIQUE, " +
                            "shipmentName TEXT, " +
                            "shipmentId TEXT, " +
                            "totalWeightKg REAL NOT NULL DEFAULT 0, " +
                            "totalBatches INTEGER NOT NULL DEFAULT 0, " +
                            "grades TEXT, " +
                            "certifications TEXT, " +
                            "batchNumbersJson TEXT, " +
                            "batchTraceabilityIdsJson TEXT, " +
                            "buyer TEXT, " +
                            "destination TEXT, " +
                            "exportDate INTEGER, " +
                            "traceabilityUrl TEXT, " +
                            "createdAt INTEGER NOT NULL, " +
                            "updatedAt INTEGER NOT NULL, " +
                            "isSynced INTEGER NOT NULL DEFAULT 0)"
            );
            Log.d(TAG, "✅ Created master_batch_traceability table");

            // 2. Create indexes for better performance
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_master_batch_number ON master_batch_traceability(masterBatchNumber)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_master_buyer ON master_batch_traceability(buyer)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_master_export_date ON master_batch_traceability(exportDate)");
            Log.d(TAG, "✅ Created indexes on master_batch_traceability table");

            Log.d(TAG, "✅ Migration 20 -> 21 completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 20 -> 21 failed", e);
            throw e;
        }
    }
}