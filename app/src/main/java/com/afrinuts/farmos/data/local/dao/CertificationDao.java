package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.CertificationEntity;

import java.util.List;

@Dao
public interface CertificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CertificationEntity certification);

    @Update
    void update(CertificationEntity certification);

    @Delete
    void delete(CertificationEntity certification);

    @Query("DELETE FROM certifications WHERE certificationId = :certificationId")
    void deleteById(String certificationId);

    // Get all certifications for a farm
    @Query("SELECT * FROM certifications WHERE farmId = :farmId ORDER BY targetDate ASC")
    List<CertificationEntity> getCertificationsByFarmId(long farmId);

    @Query("SELECT * FROM certifications WHERE farmId = :farmId ORDER BY targetDate ASC")
    LiveData<List<CertificationEntity>> getCertificationsByFarmIdLiveData(long farmId);

    // Get single certification
    @Query("SELECT * FROM certifications WHERE certificationId = :certificationId")
    CertificationEntity getCertificationById(String certificationId);

    @Query("SELECT * FROM certifications WHERE certificationId = :certificationId")
    LiveData<CertificationEntity> getCertificationByIdLiveData(String certificationId);

    // Filter by status
    @Query("SELECT * FROM certifications WHERE farmId = :farmId AND status = :status ORDER BY targetDate ASC")
    List<CertificationEntity> getCertificationsByStatus(long farmId, String status);

    // Get active certifications (not expired or suspended)
    @Query("SELECT * FROM certifications WHERE farmId = :farmId AND status IN ('IN_PROGRESS', 'AUDIT_READY', 'CERTIFIED') ORDER BY targetDate ASC")
    List<CertificationEntity> getActiveCertifications(long farmId);

    // Get expiring certifications (within 90 days)
    @Query("SELECT * FROM certifications WHERE farmId = :farmId AND status = 'CERTIFIED' AND expiryDate > :currentTime AND expiryDate <= :expiryThreshold ORDER BY expiryDate ASC")
    List<CertificationEntity> getExpiringCertifications(long farmId, long currentTime, long expiryThreshold);

    // Get expired certifications
    @Query("SELECT * FROM certifications WHERE farmId = :farmId AND status = 'CERTIFIED' AND expiryDate < :currentTime")
    List<CertificationEntity> getExpiredCertifications(long farmId, long currentTime);

    // Count by status
    @Query("SELECT COUNT(*) FROM certifications WHERE farmId = :farmId AND status = :status")
    int countByStatus(long farmId, String status);

    // Sync related
    @Query("SELECT * FROM certifications WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<CertificationEntity> getUnsyncedCertifications();

    @Query("UPDATE certifications SET isSynced = 1 WHERE certificationId = :certificationId")
    void markAsSynced(String certificationId);
}