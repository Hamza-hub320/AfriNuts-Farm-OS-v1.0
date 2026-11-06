package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration39To40 extends Migration {

    private static final String TAG = "Migration39To40";

    public Migration39To40() {
        super(39, 40);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Migration 39 -> 40: adding supplier_score_history");

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `supplier_score_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`supplierId` INTEGER NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`overall` REAL, " +
                        "`quality` REAL, " +
                        "`delivery` REAL, " +
                        "`traceability` REAL, " +
                        "`compliance` REAL, " +
                        "`sustainability` REAL, " +
                        "`cooperation` REAL, " +
                        "`confidence` TEXT, " +
                        "`verifiedTransactions` INTEGER NOT NULL DEFAULT 0, " +
                        "`scoringVersion` TEXT, " +
                        "`weightsUsed` TEXT, " +
                        "`calculatedAt` INTEGER NOT NULL, " +
                        "`calculatedBy` TEXT, " +
                        "`evidenceRefs` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                        ")"
        );

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_score_history_supplierId_calculatedAt` " +
                "ON `supplier_score_history` (`supplierId`, `calculatedAt`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_score_history_scoringVersion` " +
                "ON `supplier_score_history` (`scoringVersion`)");

        Log.d(TAG, "✅ supplier_score_history created with 2 indexes");
    }
}