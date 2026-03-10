package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks",
        foreignKeys = {
                @ForeignKey(
                        entity = FarmEntity.class,
                        parentColumns = "id",
                        childColumns = "farmId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = BlockEntity.class,
                        parentColumns = "id",
                        childColumns = "blockId",
                        onDelete = ForeignKey.SET_NULL
                )
        })
public class TaskEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;
    private Long blockId;  // null for farm-wide tasks

    private String title;
    private String description;

    private TaskStatus status;
    private TaskType type;

    private long dueDate;
    private Long completedDate;  // null if not completed

    // Audit timestamps
    private long createdAt;
    private long updatedAt;

    public enum TaskStatus {
        PENDING("Pending", "Not started"),
        IN_PROGRESS("In Progress", "Currently being worked on"),
        COMPLETED("Completed", "Finished successfully"),
        CANCELLED("Cancelled", "No longer needed");

        private final String displayName;
        private final String description;

        TaskStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public enum TaskType {
        CLEARING("Land Clearing", "Clear land for planting"),
        PLOWING("Plowing", "Prepare soil"),
        PLANTING("Planting", "Plant cashew trees"),
        REPLACEMENT("Replacement", "Replace dead trees"),
        FERTILIZING("Fertilizing", "Apply fertilizer"),
        PRUNING("Pruning", "Prune trees"),
        WEEDING("Weeding", "Remove weeds"),
        HARVEST("Harvest", "Harvest cashews"),
        IRRIGATION("Irrigation", "Water management"),
        MAINTENANCE("Maintenance", "General maintenance"),
        OTHER("Other", "Miscellaneous tasks");

        private final String displayName;
        private final String description;

        TaskType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // Constructor
    public TaskEntity(long farmId, Long blockId, String title, String description,
                      TaskType type, TaskStatus status, long dueDate) {
        this.farmId = farmId;
        this.blockId = blockId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = status;
        this.dueDate = dueDate;
        this.completedDate = null;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) {
        this.blockId = blockId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) {
        this.status = status;
        if (status == TaskStatus.COMPLETED && completedDate == null) {
            this.completedDate = System.currentTimeMillis();
        }
        this.updatedAt = System.currentTimeMillis();
    }

    public TaskType getType() { return type; }
    public void setType(TaskType type) {
        this.type = type;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getCompletedDate() { return completedDate; }
    public void setCompletedDate(Long completedDate) {
        this.completedDate = completedDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    public boolean isOverdue() {
        if (isCompleted()) return false;
        return System.currentTimeMillis() > dueDate;
    }

    public String getFormattedDueDate() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(dueDate));
    }

    public String getBlockDisplay() {
        if (blockId == null) {
            return "Farm-Wide";
        } else {
            return "Block #" + blockId;
        }
    }
}