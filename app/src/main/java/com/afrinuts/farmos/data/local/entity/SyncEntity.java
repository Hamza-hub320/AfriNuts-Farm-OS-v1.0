package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_queue")
public class SyncEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String entityType; // "block", "expense", "revenue", "task", "photo"
    private long entityId;     // ID in local database
    private String farmId;
    private SyncAction action; // "CREATE", "UPDATE", "DELETE"
    private long lastAttemptAt;
    private int attemptCount;
    private SyncStatus status;

    public enum SyncAction {
        CREATE, UPDATE, DELETE
    }

    public enum SyncStatus {
        PENDING, SYNCING, COMPLETED, FAILED
    }

    public SyncEntity(String entityType, long entityId, String farmId, SyncAction action) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.farmId = farmId;
        this.action = action;
        this.status = SyncStatus.PENDING;
        this.attemptCount = 0;
        this.lastAttemptAt = 0;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public long getEntityId() { return entityId; }
    public void setEntityId(long entityId) { this.entityId = entityId; }

    public String getFarmId() { return farmId; }
    public void setFarmId(String farmId) { this.farmId = farmId; }

    public SyncAction getAction() { return action; }
    public void setAction(SyncAction action) { this.action = action; }

    public long getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(long lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }

    public boolean shouldRetry() {
        // Exponential backoff: retry after 1min, 5min, 15min, 30min, 1hour, then give up
        int maxAttempts = 6;
        return status == SyncStatus.FAILED && attemptCount < maxAttempts;
    }

    public long getNextRetryDelay() {
        // Exponential backoff: 1min, 5min, 15min, 30min, 1hour
        int[] delays = {60000, 300000, 900000, 1800000, 3600000};
        int index = Math.min(attemptCount, delays.length - 1);
        return delays[index];
    }
}