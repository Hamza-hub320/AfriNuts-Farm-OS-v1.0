package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Step 1 of the RCN/kernel split:
 *  - Adds the product discriminator and the RCN-only / kernel-only columns.
 *  - Backfills every existing row as productType = "KERNEL"
 *    (all legacy batches were recorded with kernel inputs).
 *  - Does NOT change grade logic, UI, or stored scores.
 *    New columns are inert until Step 2 introduces QualityGradeCalculator.
 */
public class Migration32To33 extends Migration {

    private static final String TAG = "Migration32To33";

    public Migration32To33() {
        super(32, 33);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration 32 -> 33 - RCN/kernel split (Step 1)");

        try {
            // Discriminator
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN productType TEXT");

            // RCN-only fields
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN mouldyRate REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN immatureRate REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN insectDamagedRate REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN shrivelledRate REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN foreignMatter REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN dryingMethod TEXT");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN rcnQualityScore REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN korPercent REAL NOT NULL DEFAULT 0");

            // Kernel-only fields
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN brokenKernels REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN aflatoxinPpb REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN heavyMetalsPpb REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN pesticideResiduePpb REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN kernelQualityScore REAL NOT NULL DEFAULT 0");

            // Backfill: every existing row is a kernel batch.
            database.execSQL("UPDATE quality_batches SET productType = 'KERNEL' WHERE productType IS NULL");

            // Pull the three ppb metrics out of the testResults JSON into the new columns.
            // testResults is a Gson-serialized TestResults object with keys
            // aflatoxin, pesticideResidue, heavyMetals. SQLite's json_extract is available
            // on API 24+ via Room's bundled SQLite; if the build targets older devices,
            // run this backfill in Kotlin/Java in the repository instead and drop these lines.
            database.execSQL(
                    "UPDATE quality_batches SET aflatoxinPpb = " +
                            "  COALESCE(CAST(json_extract(testResults, '$.aflatoxin') AS REAL), 0) " +
                            "WHERE testResults IS NOT NULL AND testResults LIKE '%aflatoxin%'"
            );
            database.execSQL(
                    "UPDATE quality_batches SET pesticideResiduePpb = " +
                            "  COALESCE(CAST(json_extract(testResults, '$.pesticideResidue') AS REAL), 0) " +
                            "WHERE testResults IS NOT NULL AND testResults LIKE '%pesticideResidue%'"
            );
            database.execSQL(
                    "UPDATE quality_batches SET heavyMetalsPpb = " +
                            "  COALESCE(CAST(json_extract(testResults, '$.heavyMetals') AS REAL), 0) " +
                            "WHERE testResults IS NOT NULL AND testResults LIKE '%heavyMetals%'"
            );

            Log.d(TAG, "✅ Added RCN/kernel columns and backfilled legacy rows as KERNEL");
            Log.d(TAG, "✅ Migration 32 -> 33 completed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 32 -> 33 failed", e);
            throw e;
        }
    }
}