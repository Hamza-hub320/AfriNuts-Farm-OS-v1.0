package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;

import java.util.List;

@Dao
public interface MasterTraceabilityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MasterBatchTraceabilityEntity master);

    @Update
    void update(MasterBatchTraceabilityEntity master);

    @Delete
    void delete(MasterBatchTraceabilityEntity master);

    @Query("SELECT * FROM master_batch_traceability WHERE id = :id")
    MasterBatchTraceabilityEntity getById(long id);

    @Query("SELECT * FROM master_batch_traceability WHERE id = :id")
    LiveData<MasterBatchTraceabilityEntity> getByIdLiveData(long id);

    @Query("SELECT * FROM master_batch_traceability WHERE masterBatchNumber = :masterBatchNumber")
    MasterBatchTraceabilityEntity getByMasterBatchNumber(String masterBatchNumber);

    @Query("SELECT * FROM master_batch_traceability ORDER BY createdAt DESC")
    List<MasterBatchTraceabilityEntity> getAll();

    @Query("SELECT * FROM master_batch_traceability ORDER BY createdAt DESC")
    LiveData<List<MasterBatchTraceabilityEntity>> getAllLiveData();

    @Query("SELECT * FROM master_batch_traceability WHERE buyer = :buyer ORDER BY createdAt DESC")
    List<MasterBatchTraceabilityEntity> getByBuyer(String buyer);

    @Query("SELECT COUNT(*) FROM master_batch_traceability")
    int getTotalCount();

    @Query("SELECT SUM(totalWeightKg) FROM master_batch_traceability")
    double getTotalWeight();

    @Query("SELECT * FROM master_batch_traceability WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<MasterBatchTraceabilityEntity> getUnsynced();

    @Query("UPDATE master_batch_traceability SET isSynced = 1 WHERE id = :id")
    void markAsSynced(long id);
}