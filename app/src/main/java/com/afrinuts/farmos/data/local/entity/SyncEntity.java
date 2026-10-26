package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_queue")
public class SyncEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String entityType; // "block", "expense", "revenue", "task", "photo", "certification"
    private long entityId;     // ID in local database (numeric)
    private String entityStringId; // For string IDs like certificationId
    private String farmId;
    private SyncAction action;
    private long lastAttemptAt;
    private int attemptCount;
    private SyncStatus status;

    public enum SyncAction {
        CREATE, UPDATE, DELETE
    }

    public enum SyncStatus {
        PENDING, SYNCING, COMPLETED, FAILED
    }

    // ✅ Default constructor for Room
    public SyncEntity() {
        // Default constructor required for Room
    }

    // ✅ Constructor for numeric IDs - Room will use this one
    public SyncEntity(String entityType, long entityId, String farmId, SyncAction action) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityStringId = null;
        this.farmId = farmId;
        this.action = action;
        this.status = SyncStatus.PENDING;
        this.attemptCount = 0;
        this.lastAttemptAt = 0;
    }

    // ✅ Constructor for string IDs - Ignore this one for Room
    @Ignore
    public SyncEntity(String entityType, String entityStringId, String farmId, SyncAction action) {
        this.entityType = entityType;
        this.entityId = 0;
        this.entityStringId = entityStringId;
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

    public String getEntityStringId() { return entityStringId; }
    public void setEntityStringId(String entityStringId) { this.entityStringId = entityStringId; }

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
        int maxAttempts = 6;
        return status == SyncStatus.FAILED && attemptCount < maxAttempts;
    }

    public long getNextRetryDelay() {
        int[] delays = {60000, 300000, 900000, 1800000, 3600000};
        int index = Math.min(attemptCount, delays.length - 1);
        return delays[index];
    }
}