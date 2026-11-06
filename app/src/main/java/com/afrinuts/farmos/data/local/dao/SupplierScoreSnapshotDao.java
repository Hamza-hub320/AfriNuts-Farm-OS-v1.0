package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.afrinuts.farmos.data.local.entity.SupplierScoreSnapshotEntity;

import java.util.List;

/**
 * Insert + reads only. There is deliberately no update() and no delete() here:
 * a snapshot, once written, is immutable. Corrections create a new snapshot.
 */
@Dao
public interface SupplierScoreSnapshotDao {

    @Insert
    long insert(SupplierScoreSnapshotEntity snapshot);

    @Query("SELECT * FROM supplier_score_history WHERE supplierId = :supplierId " +
            "ORDER BY calculatedAt DESC LIMIT 1")
    SupplierScoreSnapshotEntity getLatestBySupplierId(long supplierId);

    @Query("SELECT * FROM supplier_score_history WHERE supplierId = :supplierId " +
            "ORDER BY calculatedAt DESC LIMIT 1")
    LiveData<SupplierScoreSnapshotEntity> getLatestBySupplierIdLive(long supplierId);

    @Query("SELECT * FROM supplier_score_history WHERE supplierId = :supplierId " +
            "ORDER BY calculatedAt DESC")
    List<SupplierScoreSnapshotEntity> getHistoryBySupplierId(long supplierId);

    @Query("SELECT * FROM supplier_score_history WHERE supplierId = :supplierId " +
            "ORDER BY calculatedAt DESC")
    LiveData<List<SupplierScoreSnapshotEntity>> getHistoryBySupplierIdLive(long supplierId);

    @Query("SELECT * FROM supplier_score_history WHERE id = :id")
    SupplierScoreSnapshotEntity getById(long id);

    /** For the sync manager — snapshots are usually written by the portal, so this is rarely used. */
    @Query("SELECT * FROM supplier_score_history WHERE id = :id")
    LiveData<SupplierScoreSnapshotEntity> getByIdLive(long id);
}