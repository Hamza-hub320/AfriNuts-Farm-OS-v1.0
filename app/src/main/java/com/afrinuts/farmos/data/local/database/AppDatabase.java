package com.afrinuts.farmos.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.afrinuts.farmos.data.local.converter.Converters;
import com.afrinuts.farmos.data.local.dao.BlockDao;
import com.afrinuts.farmos.data.local.dao.ExpenseDao;
import com.afrinuts.farmos.data.local.dao.FarmDao;
import com.afrinuts.farmos.data.local.dao.RevenueDao;
import com.afrinuts.farmos.data.local.dao.TaskDao;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;

/**
 * Main database configuration for AfriNuts Farm OS.
 * Version 1: Core entities only.
 */
@Database(
        entities = {
                FarmEntity.class,
                BlockEntity.class,
                ExpenseEntity.class,
                RevenueEntity.class,
                TaskEntity.class,
                TaskAssignmentHistoryEntity.class

                // We'll add BlockEntity, ExpenseEntity, etc. here as we create them
        },
        version = 6,
        exportSchema = true
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    // DAO declarations
    public abstract FarmDao farmDao();

    public abstract BlockDao blockDao();
    public abstract ExpenseDao expenseDao();
    public abstract RevenueDao revenueDao();
    public abstract TaskDao taskDao();

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
                            .allowMainThreadQueries()
                            // 🔥 This will wipe and recreate the database
                            .fallbackToDestructiveMigration()
                            // .addMigrations(new Migration1To2(), new Migration2To3(),
                            //               new Migration3To4(), new Migration4To5(),
                            //               new Migration5To6())
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}