package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;

import java.util.List;

@Dao
public interface SupplierCommitmentDao {

    // ---- writes (no delete: cancel via status = CANCELLED) ----

    @Insert
    long insert(SupplierCommitmentEntity commitment);

    @Update
    void update(SupplierCommitmentEntity commitment);

    @Query("UPDATE supplier_commitments SET status = 'CANCELLED', " +
            "updatedAt = :now WHERE id = :id")
    void cancel(long id, long now);

    // ---- reads ----

    @Query("SELECT * FROM supplier_commitments WHERE supplierId = :supplierId " +
            "ORDER BY committedDate DESC")
    List<SupplierCommitmentEntity> getBySupplierId(long supplierId);

    @Query("SELECT * FROM supplier_commitments WHERE supplierId = :supplierId " +
            "ORDER BY committedDate DESC")
    LiveData<List<SupplierCommitmentEntity>> getBySupplierIdLive(long supplierId);

    @Query("SELECT * FROM supplier_commitments WHERE supplierId = :supplierId " +
            "AND (validatedAt IS NULL OR validatedAt = 0) " +
            "ORDER BY committedDate DESC")
    List<SupplierCommitmentEntity> getUnvalidatedBySupplierId(long supplierId);

    @Query("SELECT * FROM supplier_commitments WHERE supplierId = :supplierId " +
            "AND season = :season ORDER BY committedDate DESC")
    List<SupplierCommitmentEntity> getBySeason(long supplierId, String season);

    /** The query the portal/engine side mirrors. */
    @Query("SELECT * FROM supplier_commitments WHERE supplierId = :supplierId " +
            "AND season = :season AND isSynced = 1 AND validatedAt IS NOT NULL " +
            "AND validatedAt > 0 ORDER BY committedDate DESC")
    List<SupplierCommitmentEntity> getValidatedBySupplierIdAndSeason(long supplierId, String season);

    /** Evidence confidence: how many validated commitments support an assessment. */
    @Query("SELECT COUNT(*) FROM supplier_commitments WHERE supplierId = :supplierId " +
            "AND isSynced = 1 AND validatedAt IS NOT NULL AND validatedAt > 0")
    int countValidatedBySupplierId(long supplierId);

    /** For the sync manager. */
    @Query("SELECT * FROM supplier_commitments WHERE isSynced = 0")
    List<SupplierCommitmentEntity> getPendingSync();

    @Query("SELECT * FROM supplier_commitments WHERE id = :id")
    SupplierCommitmentEntity getById(long id);

    @Query("SELECT * FROM supplier_commitments WHERE id = :id")
    LiveData<SupplierCommitmentEntity> getByIdLive(long id);
}