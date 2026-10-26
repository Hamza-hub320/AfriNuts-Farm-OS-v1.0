package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration26To27 extends Migration {

    private static final String TAG = "Migration26To27";

    public Migration26To27() {
        super(26, 27);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 26 to 27 - Adding Audit table");

        try {
            // Create audits table
            database.execSQL("CREATE TABLE IF NOT EXISTS audits (" +
                    "auditId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "farmId INTEGER NOT NULL, " +
                    "auditType TEXT, " +
                    "auditDate INTEGER NOT NULL DEFAULT 0, " +
                    "auditorName TEXT, " +
                    "auditorBody TEXT, " +
                    "scope TEXT, " +
                    "findingsJson TEXT, " +
                    "status TEXT, " +
                    "reportUrl TEXT, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            // Create indexes
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_audits_certificationId ON audits(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_audits_farmId ON audits(farmId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_audits_status ON audits(status)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_audits_auditDate ON audits(auditDate)");

            Log.d(TAG, "✅ Audits table created successfully");
            Log.d(TAG, "✅ Migration 26->27 completed successfully!");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 26->27 failed!", e);
            throw e;
        }
    }
}