package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration21To22 extends Migration {

    private static final String TAG = "Migration21To22";

    public Migration21To22() {
        super(21, 22);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 21 to 22 - Adding Verification Codes");

        try {
            // 1. Add verificationCode to quality_batches
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN verificationCode TEXT");
            Log.d(TAG, "✅ Added verificationCode to quality_batches");

            // 2. Add verificationCode to batch_traceability
            database.execSQL("ALTER TABLE batch_traceability ADD COLUMN verificationCode TEXT");
            Log.d(TAG, "✅ Added verificationCode to batch_traceability");

            // 3. Add verificationCode to master_batch_traceability
            database.execSQL("ALTER TABLE master_batch_traceability ADD COLUMN verificationCode TEXT");
            Log.d(TAG, "✅ Added verificationCode to master_batch_traceability");

            Log.d(TAG, "✅ Migration 21 -> 22 completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 21 -> 22 failed", e);
            throw e;
        }
    }
}