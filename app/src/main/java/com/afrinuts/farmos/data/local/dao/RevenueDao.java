package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.RevenueEntity;

import java.util.List;

@Dao
public interface RevenueDao {

    @Insert
    long insert(RevenueEntity revenue);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(RevenueEntity revenue);

    @Update
    void update(RevenueEntity revenue);

    @Delete
    void delete(RevenueEntity revenue);

    @Query("DELETE FROM revenues WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM revenues WHERE id = :id")
    RevenueEntity getRevenueById(long id);

    @Query("SELECT * FROM revenues WHERE id = :id")
    LiveData<RevenueEntity> getRevenueByIdLiveData(long id);

    @Query("SELECT * FROM revenues WHERE farmId = :farmId ORDER BY createdAt DESC")
    List<RevenueEntity> getRevenuesByFarmId(long farmId);

    @Query("SELECT * FROM revenues WHERE farmId = :farmId ORDER BY createdAt DESC")
    LiveData<List<RevenueEntity>> getRevenuesByFarmIdLiveData(long farmId);

    @Query("SELECT * FROM revenues ORDER BY createdAt DESC")
    LiveData<List<RevenueEntity>> getAllRevenuesLiveData();

    @Query("SELECT SUM(totalAmount) FROM revenues WHERE farmId = :farmId")
    double getTotalRevenue(long farmId);

    @Query("SELECT SUM(quantityKg) FROM revenues WHERE farmId = :farmId")
    double getTotalHarvestKg(long farmId);

    @Query("SELECT AVG(pricePerKg) FROM revenues WHERE farmId = :farmId")
    double getAveragePricePerKg(long farmId);

    @Query("SELECT * FROM revenues WHERE batchId = :batchId ORDER BY createdAt DESC")
    List<RevenueEntity> getRevenuesByBatchId(long batchId);
}