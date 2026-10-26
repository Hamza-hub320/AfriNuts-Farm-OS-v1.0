package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.InspectionEntity;

import java.util.List;

@Dao
public interface InspectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(InspectionEntity inspection);

    @Update
    void update(InspectionEntity inspection);

    @Delete
    void delete(InspectionEntity inspection);

    @Query("DELETE FROM inspections WHERE inspectionId = :inspectionId")
    void deleteById(String inspectionId);

    // Get all inspections for a farm
    @Query("SELECT * FROM inspections WHERE farmId = :farmId ORDER BY inspectionDate DESC")
    List<InspectionEntity> getInspectionsByFarmId(long farmId);

    @Query("SELECT * FROM inspections WHERE farmId = :farmId ORDER BY inspectionDate DESC")
    LiveData<List<InspectionEntity>> getInspectionsByFarmIdLiveData(long farmId);

    // Get inspections for a specific certification
    @Query("SELECT * FROM inspections WHERE certificationId = :certificationId ORDER BY inspectionDate DESC")
    List<InspectionEntity> getInspectionsByCertificationId(String certificationId);

    @Query("SELECT * FROM inspections WHERE certificationId = :certificationId ORDER BY inspectionDate DESC")
    LiveData<List<InspectionEntity>> getInspectionsByCertificationIdLiveData(String certificationId);

    // Get single inspection
    @Query("SELECT * FROM inspections WHERE inspectionId = :inspectionId")
    InspectionEntity getInspectionById(String inspectionId);

    @Query("SELECT * FROM inspections WHERE inspectionId = :inspectionId")
    LiveData<InspectionEntity> getInspectionByIdLiveData(String inspectionId);

    // Filter by status
    @Query("SELECT * FROM inspections WHERE farmId = :farmId AND status = :status ORDER BY inspectionDate DESC")
    List<InspectionEntity> getInspectionsByStatus(long farmId, String status);

    // Filter by category
    @Query("SELECT * FROM inspections WHERE farmId = :farmId AND requirementCategory = :category ORDER BY inspectionDate DESC")
    List<InspectionEntity> getInspectionsByCategory(long farmId, String category);

    // Sync related
    @Query("SELECT * FROM inspections WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<InspectionEntity> getUnsyncedInspections();

    @Query("UPDATE inspections SET isSynced = 1 WHERE inspectionId = :inspectionId")
    void markAsSynced(String inspectionId);
}