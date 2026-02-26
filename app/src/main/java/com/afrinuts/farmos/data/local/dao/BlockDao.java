package com.afrinuts.farmos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.BlockEntity;

import java.util.List;

@Dao
public interface BlockDao {

    @Insert
    long insert(BlockEntity block);

    @Update
    void update(BlockEntity block);

    @Delete
    void delete(BlockEntity block);

    @Query("SELECT * FROM blocks WHERE id = :id")
    BlockEntity getBlockById(long id);

    @Query("SELECT * FROM blocks WHERE farmId = :farmId ORDER BY blockName ASC")
    List<BlockEntity> getBlocksByFarmId(long farmId);

    @Query("SELECT COUNT(*) FROM blocks WHERE farmId = :farmId")
    int getBlockCount(long farmId);

    @Query("SELECT COUNT(*) FROM blocks WHERE farmId = :farmId AND status = :status")
    int getBlockCountByStatus(long farmId, BlockEntity.BlockStatus status);

    @Query("SELECT SUM(replacementCount) FROM blocks WHERE farmId = :farmId")
    int getTotalReplacements(long farmId);

    @Query("SELECT AVG(survivalRate) FROM blocks WHERE farmId = :farmId")
    double getAverageSurvivalRate(long farmId);
}