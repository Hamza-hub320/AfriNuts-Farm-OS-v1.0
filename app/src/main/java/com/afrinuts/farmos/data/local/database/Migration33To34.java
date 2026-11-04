package com.afrinuts.farmos.data.local.database;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migration 33 -> 34: KOR unit correction.
 *
 * Renames korPercent -> korGPerKg and converts the stored value from
 * whatever the inspector entered (lbs/bag, %, or already g/kg) into g/kg.
 *
 * Value ranges in existing data:
 *   20–30   → true percent → × 10
 *   40–55   → lbs/80kg bag → × 5.67
 *   150–350 → already g/kg → × 1 (leave)
 *   other   → ambiguous → × 5.67 (assume lbs, market convention)
 *
 * Adds two new columns for raw measurements (sample grams, kernel grams).
 */
public class Migration33To34 extends Migration {

    private static final String TAG = "Migration33To34";

    public Migration33To34() { super(33, 34); }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Migration 33 -> 34: KOR unit correction (percent/lbs → g/kg)");

        try {
            // 1. Add the new columns
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN korGPerKg REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN korSampleGrams REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN korKernelGrams REAL NOT NULL DEFAULT 0");

            // 2. Convert existing korPercent values by range.
            //    Order matters: check the tightest ranges first.

            // True percent: 20–30
            database.execSQL(
                    "UPDATE quality_batches SET korGPerKg = korPercent * 10.0 " +
                            "WHERE korPercent >= 20.0 AND korPercent <= 30.0"
            );

            // Lbs per 80 kg bag: 40–55
            database.execSQL(
                    "UPDATE quality_batches SET korGPerKg = korPercent * 5.67 " +
                            "WHERE korPercent >= 40.0 AND korPercent <= 55.0"
            );

            // Already g/kg: 150–350
            database.execSQL(
                    "UPDATE quality_batches SET korGPerKg = korPercent " +
                            "WHERE korPercent >= 150.0 AND korPercent <= 350.0"
            );

            // Anything else that was non-zero and not yet converted:
            // ambiguous — assume lbs (the market convention).
            database.execSQL(
                    "UPDATE quality_batches SET korGPerKg = korPercent * 5.67 " +
                            "WHERE korPercent > 0 AND korGPerKg = 0"
            );

            // 3. Log the distribution for audit (Java cursor style, not Kotlin .use)
            Cursor cursor = database.query(
                    "SELECT batchNumber, korPercent, korGPerKg FROM quality_batches WHERE korPercent > 0");
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        String batchNumber = cursor.getString(0);
                        double oldPercent = cursor.getDouble(1);
                        double newGPerKg = cursor.getDouble(2);
                        Log.d(TAG, "  converted " + batchNumber
                                + ": korPercent=" + oldPercent
                                + " → korGPerKg=" + newGPerKg);
                    }
                } finally {
                    cursor.close();
                }
            }

            // 4. Drop korPercent if SQLite supports DROP COLUMN (3.35+, API 31+).
            //    Otherwise leave it — the entity no longer declares it, so Room
            //    will ignore it on read/write. A later migration drops it.
            boolean canDrop = isSqliteAtLeast(database, 3, 35);
            if (canDrop) {
                database.execSQL("ALTER TABLE quality_batches DROP COLUMN korPercent");
                Log.d(TAG, "✅ Dropped korPercent column (SQLite ≥ 3.35)");
            } else {
                Log.w(TAG, "⚠️ SQLite < 3.35 — keeping korPercent as a dead column. "
                        + "The entity no longer declares it; a future migration drops it.");
            }

            Log.d(TAG, "✅ Migration 33 -> 34 complete");
        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 33 -> 34 failed", e);
            throw e;
        }
    }

    /** Returns true if the SQLite version bundled with the device meets the minimum. */
    private boolean isSqliteAtLeast(SupportSQLiteDatabase db, int major, int minor) {
        Cursor cursor = db.query("SELECT sqlite_version()");
        if (cursor == null) return false;
        try {
            if (cursor.moveToFirst()) {
                String version = cursor.getString(0);
                String[] parts = version.split("\\.");
                int vMajor = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
                int vMinor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                if (vMajor > major) return true;
                if (vMajor == major && vMinor >= minor) return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not determine SQLite version, assuming old", e);
        } finally {
            cursor.close();
        }
        return false;
    }
}