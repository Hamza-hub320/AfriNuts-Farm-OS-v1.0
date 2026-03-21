package com.afrinuts.farmos.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.afrinuts.farmos.BuildConfig;
import com.afrinuts.farmos.data.local.converter.Converters;
import com.afrinuts.farmos.data.local.dao.BlockDao;
import com.afrinuts.farmos.data.local.dao.ExpenseDao;
import com.afrinuts.farmos.data.local.dao.FarmDao;
import com.afrinuts.farmos.data.local.dao.PhotoDao;
import com.afrinuts.farmos.data.local.dao.RevenueDao;
import com.afrinuts.farmos.data.local.dao.SyncDao;
import com.afrinuts.farmos.data.local.dao.TaskAssignmentDao;
import com.afrinuts.farmos.data.local.dao.TaskDao;
import com.afrinuts.farmos.data.local.dao.UserDao;
import com.afrinuts.farmos.data.local.dao.WeatherDao;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.WeatherCacheEntity;

/**
 * Main database configuration for AfriNuts Farm OS.
 */
@Database(
        entities = {
                FarmEntity.class,
                BlockEntity.class,
                ExpenseEntity.class,
                RevenueEntity.class,
                TaskEntity.class,
                TaskAssignmentHistoryEntity.class,  // Added this entity
                WeatherCacheEntity.class,
                UserEntity.class,
                PhotoEntity.class,
                SyncEntity.class
        },
        version = 13,
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

    public abstract WeatherDao weatherDao();

    public abstract UserDao userDao();

    public abstract PhotoDao photoDao();

    public abstract SyncDao syncDao();

    public abstract TaskAssignmentDao taskAssignmentDao();

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
                            .fallbackToDestructiveMigration() // This will recreate the DB
                            .allowMainThreadQueries() // For development only
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}