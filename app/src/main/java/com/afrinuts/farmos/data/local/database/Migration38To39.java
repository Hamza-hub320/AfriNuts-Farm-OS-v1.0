package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration38To39 extends Migration {

    private static final String TAG = "Migration38To39";

    public Migration38To39() {
        super(38, 39);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Migration 38 -> 39: adding supplier_disputes");

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `supplier_disputes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`supplierId` INTEGER NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`targetEntity` TEXT, " +
                        "`targetId` INTEGER NOT NULL, " +
                        "`reason` TEXT, " +
                        "`evidenceUrls` TEXT, " +
                        "`submittedBy` TEXT, " +
                        "`submittedDate` INTEGER NOT NULL, " +
                        "`reviewStatus` TEXT, " +
                        "`decision` TEXT, " +
                        "`reviewedBy` TEXT, " +
                        "`resolvedDate` INTEGER, " +
                        "`correctionApplied` INTEGER NOT NULL DEFAULT 0, " +
                        "`originalValue` TEXT, " +
                        "`correctedValue` TEXT, " +
                        "`recalculatedSnapshotId` INTEGER, " +
                        "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                        "`createdBy` TEXT, " +
                        "`updatedBy` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                        ")"
        );

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_disputes_supplierId_submittedDate` " +
                "ON `supplier_disputes` (`supplierId`, `submittedDate`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_disputes_targetEntity_targetId` " +
                "ON `supplier_disputes` (`targetEntity`, `targetId`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_disputes_reviewStatus` " +
                "ON `supplier_disputes` (`reviewStatus`)");

        Log.d(TAG, "✅ supplier_disputes created with 3 indexes");
    }
}