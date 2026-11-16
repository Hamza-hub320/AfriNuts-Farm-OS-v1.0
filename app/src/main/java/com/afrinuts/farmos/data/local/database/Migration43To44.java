package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * 43 -> 44 (Stage 1, step 4 validation fields):
 * validatedAt / validatedBy on the three append-only record types.
 * Both nullable, no default. null validatedAt = not yet validated.
 */
public class Migration43To44 extends Migration {

    private static final String TAG = "Migration43To44";

    public Migration43To44() {
        super(43, 44);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        addValidationFields(database, "audits");
        addValidationFields(database, "inspections");
        addValidationFields(database, "compliance_verifications");
    }

    private void addValidationFields(SupportSQLiteDatabase database, String table) {
        try {
            database.execSQL("ALTER TABLE " + table + " ADD COLUMN validatedAt INTEGER;");
            database.execSQL("ALTER TABLE " + table + " ADD COLUMN validatedBy TEXT;");
            Log.d(TAG, "Added validation fields to " + table);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add validation fields to " + table, e);
            throw e;
        }
    }
}