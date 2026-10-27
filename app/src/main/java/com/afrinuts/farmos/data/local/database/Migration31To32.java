package com.afrinuts.farmos.data.local.database;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration31To32 extends Migration {
    private static final String TAG = "Migration31To32";

    public Migration31To32() { super(31, 32); }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        Log.d(TAG, "🚀 Migration 31 -> 32: cooperative linkage + farmer ID on suppliers");
        try {
            db.execSQL("ALTER TABLE suppliers ADD COLUMN cooperativeName TEXT");
            db.execSQL("ALTER TABLE suppliers ADD COLUMN cooperativeId TEXT");
            db.execSQL("ALTER TABLE suppliers ADD COLUMN farmerIdNumber TEXT");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_suppliers_cooperativeId ON suppliers(cooperativeId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_suppliers_type ON suppliers(type)");

            Log.d(TAG, "✅ Migration 31 -> 32 complete");
        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 31 -> 32 failed", e);
            throw e;
        }
    }
}