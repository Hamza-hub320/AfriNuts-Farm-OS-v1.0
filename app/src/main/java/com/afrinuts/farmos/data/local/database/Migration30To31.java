package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Adds granular farmer data-consent fields to the suppliers table.
 *
 * New columns:
 *  - dataConsentGiven           (INTEGER  NOT NULL DEFAULT 0)
 *  - dataConsentDate            (INTEGER  NOT NULL DEFAULT 0)   epoch millis
 *  - dataConsentScope           (TEXT)                           JSON array: ["identity","location","photo"]
 *  - dataConsentSignedBy        (TEXT)
 *  - dataConsentVersion         (TEXT)
 *  - dataConsentText            (TEXT)                           full immutable clause (audit)
 *  - dataConsentWithdrawnDate   (TEXT)                           ISO-8601 or null
 *  - dataConsentWithdrawnReason (TEXT)
 */
public class Migration30To31 extends Migration {

    private static final String TAG = "Migration30To31";

    public Migration30To31() {
        super(30, 31);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration 30 -> 31: farmer data-consent fields on suppliers");

        try {
            // ========================================
            // 1. Consent columns
            // ========================================
            database.execSQL(
                    "ALTER TABLE suppliers ADD COLUMN dataConsentGiven INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE suppliers ADD COLUMN dataConsentDate INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE suppliers ADD COLUMN dataConsentScope TEXT");
            database.execSQL(
                    "ALTER TABLE suppliers ADD COLUMN dataConsentSignedBy TEXT");
            database.execSQL(
                    "ALTER TABLE suppliers ADD COLUMN dataConsentVersion TEXT");
            database.execSQL(
                    "ALTER TABLE suppliers ADD COLUMN dataConsentText TEXT");
            database.execSQL(
                    "ALTER TABLE suppliers ADD COLUMN dataConsentWithdrawnDate TEXT");
            database.execSQL(
                    "ALTER TABLE suppliers ADD COLUMN dataConsentWithdrawnReason TEXT");

            Log.d(TAG, "✅ Added 8 consent columns to suppliers");

            // ========================================
            // 2. Backfill existing rows
            //    - no consent yet → empty JSON array, given=false
            //    - preserves any pre-existing dataConsentGiven=0 rows
            // ========================================
            database.execSQL(
                    "UPDATE suppliers SET dataConsentScope = '[]' " +
                            "WHERE dataConsentScope IS NULL");

            Log.d(TAG, "✅ Backfilled dataConsentScope for existing suppliers");

            // ========================================
            // 3. Indexes used by the Buyer Portal / audit queries
            // ========================================
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_suppliers_consent_given " +
                            "ON suppliers(dataConsentGiven)");
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_suppliers_consent_date " +
                            "ON suppliers(dataConsentDate)");

            Log.d(TAG, "✅ Created indexes on suppliers.dataConsentGiven / dataConsentDate");
            Log.d(TAG, "✅ Migration 30 -> 31 completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 30 -> 31 failed", e);
            throw e;
        }
    }
}