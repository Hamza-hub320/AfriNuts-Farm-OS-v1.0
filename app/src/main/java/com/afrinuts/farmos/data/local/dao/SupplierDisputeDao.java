package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;

import java.util.List;

@Dao
public interface SupplierDisputeDao {

    // ---- writes: insert + update only. No delete: a dispute is an audit record.
    // The repository's recordResolution() is the only caller of update(). ----

    @Insert
    long insert(SupplierDisputeEntity dispute);

    @Update
    void update(SupplierDisputeEntity dispute);

    // ---- reads ----

    @Query("SELECT * FROM supplier_disputes WHERE supplierId = :supplierId " +
            "ORDER BY submittedDate DESC")
    List<SupplierDisputeEntity> getBySupplierId(long supplierId);

    @Query("SELECT * FROM supplier_disputes WHERE supplierId = :supplierId " +
            "ORDER BY submittedDate DESC")
    LiveData<List<SupplierDisputeEntity>> getBySupplierIdLive(long supplierId);

    @Query("SELECT * FROM supplier_disputes WHERE targetEntity = :targetEntity " +
            "AND targetId = :targetId ORDER BY submittedDate DESC")
    List<SupplierDisputeEntity> getByTarget(String targetEntity, long targetId);

    @Query("SELECT * FROM supplier_disputes WHERE supplierId = :supplierId " +
            "AND reviewStatus IN ('OPEN','UNDER_REVIEW') ORDER BY submittedDate DESC")
    List<SupplierDisputeEntity> getOpenBySupplierId(long supplierId);

    @Query("SELECT * FROM supplier_disputes WHERE id = :id")
    SupplierDisputeEntity getById(long id);

    @Query("SELECT * FROM supplier_disputes WHERE id = :id")
    LiveData<SupplierDisputeEntity> getByIdLive(long id);

    /** For the sync manager. */
    @Query("SELECT * FROM supplier_disputes WHERE isSynced = 0")
    List<SupplierDisputeEntity> getPendingSync();
}