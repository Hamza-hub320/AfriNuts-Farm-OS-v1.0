package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration14To15 extends Migration {

    public Migration14To15() {
        super(14, 15);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create quality_batches table - MATCHING ROOM EXPECTATION
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `quality_batches` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`farmId` INTEGER NOT NULL, " +
                        "`blockId` INTEGER, " +
                        "`harvestCycleId` INTEGER, " +
                        "`batchNumber` TEXT, " +  // ← Changed: NOT NULL removed
                        "`processingDate` INTEGER NOT NULL, " +
                        "`moistureContent` REAL NOT NULL, " +
                        "`nutCountPerKg` INTEGER NOT NULL, " +
                        "`defectiveRate` REAL NOT NULL, " +
                        "`kernelRatio` REAL NOT NULL, " +
                        "`weightKg` REAL NOT NULL, " +
                        "`qualityGrade` TEXT, " +  // ← Changed: NOT NULL removed
                        "`testResults` TEXT, " +
                        "`qualityScore` REAL NOT NULL, " +
                        "`notes` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON DELETE CASCADE, " +
                        "FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) ON DELETE SET NULL, " +
                        "FOREIGN KEY(`harvestCycleId`) REFERENCES `revenues`(`id`) ON DELETE SET NULL" +
                        ")"
        );

        // Create indexes
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_farmId` ON `quality_batches` (`farmId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_blockId` ON `quality_batches` (`blockId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_harvestCycleId` ON `quality_batches` (`harvestCycleId`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_qualityGrade` ON `quality_batches` (`qualityGrade`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_processingDate` ON `quality_batches` (`processingDate`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_qualityScore` ON `quality_batches` (`qualityScore`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quality_batches_batchNumber` ON `quality_batches` (`batchNumber`)"
        );
    }
}