package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.afrinuts.farmos.data.local.entity.IntegrityFindingEntity;
import java.util.List;

@Dao
public interface IntegrityFindingDao {
    @Insert long insert(IntegrityFindingEntity finding);
    @Update void update(IntegrityFindingEntity finding);
    @Query("SELECT * FROM integrity_findings WHERE id = :id") IntegrityFindingEntity getById(long id);
    @Query("SELECT * FROM integrity_findings WHERE id = :id") LiveData<IntegrityFindingEntity> getByIdLive(long id);
    @Query("SELECT * FROM integrity_findings WHERE farmId = :farmId ORDER BY CASE status WHEN 'OPEN' THEN 0 WHEN 'UNDER_REVIEW' THEN 1 WHEN 'RESOLVED' THEN 2 ELSE 3 END, CASE severity WHEN 'Critical' THEN 0 ELSE 1 END, detectedAt DESC") LiveData<List<IntegrityFindingEntity>> listByFarm(long farmId);
    @Query("SELECT * FROM integrity_findings WHERE farmId = :farmId ORDER BY CASE status WHEN 'OPEN' THEN 0 WHEN 'UNDER_REVIEW' THEN 1 WHEN 'RESOLVED' THEN 2 ELSE 3 END, CASE severity WHEN 'Critical' THEN 0 ELSE 1 END, detectedAt DESC") List<IntegrityFindingEntity> listByFarmSync(long farmId);
    @Query("SELECT * FROM integrity_findings WHERE farmId = :farmId AND ruleCode = :ruleCode AND entityId = :entityId AND status IN ('OPEN','UNDER_REVIEW') LIMIT 1") IntegrityFindingEntity findActive(String ruleCode, String entityId, long farmId);
    @Query("SELECT COUNT(*) FROM integrity_findings WHERE farmId = :farmId AND severity = 'Critical' AND status IN ('OPEN','UNDER_REVIEW')") LiveData<Integer> countOpenCritical(long farmId);
    @Query("SELECT COUNT(*) FROM integrity_findings WHERE farmId = :farmId AND severity = 'Warning' AND status IN ('OPEN','UNDER_REVIEW')") LiveData<Integer> countOpenWarning(long farmId);
    @Query("SELECT COUNT(*) FROM integrity_findings WHERE farmId = :farmId AND status = 'RESOLVED'") LiveData<Integer> countResolved(long farmId);
    @Query("SELECT * FROM integrity_findings WHERE isSynced = 0") List<IntegrityFindingEntity> getUnsynced();
    @Query("UPDATE integrity_findings SET isSynced = 1 WHERE id = :id") void markSynced(long id);
}
