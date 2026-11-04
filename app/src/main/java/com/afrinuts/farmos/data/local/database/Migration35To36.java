package com.afrinuts.farmos.data.local.database;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migration 35 -> 36: promote inspectionDate and inspectedBy to real fields.
 *
 * Previously:
 *  - inspection timestamp was stored in processingDate (RCN only)
 *  - inspector name was stored in notes as "Inspected by X | ..."
 *
 * After this migration:
 *  - inspectionDate  = processingDate for RCN batches
 *  - inspectedBy     = parsed from notes prefix if present
 *  - notes prefix "Inspected by X |" is stripped from the notes field
 *
 * processingDate is left alone (harmless: for RCN it duplicates inspectionDate,
 * for kernel it will hold the real processing timestamp in Phase Two).
 */
public class Migration35To36 extends Migration {

    private static final String TAG = "Migration35To36";
    private static final String NOTES_PREFIX = "Inspected by ";

    public Migration35To36() { super(35, 36); }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Migration 35 -> 36: promote inspectionDate + inspectedBy");

        try {
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN inspectionDate INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quality_batches ADD COLUMN inspectedBy TEXT");

            // 1. Backfill inspectionDate from processingDate for RCN batches
            database.execSQL(
                    "UPDATE quality_batches SET inspectionDate = processingDate " +
                            "WHERE productType = 'RCN' AND processingDate > 0"
            );

            // 2. Backfill inspectedBy by extracting "Inspected by X |" from notes
            Cursor cursor = database.query(
                    "SELECT id, notes FROM quality_batches " +
                            "WHERE notes IS NOT NULL AND notes LIKE 'Inspected by %'"
            );
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        String notes = cursor.getString(1);
                        if (notes == null) continue;

                        String inspector = null;
                        String cleanNotes = notes;

                        if (notes.startsWith(NOTES_PREFIX)) {
                            int pipeIdx = notes.indexOf('|', NOTES_PREFIX.length());
                            if (pipeIdx > 0) {
                                inspector = notes.substring(NOTES_PREFIX.length(), pipeIdx).trim();
                                cleanNotes = notes.substring(pipeIdx + 1).trim();
                            } else {
                                // no pipe — the whole rest is the inspector name
                                inspector = notes.substring(NOTES_PREFIX.length()).trim();
                                cleanNotes = "";
                            }
                        }

                        if (inspector != null && !inspector.isEmpty()) {
                            database.execSQL(
                                    "UPDATE quality_batches SET inspectedBy = ?, notes = ? WHERE id = ?",
                                    new Object[]{inspector, cleanNotes.isEmpty() ? null : cleanNotes, id}
                            );
                            Log.d(TAG, "  batch " + id + ": inspector=\"" + inspector
                                    + "\" notes=\"" + cleanNotes + "\"");
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            Log.d(TAG, "✅ Migration 35 -> 36 complete");
        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 35 -> 36 failed", e);
            throw e;
        }
    }
}