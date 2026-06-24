package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;

import java.util.List;

@Dao
public interface QualityBatchDao {

    @Insert
    long insert(QualityBatchEntity batch);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(QualityBatchEntity batch);

    @Update
    void update(QualityBatchEntity batch);

    @Delete
    void delete(QualityBatchEntity batch);

    @Query("DELETE FROM quality_batches WHERE id = :id")
    void deleteById(long id);

    // Single batch queries
    @Query("SELECT * FROM quality_batches WHERE id = :id")
    QualityBatchEntity getBatchById(long id);

    @Query("SELECT * FROM quality_batches WHERE id = :id")
    LiveData<QualityBatchEntity> getBatchByIdLiveData(long id);

    @Query("SELECT * FROM quality_batches WHERE batchNumber = :batchNumber")
    QualityBatchEntity getBatchByNumber(String batchNumber);

    // Farm-wide queries
    @Query("SELECT * FROM quality_batches WHERE farmId = :farmId ORDER BY processingDate DESC")
    List<QualityBatchEntity> getBatchesByFarmId(long farmId);

    @Query("SELECT * FROM quality_batches WHERE farmId = :farmId ORDER BY processingDate DESC")
    LiveData<List<QualityBatchEntity>> getBatchesByFarmIdLiveData(long farmId);

    // Block-specific queries
    @Query("SELECT * FROM quality_batches WHERE blockId = :blockId ORDER BY processingDate DESC")
    List<QualityBatchEntity> getBatchesByBlockId(long blockId);

    @Query("SELECT * FROM quality_batches WHERE blockId = :blockId ORDER BY processingDate DESC")
    LiveData<List<QualityBatchEntity>> getBatchesByBlockIdLiveData(long blockId);

    // Harvest cycle queries
    @Query("SELECT * FROM quality_batches WHERE harvestCycleId = :harvestCycleId")
    List<QualityBatchEntity> getBatchesByHarvestCycle(long harvestCycleId);

    // Quality grade queries
    @Query("SELECT * FROM quality_batches WHERE qualityGrade = :grade ORDER BY processingDate DESC")
    List<QualityBatchEntity> getBatchesByGrade(QualityBatchEntity.QualityGrade grade);

    @Query("SELECT COUNT(*) FROM quality_batches WHERE qualityGrade = :grade")
    int getBatchCountByGrade(QualityBatchEntity.QualityGrade grade);

    // Analytics queries
    @Query("SELECT AVG(moistureContent) FROM quality_batches WHERE farmId = :farmId")
    double getAverageMoisture(long farmId);

    @Query("SELECT AVG(defectiveRate) FROM quality_batches WHERE farmId = :farmId")
    double getAverageDefectiveRate(long farmId);

    @Query("SELECT AVG(kernelRatio) FROM quality_batches WHERE farmId = :farmId")
    double getAverageKernelRatio(long farmId);

    @Query("SELECT AVG(qualityScore) FROM quality_batches WHERE farmId = :farmId")
    double getAverageQualityScore(long farmId);

    @SuppressWarnings("unused")
    @Query("SELECT * FROM quality_batches WHERE farmId = :farmId AND qualityScore < 60 ORDER BY processingDate DESC")
    List<QualityBatchEntity> getLowQualityBatches(long farmId);

    @Query("SELECT * FROM quality_batches WHERE farmId = :farmId ORDER BY qualityScore DESC LIMIT 10")
    List<QualityBatchEntity> getTopQualityBatches(long farmId);

    // Date range queries
    @Query("SELECT * FROM quality_batches WHERE farmId = :farmId " +
            "AND processingDate BETWEEN :startDate AND :endDate ORDER BY processingDate DESC")
    List<QualityBatchEntity> getBatchesInDateRange(long farmId, long startDate, long endDate);

    // Sync-related queries
    @Query("SELECT * FROM quality_batches WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<QualityBatchEntity> getUnsyncedBatches();

    @Query("UPDATE quality_batches SET isSynced = 1 WHERE id = :id")
    void markAsSynced(long id);

    // Statistics
    @Query("SELECT COUNT(*) FROM quality_batches WHERE farmId = :farmId")
    int getTotalBatchCount(long farmId);

    @Query("SELECT SUM(weightKg) FROM quality_batches WHERE farmId = :farmId")
    double getTotalProcessedWeight(long farmId);
}