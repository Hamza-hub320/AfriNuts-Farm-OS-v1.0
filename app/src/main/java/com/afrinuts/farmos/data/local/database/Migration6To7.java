package com.afrinuts.farmos.data.local.database;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration6To7 extends Migration {

    public Migration6To7() {
        super(6, 7);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Create weather_cache table
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `weather_cache` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`location` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, " +
                        "`weatherData` TEXT NOT NULL, " +
                        "`forecastDays` INTEGER NOT NULL" +
                        ")"
        );

        // Create index for faster lookups
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_weather_cache_location` ON `weather_cache` (`location`)"
        );
    }
}