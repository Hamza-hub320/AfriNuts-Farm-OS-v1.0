package com.afrinuts.farmos.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Adds the optional invoice number stored against a revenue sale. */
public class Migration46To47 extends Migration {

    public Migration46To47() {
        super(46, 47);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE revenues ADD COLUMN invoiceNumber TEXT");
    }
}
