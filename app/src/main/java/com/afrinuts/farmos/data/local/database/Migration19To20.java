package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration19To20 extends Migration {

    private static final String TAG = "Migration19To20";

    public Migration19To20() {
        super(19, 20);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 19 to 20 - Adding Batch Traceability");

        try {
            // 1. Create batch_traceability table
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS batch_traceability (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "batchNumber TEXT NOT NULL UNIQUE, " +
                            "farmId INTEGER NOT NULL, " +
                            "blockId INTEGER, " +
                            "harvestCycleId INTEGER, " +
                            "qualityBatchId INTEGER, " +
                            "supplierId INTEGER, " +
                            "supplierName TEXT, " +
                            "blockName TEXT, " +
                            "qualityGrade TEXT, " +
                            "certificationIds TEXT, " +
                            "sustainabilityMetricsId INTEGER, " +
                            "processingDate INTEGER NOT NULL, " +
                            "exportDate INTEGER, " +
                            "buyer TEXT, " +
                            "destination TEXT, " +
                            "weightKg REAL NOT NULL, " +
                            "grade TEXT NOT NULL, " +
                            "pricePerKg REAL NOT NULL, " +
                            "totalAmount REAL NOT NULL, " +
                            "documents TEXT, " +
                            "traceabilityUrl TEXT, " +
                            "createdAt INTEGER NOT NULL, " +
                            "updatedAt INTEGER NOT NULL, " +
                            "isSynced INTEGER NOT NULL DEFAULT 0, " +
                            "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE, " +
                            "FOREIGN KEY(blockId) REFERENCES blocks(id) ON DELETE SET NULL, " +
                            "FOREIGN KEY(harvestCycleId) REFERENCES revenues(id) ON DELETE SET NULL, " +
                            "FOREIGN KEY(qualityBatchId) REFERENCES quality_batches(id) ON DELETE CASCADE, " +
                            "FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON DELETE SET NULL)"
            );
            Log.d(TAG, "✅ Created batch_traceability table");

            // 2. Create indexes for better performance
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_traceability_batchNumber ON batch_traceability(batchNumber)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_traceability_qualityBatchId ON batch_traceability(qualityBatchId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_traceability_supplierId ON batch_traceability(supplierId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_traceability_exportDate ON batch_traceability(exportDate)");
            Log.d(TAG, "✅ Created indexes on batch_traceability table");

            Log.d(TAG, "✅ Migration 19 -> 20 completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 19 -> 20 failed", e);
            throw e;
        }
    }
}