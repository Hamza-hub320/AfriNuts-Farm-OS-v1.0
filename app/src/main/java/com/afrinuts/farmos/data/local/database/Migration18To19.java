package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration18To19 extends Migration {

    public Migration18To19() {
        super(18, 19);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // ========================================
        // 1. ADD FARMER PROFILE FIELDS TO SUPPLIERS
        // ========================================
        database.execSQL("ALTER TABLE suppliers ADD COLUMN photoUrl TEXT");
        database.execSQL("ALTER TABLE suppliers ADD COLUMN yearsFarming INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE suppliers ADD COLUMN bio TEXT");
        database.execSQL("ALTER TABLE suppliers ADD COLUMN quote TEXT");
        database.execSQL("ALTER TABLE suppliers ADD COLUMN farmSize REAL NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE suppliers ADD COLUMN firebreakProtection INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE suppliers ADD COLUMN sustainableHarvesting INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE suppliers ADD COLUMN noChildLabor INTEGER NOT NULL DEFAULT 0");

        // ========================================
        // 2. ADD SUSTAINABILITY FIELDS TO QUALITY_BATCHES
        // ========================================
        database.execSQL("ALTER TABLE quality_batches ADD COLUMN noChildLabor INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE quality_batches ADD COLUMN farmOsMonitored INTEGER NOT NULL DEFAULT 1");
        database.execSQL("ALTER TABLE quality_batches ADD COLUMN firebreakProtection INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE quality_batches ADD COLUMN sustainableHarvesting INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE quality_batches ADD COLUMN verifiedAt INTEGER NOT NULL DEFAULT 0");
    }
}