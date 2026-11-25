package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
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

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY lastAttemptAt ASC")
    LiveData<List<SyncEntity>> getPendingSyncItemsLiveData();

    @Query("SELECT * FROM sync_queue WHERE status = 'COMPLETED' ORDER BY lastAttemptAt DESC LIMIT 3")
    LiveData<List<SyncEntity>> getRecentCompletedLiveData();

    @Query("UPDATE sync_queue SET status = 'PENDING' WHERE status = 'SYNCING' AND lastAttemptAt < :cutoffTime")
    int reclaimStaleSyncing(long cutoffTime);

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    SyncEntity getSyncItem(String entityType, long entityId);

    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED' AND lastAttemptAt < :cutoffTime")
    int deleteOldCompleted(long cutoffTime);

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED'")
    int getPendingCount();

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED'")
    LiveData<Integer> getPendingCountLiveData();

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    SyncEntity getSyncItemById(long id);

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityStringId = :entityStringId LIMIT 1")
    SyncEntity getSyncItemByStringId(String entityType, String entityStringId);

    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityStringId = :entityStringId")
    void deleteByStringId(String entityType, String entityStringId);
}
