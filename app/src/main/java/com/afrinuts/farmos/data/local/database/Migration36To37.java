package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration36To37 extends Migration {

    private static final String TAG = "Migration36To37";

    public Migration36To37() {
        super(36, 37);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Migration 36 -> 37: adding supplier_commitments");

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `supplier_commitments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`supplierId` INTEGER NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`contractId` TEXT, " +
                        "`season` TEXT, " +
                        "`productType` TEXT, " +
                        "`committedKg` REAL NOT NULL, " +
                        "`committedDate` INTEGER NOT NULL, " +
                        "`linkedBatchIds` TEXT, " +
                        "`status` TEXT, " +
                        "`delayReason` TEXT, " +
                        "`buyerRequestedChange` INTEGER NOT NULL DEFAULT 0, " +
                        "`revisions` TEXT, " +
                        "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                        "`validatedAt` INTEGER, " +
                        "`validatedBy` TEXT, " +
                        "`createdBy` TEXT, " +
                        "`updatedBy` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                        ")"
        );

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_commitments_supplierId_season` " +
                "ON `supplier_commitments` (`supplierId`, `season`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_commitments_validatedAt` " +
                "ON `supplier_commitments` (`validatedAt`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_commitments_isSynced` " +
                "ON `supplier_commitments` (`isSynced`)");

        Log.d(TAG, "✅ supplier_commitments created with 3 indexes");
    }
}