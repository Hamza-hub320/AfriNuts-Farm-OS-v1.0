package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration37To38 extends Migration {

    private static final String TAG = "Migration37To38";

    public Migration37To38() {
        super(37, 38);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Migration 37 -> 38: adding supplier_inspections");

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `supplier_inspections` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`supplierId` INTEGER NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`inspectionType` TEXT, " +
                        "`inspectionDate` INTEGER NOT NULL, " +
                        "`outcome` TEXT, " +
                        "`severity` TEXT, " +
                        "`findings` TEXT, " +
                        "`correctiveAction` TEXT, " +
                        "`closedDate` INTEGER, " +
                        "`status` TEXT, " +
                        "`inspector` TEXT, " +
                        "`evidenceUrls` TEXT, " +
                        "`linkedBatchIds` TEXT, " +
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

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_inspections_supplierId_inspectionDate` " +
                "ON `supplier_inspections` (`supplierId`, `inspectionDate`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_inspections_validatedAt` " +
                "ON `supplier_inspections` (`validatedAt`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_inspections_isSynced` " +
                "ON `supplier_inspections` (`isSynced`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_inspections_outcome_severity` " +
                "ON `supplier_inspections` (`outcome`, `severity`)");

        Log.d(TAG, "✅ supplier_inspections created with 4 indexes");
    }
}