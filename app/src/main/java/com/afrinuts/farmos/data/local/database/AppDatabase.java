package com.afrinuts.farmos.data.local.database;

import static androidx.compose.ui.text.input.CursorAnchorInfoBuilder_androidKt.build;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.afrinuts.farmos.BuildConfig;
import com.afrinuts.farmos.data.local.converter.Converters;
import com.afrinuts.farmos.data.local.dao.AuditDao;
import com.afrinuts.farmos.data.local.dao.BatchTraceabilityDao;
import com.afrinuts.farmos.data.local.dao.BlockDao;
import com.afrinuts.farmos.data.local.dao.CertificationDao;
import com.afrinuts.farmos.data.local.dao.DocumentDao;
import com.afrinuts.farmos.data.local.dao.ExpenseDao;
import com.afrinuts.farmos.data.local.dao.FarmDao;
import com.afrinuts.farmos.data.local.dao.InspectionDao;
import com.afrinuts.farmos.data.local.dao.MasterTraceabilityDao;
import com.afrinuts.farmos.data.local.dao.PhotoDao;
import com.afrinuts.farmos.data.local.dao.QualityBatchDao;
import com.afrinuts.farmos.data.local.dao.RevenueDao;
import com.afrinuts.farmos.data.local.dao.SupplierDao;
import com.afrinuts.farmos.data.local.dao.SyncDao;
import com.afrinuts.farmos.data.local.dao.TaskAssignmentDao;
import com.afrinuts.farmos.data.local.dao.TaskDao;
import com.afrinuts.farmos.data.local.dao.UserDao;
import com.afrinuts.farmos.data.local.dao.VerificationDao;
import com.afrinuts.farmos.data.local.dao.WeatherDao;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
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
                TaskAssignmentHistoryEntity.class,
                WeatherCacheEntity.class,
                UserEntity.class,
                PhotoEntity.class,
                SyncEntity.class,
                QualityBatchEntity.class,
                SupplierEntity.class,
                BatchTraceabilityEntity.class,
                MasterBatchTraceabilityEntity.class,
                CertificationEntity.class,
                AuditEntity.class,
                InspectionEntity.class,
                VerificationEntity.class,
                DocumentEntity.class,
                // OrganicInputEntity.class,  // Add when implemented
                // HalalInputEntity.class,  // Add when implemented
                // HACCPPlanEntity.class,  // Add when implemented
                // PRPEntity.class,  // Add when implemented
                // CCPMonitoringEntity.class,  // Add when implemented
                // PremiumDistributionEntity.class,  // Add when implemented
                // CooperativeMemberEntity.class  // Add when implemented
        },
        version = 32,
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

    public abstract QualityBatchDao qualityBatchDao();

    public abstract SupplierDao supplierDao();
    public abstract BatchTraceabilityDao batchTraceabilityDao();
    public abstract MasterTraceabilityDao masterTraceabilityDao();
    public abstract CertificationDao certificationDao();
    public abstract AuditDao auditDao();
    public abstract InspectionDao inspectionDao();
    public abstract VerificationDao verificationDao();
    public abstract DocumentDao documentDao();

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
                            .addMigrations(
                                    new Migration1To2(),
                                    new Migration2To3(),
                                    new Migration3To4(),
                                    new Migration4To5(),
                                    new Migration5To6(),
                                    new Migration6To7(),
                                    new Migration7To8(),
                                    new Migration8To9(),
                                    new Migration9To10(),
                                    new Migration10To11(),
                                    new Migration11To12(),
                                    new Migration12To13(),
                                    new Migration13To14(),
                                    new Migration14To15(),
                                    new Migration15To16(),
                                    new Migration16To17(),
                                    new Migration17To18(),
                                    new Migration18To19(),
                                    new Migration19To20(),
                                    new Migration20To21(),
                                    new Migration21To22(),
                                    new Migration22To23(),
                                    new Migration23To24(),
                                    new Migration24To25(),
                                    new Migration25To26(),
                                    new Migration26To27(),
                                    new Migration27To28(),
                                    new Migration28To29(),
                                    new Migration29To30(),
                                    new Migration30To31(),
                                    new Migration31To32()

                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}