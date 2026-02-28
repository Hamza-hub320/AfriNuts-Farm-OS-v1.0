package com.afrinuts.farmos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.RevenueEntity;

import java.util.List;

@Dao
public interface RevenueDao {

    @Insert
    long insert(RevenueEntity revenue);

    @Update
    void update(RevenueEntity revenue);

    @Delete
    void delete(RevenueEntity revenue);

    @Query("SELECT * FROM revenues WHERE id = :id")
    RevenueEntity getRevenueById(long id);

    @Query("SELECT * FROM revenues WHERE farmId = :farmId ORDER BY harvestDate DESC")
    List<RevenueEntity> getRevenuesByFarmId(long farmId);

    @Query("SELECT * FROM revenues WHERE blockId = :blockId ORDER BY harvestDate DESC")
    List<RevenueEntity> getRevenuesByBlockId(long blockId);

    @Query("SELECT * FROM revenues WHERE farmId = :farmId AND blockId IS NULL ORDER BY harvestDate DESC")
    List<RevenueEntity> getFarmWideRevenues(long farmId);

    @Query("SELECT SUM(totalAmount) FROM revenues WHERE farmId = :farmId")
    double getTotalRevenue(long farmId);

    @Query("SELECT SUM(quantityKg) FROM revenues WHERE farmId = :farmId")
    double getTotalHarvestKg(long farmId);

    @Query("SELECT AVG(pricePerKg) FROM revenues WHERE farmId = :farmId")
    double getAveragePricePerKg(long farmId);

    @Query("SELECT blockId, SUM(quantityKg) as totalKg FROM revenues " +
            "WHERE farmId = :farmId AND blockId IS NOT NULL " +
            "GROUP BY blockId ORDER BY totalKg DESC")
    List<BlockYield> getYieldsByBlock(long farmId);

    @Query("SELECT strftime('%Y', harvestDate/1000, 'unixepoch') as year, " +
            "SUM(quantityKg) as totalKg FROM revenues " +
            "WHERE farmId = :farmId GROUP BY year ORDER BY year")
    List<YearlyYield> getYearlyYields(long farmId);

    // Inner class for block yield data
    class BlockYield {
        public long blockId;
        public double totalKg;
    }

    // Inner class for yearly yield data
    class YearlyYield {
        public String year;
        public double totalKg;
    }
}