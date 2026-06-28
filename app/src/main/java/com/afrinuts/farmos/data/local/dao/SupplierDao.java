package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.SupplierEntity;

import java.util.List;

@Dao
public interface SupplierDao {

    @Insert
    long insert(SupplierEntity supplier);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SupplierEntity supplier);

    @Update
    void update(SupplierEntity supplier);

    @Delete
    void delete(SupplierEntity supplier);

    @Query("DELETE FROM suppliers WHERE id = :id")
    void deleteById(long id);

    // Single supplier queries
    @Query("SELECT * FROM suppliers WHERE id = :id")
    SupplierEntity getSupplierById(long id);

    @Query("SELECT * FROM suppliers WHERE id = :id")
    LiveData<SupplierEntity> getSupplierByIdLiveData(long id);

    @Query("SELECT * FROM suppliers WHERE name = :name")
    SupplierEntity getSupplierByName(String name);

    // Farm-wide queries
    @Query("SELECT * FROM suppliers WHERE farmId = :farmId ORDER BY name ASC")
    List<SupplierEntity> getSuppliersByFarmId(long farmId);

    @Query("SELECT * FROM suppliers WHERE farmId = :farmId ORDER BY name ASC")
    LiveData<List<SupplierEntity>> getSuppliersByFarmIdLiveData(long farmId);

    // Filter queries
    @Query("SELECT * FROM suppliers WHERE farmId = :farmId AND type = :type ORDER BY name ASC")
    List<SupplierEntity> getSuppliersByType(long farmId, SupplierEntity.SupplierType type);

    @Query("SELECT * FROM suppliers WHERE farmId = :farmId AND status = :status ORDER BY name ASC")
    List<SupplierEntity> getSuppliersByStatus(long farmId, SupplierEntity.SupplierStatus status);

    @Query("SELECT * FROM suppliers WHERE farmId = :farmId AND isActive = 1 ORDER BY rating DESC")
    List<SupplierEntity> getActiveSuppliers(long farmId);

    @Query("SELECT * FROM suppliers WHERE farmId = :farmId AND isCertified = 1 ORDER BY name ASC")
    List<SupplierEntity> getCertifiedSuppliers(long farmId);

    // Rating queries
    @Query("SELECT * FROM suppliers WHERE farmId = :farmId AND rating >= :minRating ORDER BY rating DESC")
    List<SupplierEntity> getSuppliersByRating(long farmId, int minRating);

    // Statistics
    @Query("SELECT COUNT(*) FROM suppliers WHERE farmId = :farmId")
    int getTotalSupplierCount(long farmId);

    @Query("SELECT COUNT(*) FROM suppliers WHERE farmId = :farmId AND isActive = 1")
    int getActiveSupplierCount(long farmId);

    @Query("SELECT COUNT(*) FROM suppliers WHERE farmId = :farmId AND isCertified = 1")
    int getCertifiedSupplierCount(long farmId);

    @Query("SELECT AVG(rating) FROM suppliers WHERE farmId = :farmId")
    double getAverageRating(long farmId);

    @Query("SELECT SUM(annualVolumeKg) FROM suppliers WHERE farmId = :farmId")
    double getTotalAnnualVolume(long farmId);

    // Sync-related queries
    @Query("SELECT * FROM suppliers WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<SupplierEntity> getUnsyncedSuppliers();

    @Query("UPDATE suppliers SET isSynced = 1 WHERE id = :id")
    void markAsSynced(long id);

    @Query("SELECT * FROM suppliers WHERE registrationNumber = :registrationNumber")
    SupplierEntity getSupplierByRegistrationNumber(String registrationNumber);

    @Query("SELECT COUNT(*) FROM suppliers WHERE registrationNumber = :registrationNumber")
    int countByRegistrationNumber(String registrationNumber);
}