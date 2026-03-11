package com.afrinuts.farmos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.afrinuts.farmos.data.local.entity.SyncEntity;

import java.util.List;

@Dao
public interface SyncDao {

    @Insert
    long insert(SyncEntity syncEntity);

    @Update
    void update(SyncEntity syncEntity);

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY lastAttemptAt ASC")
    List<SyncEntity> getPendingSyncItems();

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    SyncEntity getSyncItem(String entityType, long entityId);

    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED' AND lastAttemptAt < :cutoffTime")
    void deleteOldCompleted(long cutoffTime);

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED'")
    int getPendingCount();
}