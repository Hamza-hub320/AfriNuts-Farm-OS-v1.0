package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * 42 -> 43 (Stage 1, step 3 append-only):
 * supersedesId on the three append-only record types. Nullable, no default,
 * no index. A correction record points supersedesId at the record it corrects.
 */
public class Migration42To43 extends Migration {

    private static final String TAG = "Migration42To43";

    public Migration42To43() {
        super(42, 43);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        addSupersedesId(database, "audits");
        addSupersedesId(database, "inspections");
        addSupersedesId(database, "compliance_verifications");
    }

    private void addSupersedesId(SupportSQLiteDatabase database, String table) {
        try {
            database.execSQL("ALTER TABLE " + table + " ADD COLUMN supersedesId TEXT;");
            Log.d(TAG, "Added supersedesId to " + table);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add supersedesId to " + table, e);
            throw e;
        }
    }
}