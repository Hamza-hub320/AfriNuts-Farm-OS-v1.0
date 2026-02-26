package com.afrinuts.farmos.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.afrinuts.farmos.data.local.converter.Converters;
import com.afrinuts.farmos.data.local.dao.FarmDao;
import com.afrinuts.farmos.data.local.entity.FarmEntity;

/**
 * Main database configuration for AfriNuts Farm OS.
 * Version 1: Core entities only.
 */
@Database(
        entities = {
                FarmEntity.class
                // We'll add BlockEntity, ExpenseEntity, etc. here as we create them
        },
        version = 1,
        exportSchema = true
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    // DAO declarations
    public abstract FarmDao farmDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "afrinuts_farm_os.db"
                            )
                            // Allow queries on main thread for development only
                            // REMOVE THIS BEFORE PRODUCTION RELEASE
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}