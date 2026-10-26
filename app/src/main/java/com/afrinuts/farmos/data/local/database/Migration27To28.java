package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration27To28 extends Migration {

    private static final String TAG = "Migration27To28";

    public Migration27To28() {
        super(27, 28);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 27 to 28 - Adding Inspections table");

        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS inspections (" +
                    "inspectionId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "certificationName TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "requirementCategory TEXT, " +
                    "inspectionType TEXT, " +
                    "inspectionDate INTEGER NOT NULL DEFAULT 0, " +
                    "inspectorName TEXT, " +
                    "inspectorBody TEXT, " +
                    "status TEXT, " +
                    "remarks TEXT, " +
                    "correctiveActionsJson TEXT, " +
                    "photosJson TEXT, " +
                    "reportUrl TEXT, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_inspections_certificationId ON inspections(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_inspections_farmId ON inspections(farmId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_inspections_status ON inspections(status)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_inspections_category ON inspections(requirementCategory)");

            Log.d(TAG, "✅ Inspections table created successfully");
            Log.d(TAG, "✅ Migration 27->28 completed successfully!");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 27->28 failed!", e);
            throw e;
        }
    }
}