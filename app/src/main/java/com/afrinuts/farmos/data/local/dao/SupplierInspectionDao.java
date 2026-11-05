package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;

import java.util.List;

@Dao
public interface SupplierInspectionDao {

    // ---- writes (no delete; DISPUTED / CLOSED supersede) ----

    @Insert
    long insert(SupplierInspectionEntity inspection);

    @Update
    void update(SupplierInspectionEntity inspection);

    @Query("UPDATE supplier_inspections SET status = 'CLOSED', " +
            "closedDate = :closedDate, updatedAt = :now WHERE id = :id")
    void close(long id, long closedDate, long now);

    @Query("UPDATE supplier_inspections SET status = 'DISPUTED', " +
            "updatedAt = :now WHERE id = :id")
    void markDisputed(long id, long now);

    // ---- reads ----

    @Query("SELECT * FROM supplier_inspections WHERE supplierId = :supplierId " +
            "ORDER BY inspectionDate DESC")
    List<SupplierInspectionEntity> getBySupplierId(long supplierId);

    @Query("SELECT * FROM supplier_inspections WHERE supplierId = :supplierId " +
            "ORDER BY inspectionDate DESC")
    LiveData<List<SupplierInspectionEntity>> getBySupplierIdLive(long supplierId);

    @Query("SELECT * FROM supplier_inspections WHERE supplierId = :supplierId " +
            "AND (validatedAt IS NULL OR validatedAt = 0) " +
            "ORDER BY inspectionDate DESC")
    List<SupplierInspectionEntity> getUnvalidatedBySupplierId(long supplierId);

    /** The query the portal/engine side mirrors. */
    @Query("SELECT * FROM supplier_inspections WHERE supplierId = :supplierId " +
            "AND isSynced = 1 AND validatedAt IS NOT NULL AND validatedAt > 0 " +
            "ORDER BY inspectionDate DESC")
    List<SupplierInspectionEntity> getValidatedBySupplierId(long supplierId);

    /** Evidence confidence: how many validated inspections support an assessment. */
    @Query("SELECT COUNT(*) FROM supplier_inspections WHERE supplierId = :supplierId " +
            "AND isSynced = 1 AND validatedAt IS NOT NULL AND validatedAt > 0")
    int countValidatedBySupplierId(long supplierId);

    /**
     * Engine eligibility lookup: validated FAIL with CRITICAL severity.
     * Used to raise a warning/eligibility flag, never averaged into the score.
     */
    @Query("SELECT * FROM supplier_inspections WHERE supplierId = :supplierId " +
            "AND isSynced = 1 AND validatedAt IS NOT NULL AND validatedAt > 0 " +
            "AND outcome = 'FAIL' AND severity = 'CRITICAL' " +
            "ORDER BY inspectionDate DESC")
    List<SupplierInspectionEntity> getValidatedCriticalFails(long supplierId);

    /** For the sync manager. */
    @Query("SELECT * FROM supplier_inspections WHERE isSynced = 0")
    List<SupplierInspectionEntity> getPendingSync();

    @Query("SELECT * FROM supplier_inspections WHERE id = :id")
    SupplierInspectionEntity getById(long id);

    @Query("SELECT * FROM supplier_inspections WHERE id = :id")
    LiveData<SupplierInspectionEntity> getByIdLive(long id);
}