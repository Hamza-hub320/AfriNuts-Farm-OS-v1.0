package com.afrinuts.farmos.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration49To50 extends Migration {
    public Migration49To50() { super(49, 50); }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `integrity_scan_runs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `farmId` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, `recordsScanned` INTEGER NOT NULL, `rulesEvaluated` INTEGER NOT NULL, `issuesDetected` INTEGER NOT NULL, `status` TEXT, `triggeredBy` TEXT, `pendingSyncCount` INTEGER NOT NULL)");
        database.execSQL("CREATE TABLE IF NOT EXISTS `integrity_findings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scanId` INTEGER NOT NULL, `farmId` INTEGER NOT NULL, `entityType` TEXT, `entityId` TEXT, `ruleCode` TEXT, `severity` TEXT, `description` TEXT, `expectedValue` TEXT, `actualValue` TEXT, `expectedNumeric` REAL, `actualNumeric` REAL, `status` TEXT, `detectedAt` INTEGER NOT NULL, `resolvedAt` INTEGER, `resolvedBy` TEXT, `resolutionNotes` TEXT, `isSynced` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `historyJson` TEXT)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_integrity_findings_farmId_ruleCode_entityId_status` ON `integrity_findings` (`farmId`, `ruleCode`, `entityId`, `status`)");
    }
}
