package com.afrinuts.farmos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.FarmEntity;

import java.util.List;

@Dao
public interface FarmDao {

    @Insert
    long insert(FarmEntity farm);

    @Update
    void update(FarmEntity farm);

    @Delete
    void delete(FarmEntity farm);

    @Query("SELECT * FROM farms WHERE id = :id")
    FarmEntity getFarmById(long id);

    @Query("SELECT * FROM farms LIMIT 1")
    FarmEntity getFirstFarm();

    @Query("SELECT * FROM farms")
    List<FarmEntity> getAllFarms();

    @Query("SELECT COUNT(*) FROM farms")
    int getFarmCount();
}