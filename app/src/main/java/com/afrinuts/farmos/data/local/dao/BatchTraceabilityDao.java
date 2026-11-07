package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;

import java.util.List;

@Dao
public interface BatchTraceabilityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(BatchTraceabilityEntity traceability);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BatchTraceabilityEntity> traceabilities);

    @Update
    void update(BatchTraceabilityEntity traceability);

    @Delete
    void delete(BatchTraceabilityEntity traceability);

    @Query("DELETE FROM batch_traceability WHERE id = :id")
    void deleteById(long id);

    // Get by ID
    @Query("SELECT * FROM batch_traceability WHERE id = :id")
    BatchTraceabilityEntity getById(long id);

    @Query("SELECT * FROM batch_traceability WHERE id = :id")
    LiveData<BatchTraceabilityEntity> getByIdLiveData(long id);

    // Get by batch number
    @Query("SELECT * FROM batch_traceability WHERE batchNumber = :batchNumber")
    BatchTraceabilityEntity getByBatchNumber(String batchNumber);

    @Query("SELECT * FROM batch_traceability WHERE batchNumber = :batchNumber")
    LiveData<BatchTraceabilityEntity> getByBatchNumberLiveData(String batchNumber);

    // Get by quality batch ID
    @Query("SELECT * FROM batch_traceability WHERE qualityBatchId = :qualityBatchId")
    BatchTraceabilityEntity getByQualityBatchId(long qualityBatchId);

    @Query("SELECT * FROM batch_traceability WHERE qualityBatchId = :qualityBatchId")
    LiveData<BatchTraceabilityEntity> getByQualityBatchIdLiveData(long qualityBatchId);

    // Get by supplier
    @Query("SELECT * FROM batch_traceability WHERE supplierId = :supplierId ORDER BY processingDate DESC")
    List<BatchTraceabilityEntity> getBySupplierId(long supplierId);

    @Query("SELECT * FROM batch_traceability WHERE supplierId = :supplierId ORDER BY processingDate DESC")
    LiveData<List<BatchTraceabilityEntity>> getBySupplierIdLiveData(long supplierId);

    // Get all
    @Query("SELECT * FROM batch_traceability ORDER BY processingDate DESC")
    List<BatchTraceabilityEntity> getAll();

    @Query("SELECT * FROM batch_traceability ORDER BY processingDate DESC")
    LiveData<List<BatchTraceabilityEntity>> getAllLiveData();

    // Get by farm
    @Query("SELECT * FROM batch_traceability WHERE farmId = :farmId ORDER BY processingDate DESC")
    List<BatchTraceabilityEntity> getByFarmId(long farmId);

    @Query("SELECT * FROM batch_traceability WHERE farmId = :farmId ORDER BY processingDate DESC")
    LiveData<List<BatchTraceabilityEntity>> getByFarmIdLiveData(long farmId);

    // Get exported batches
    @Query("SELECT * FROM batch_traceability WHERE exportDate IS NOT NULL ORDER BY exportDate DESC")
    List<BatchTraceabilityEntity> getExportedBatches();

    // Get batches by date range
    @Query("SELECT * FROM batch_traceability WHERE processingDate BETWEEN :startDate AND :endDate ORDER BY processingDate DESC")
    List<BatchTraceabilityEntity> getBatchesInDateRange(long startDate, long endDate);

    // Get recent batches
    @Query("SELECT * FROM batch_traceability ORDER BY processingDate DESC LIMIT :limit")
    List<BatchTraceabilityEntity> getRecentBatches(int limit);

    // Search by batch number or buyer
    @Query("SELECT * FROM batch_traceability WHERE batchNumber LIKE '%' || :query || '%' OR buyer LIKE '%' || :query || '%' ORDER BY processingDate DESC")
    List<BatchTraceabilityEntity> searchBatches(String query);

    // Statistics
    @Query("SELECT COUNT(*) FROM batch_traceability WHERE farmId = :farmId")
    int getTotalCount(long farmId);

    @Query("SELECT COUNT(*) FROM batch_traceability WHERE farmId = :farmId AND exportDate IS NOT NULL")
    int getExportedCount(long farmId);

    @Query("SELECT SUM(weightKg) FROM batch_traceability WHERE farmId = :farmId")
    double getTotalWeight(long farmId);

    // Sync
    @Query("SELECT * FROM batch_traceability WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<BatchTraceabilityEntity> getUnsyncedBatches();

    @Query("UPDATE batch_traceability SET isSynced = 1 WHERE id = :id")
    void markAsSynced(long id);

    @Query("SELECT * FROM batch_traceability WHERE masterBatchId = :masterBatchId " +
            "ORDER BY createdAt ASC")
    List<BatchTraceabilityEntity> getChildrenOfMaster(String masterBatchId);
}