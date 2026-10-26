package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration29To30 extends Migration {

    private static final String TAG = "Migration29To30";

    public Migration29To30() {
        super(29, 30);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 29 to 30 - Adding Documents table");

        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS compliance_documents (" +
                    "documentId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "farmId INTEGER NOT NULL, " +
                    "documentType TEXT, " +
                    "documentName TEXT, " +
                    "issueDate INTEGER NOT NULL DEFAULT 0, " +
                    "expiryDate INTEGER NOT NULL DEFAULT 0, " +
                    "fileUrl TEXT, " +
                    "status TEXT, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_certificationId ON compliance_documents(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_farmId ON compliance_documents(farmId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_status ON compliance_documents(status)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_type ON compliance_documents(documentType)");

            Log.d(TAG, "✅ Documents table created successfully");
            Log.d(TAG, "✅ Migration 29->30 completed successfully!");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 29->30 failed!", e);
            throw e;
        }
    }
}