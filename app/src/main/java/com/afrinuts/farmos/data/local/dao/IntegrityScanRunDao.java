package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.afrinuts.farmos.data.local.entity.IntegrityScanRunEntity;
import java.util.List;

@Dao
public interface IntegrityScanRunDao {
    @Insert long insert(IntegrityScanRunEntity run);
    @Update void update(IntegrityScanRunEntity run);
    @Query("SELECT * FROM integrity_scan_runs WHERE id = :id") IntegrityScanRunEntity getById(long id);
    @Query("SELECT * FROM integrity_scan_runs WHERE id = :id") LiveData<IntegrityScanRunEntity> getByIdLive(long id);
    @Query("SELECT * FROM integrity_scan_runs WHERE farmId = :farmId ORDER BY startedAt DESC") LiveData<List<IntegrityScanRunEntity>> listByFarm(long farmId);
    @Query("SELECT * FROM integrity_scan_runs WHERE farmId = :farmId ORDER BY startedAt DESC") List<IntegrityScanRunEntity> listByFarmSync(long farmId);
    @Query("SELECT * FROM integrity_scan_runs WHERE farmId = :farmId ORDER BY startedAt DESC LIMIT 1") IntegrityScanRunEntity latest(long farmId);
    @Query("SELECT * FROM integrity_scan_runs WHERE farmId = :farmId ORDER BY startedAt DESC LIMIT 1") LiveData<IntegrityScanRunEntity> latestLive(long farmId);
}
