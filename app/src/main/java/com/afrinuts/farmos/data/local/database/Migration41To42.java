package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migration 41 → 42.
 *
 * Single nullable column on tasks. No rebuild — one ALTER, no data migration.
 * Existing rows keep startDate = NULL, which means "no start recorded" and
 * leaves every pre-42 task valid under the new schema.
 */
public class Migration41To42 extends Migration {

    private static final String TAG = "Migration41To42";

    public Migration41To42() {
        super(41, 42);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Migration 41 -> 42: tasks.startDate");
        try {
            database.execSQL("ALTER TABLE tasks ADD COLUMN startDate INTEGER");
            Log.d(TAG, "✅ Migration 41 -> 42 completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 41 -> 42 failed", e);
            throw e;
        }
    }
}