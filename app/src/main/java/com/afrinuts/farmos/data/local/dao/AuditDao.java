package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.AuditEntity;

import java.util.List;

@Dao
public interface AuditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(AuditEntity audit);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertAbort(AuditEntity audit);

    @Update
    void update(AuditEntity audit);

    @Delete
    void delete(AuditEntity audit);

    @Query("DELETE FROM audits WHERE auditId = :auditId")
    void deleteById(String auditId);

    // Get all audits for a farm
    @Query("SELECT * FROM audits WHERE farmId = :farmId ORDER BY auditDate DESC")
    List<AuditEntity> getAuditsByFarmId(long farmId);

    @Query("SELECT * FROM audits WHERE farmId = :farmId ORDER BY auditDate DESC")
    LiveData<List<AuditEntity>> getAuditsByFarmIdLiveData(long farmId);

    // Get audits for a specific certification
    @Query("SELECT * FROM audits WHERE certificationId = :certificationId ORDER BY auditDate DESC")
    List<AuditEntity> getAuditsByCertificationId(String certificationId);

    @Query("SELECT * FROM audits WHERE certificationId = :certificationId ORDER BY auditDate DESC")
    LiveData<List<AuditEntity>> getAuditsByCertificationIdLiveData(String certificationId);

    // Get single audit
    @Query("SELECT * FROM audits WHERE auditId = :auditId")
    AuditEntity getAuditById(String auditId);

    @Query("SELECT * FROM audits WHERE auditId = :auditId")
    LiveData<AuditEntity> getAuditByIdLiveData(String auditId);

    // Filter by status
    @Query("SELECT * FROM audits WHERE farmId = :farmId AND status = :status ORDER BY auditDate DESC")
    List<AuditEntity> getAuditsByStatus(long farmId, String status);

    // Filter by type
    @Query("SELECT * FROM audits WHERE farmId = :farmId AND auditType = :auditType ORDER BY auditDate DESC")
    List<AuditEntity> getAuditsByType(long farmId, String auditType);

    // Get latest audit for a certification
    @Query("SELECT * FROM audits WHERE certificationId = :certificationId ORDER BY auditDate DESC LIMIT 1")
    AuditEntity getLatestAudit(String certificationId);

    // Get audits within date range
    @Query("SELECT * FROM audits WHERE farmId = :farmId AND auditDate BETWEEN :startDate AND :endDate ORDER BY auditDate DESC")
    List<AuditEntity> getAuditsInDateRange(long farmId, long startDate, long endDate);

    // Count by status
    @Query("SELECT COUNT(*) FROM audits WHERE farmId = :farmId AND status = :status")
    int countByStatus(long farmId, String status);

    // Sync related
    @Query("SELECT * FROM audits WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<AuditEntity> getUnsyncedAudits();

    @Query("UPDATE audits SET isSynced = 1 WHERE auditId = :auditId")
    void markAsSynced(String auditId);

    @Query("SELECT auditId FROM audits WHERE auditId LIKE :prefix || '%' ORDER BY auditId DESC LIMIT 1")
    String getMaxAuditIdForPrefix(String prefix);
}