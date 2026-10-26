package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration28To29 extends Migration {

    private static final String TAG = "Migration28To29";

    public Migration28To29() {
        super(28, 29);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 28 to 29 - Adding Compliance Verifications table");

        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS compliance_verifications (" +
                    "verificationId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "farmId INTEGER NOT NULL, " +
                    "verificationType TEXT, " +
                    "verificationDate INTEGER NOT NULL DEFAULT 0, " +
                    "verifiedBy TEXT, " +
                    "status TEXT, " +
                    "evidenceJson TEXT, " +
                    "comment TEXT, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_verifications_certificationId ON compliance_verifications(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_verifications_farmId ON compliance_verifications(farmId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_verifications_status ON compliance_verifications(status)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_verifications_type ON compliance_verifications(verificationType)");

            Log.d(TAG, "✅ Compliance Verifications table created successfully");
            Log.d(TAG, "✅ Migration 28->29 completed successfully!");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 28->29 failed!", e);
            throw e;
        }
    }
}