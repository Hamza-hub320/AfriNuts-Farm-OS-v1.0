package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.VerificationEntity;

import java.util.List;

@Dao
public interface VerificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(VerificationEntity verification);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertAbort(VerificationEntity verification);

    @Update
    void update(VerificationEntity verification);

    @Delete
    void delete(VerificationEntity verification);

    @Query("DELETE FROM compliance_verifications WHERE verificationId = :verificationId")
    void deleteById(String verificationId);

    // Get all verifications for a farm
    @Query("SELECT * FROM compliance_verifications WHERE farmId = :farmId ORDER BY verificationDate DESC")
    List<VerificationEntity> getVerificationsByFarmId(long farmId);

    @Query("SELECT * FROM compliance_verifications WHERE farmId = :farmId ORDER BY verificationDate DESC")
    LiveData<List<VerificationEntity>> getVerificationsByFarmIdLiveData(long farmId);

    // Get verifications for a specific certification
    @Query("SELECT * FROM compliance_verifications WHERE certificationId = :certificationId ORDER BY verificationDate DESC")
    List<VerificationEntity> getVerificationsByCertificationId(String certificationId);

    @Query("SELECT * FROM compliance_verifications WHERE certificationId = :certificationId ORDER BY verificationDate DESC")
    LiveData<List<VerificationEntity>> getVerificationsByCertificationIdLiveData(String certificationId);

    // Get single verification
    @Query("SELECT * FROM compliance_verifications WHERE verificationId = :verificationId")
    VerificationEntity getVerificationById(String verificationId);

    @Query("SELECT * FROM compliance_verifications WHERE verificationId = :verificationId")
    LiveData<VerificationEntity> getVerificationByIdLiveData(String verificationId);

    // Filter by status
    @Query("SELECT * FROM compliance_verifications WHERE farmId = :farmId AND status = :status ORDER BY verificationDate DESC")
    List<VerificationEntity> getVerificationsByStatus(long farmId, String status);

    // Filter by type
    @Query("SELECT * FROM compliance_verifications WHERE farmId = :farmId AND verificationType = :type ORDER BY verificationDate DESC")
    List<VerificationEntity> getVerificationsByType(long farmId, String type);

    // Get pending verifications
    @Query("SELECT * FROM compliance_verifications WHERE farmId = :farmId AND status = 'PENDING' ORDER BY verificationDate DESC")
    List<VerificationEntity> getPendingVerifications(long farmId);

    // Sync related
    @Query("SELECT * FROM compliance_verifications WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<VerificationEntity> getUnsyncedVerifications();

    @Query("UPDATE compliance_verifications SET isSynced = 1 WHERE verificationId = :verificationId")
    void markAsSynced(String verificationId);

    @Query("SELECT verificationId FROM compliance_verifications " +
            "WHERE verificationId LIKE :prefix || '%' ORDER BY verificationId DESC LIMIT 1")
    String getMaxVerificationIdForPrefix(String prefix);
}