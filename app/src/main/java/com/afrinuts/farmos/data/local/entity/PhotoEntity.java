package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "photos",
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
                ),
                @ForeignKey(
                        entity = TaskEntity.class,
                        parentColumns = "id",
                        childColumns = "taskId",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = ExpenseEntity.class,
                        parentColumns = "id",
                        childColumns = "expenseId",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = InspectionEntity.class,
                        parentColumns = "inspectionId",
                        childColumns = "inspectionId",
                        onDelete = ForeignKey.SET_NULL
                )
        })
public class PhotoEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;
    private Long blockId;      // null if not attached to a block
    private Long taskId;       // null if not attached to a task
    private Long expenseId;    // null if not attached to an expense
    private String inspectionId; // null if not attached to an inspection

    private String filePath;   // Path to the image file
    private String thumbnailPath; // Path to thumbnail for faster loading
    private String caption;
    private long takenAt;      // When the photo was taken
    private double latitude;    // GPS coordinates (optional)
    private double longitude;   // GPS coordinates (optional)
    private String photoType;   // Store type as string: "BLOCK", "TASK", "EXPENSE", "GENERAL"

    private long createdAt;
    private long updatedAt;

    // Constants for photo types (use these instead of enum with resources)
    public static final String TYPE_BLOCK = "BLOCK";
    public static final String TYPE_TASK = "TASK";
    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INSPECTION = "INSPECTION";
    public static final String TYPE_GENERAL = "GENERAL";

    public PhotoEntity(long farmId, Long blockId, Long taskId, Long expenseId,
                       String filePath, String caption, long takenAt) {
        this.farmId = farmId;
        this.blockId = blockId;
        this.taskId = taskId;
        this.expenseId = expenseId;
        this.filePath = filePath;
        this.caption = caption;
        this.takenAt = takenAt;
        this.latitude = 0;
        this.longitude = 0;

        // Determine photo type based on which ID is present
        if (blockId != null) {
            this.photoType = TYPE_BLOCK;
        } else if (taskId != null) {
            this.photoType = TYPE_TASK;
        } else if (expenseId != null) {
            this.photoType = TYPE_EXPENSE;
        } else {
            this.photoType = TYPE_GENERAL;
        }

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
        updatePhotoType();
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
        updatePhotoType();
    }

    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
        updatePhotoType();
    }

    public String getInspectionId() { return inspectionId; }
    public void setInspectionId(String inspectionId) {
        this.inspectionId = inspectionId;
        updatePhotoType();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCaption() { return caption; }
    public void setCaption(String caption) {
        this.caption = caption;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getTakenAt() { return takenAt; }
    public void setTakenAt(long takenAt) {
        this.takenAt = takenAt;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPhotoType() { return photoType; }
    public void setPhotoType(String photoType) {
        this.photoType = photoType;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    private void updatePhotoType() {
        if (blockId != null) {
            this.photoType = TYPE_BLOCK;
        } else if (taskId != null) {
            this.photoType = TYPE_TASK;
        } else if (expenseId != null) {
            this.photoType = TYPE_EXPENSE;
        } else if (inspectionId != null) {
            this.photoType = TYPE_INSPECTION;
        } else {
            this.photoType = TYPE_GENERAL;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    // Helper method to get display name for UI (to be used in adapter)
    public String getTypeDisplayName() {
        switch (photoType) {
            case TYPE_BLOCK:
                return "Block Photo";
            case TYPE_TASK:
                return "Task Photo";
            case TYPE_EXPENSE:
                return "Expense Photo";
            case TYPE_INSPECTION:
                return "Inspection Photo";
            case TYPE_GENERAL:
            default:
                return "Photo";
        }
    }

    // Helper method to get icon resource ID (to be used in UI layer)
    public int getIconResId() {
        switch (photoType) {
            case TYPE_BLOCK:
                return com.afrinuts.farmos.R.drawable.ic_photo_block;
            case TYPE_TASK:
                return com.afrinuts.farmos.R.drawable.ic_photo_task;
            case TYPE_EXPENSE:
                return com.afrinuts.farmos.R.drawable.ic_photo_expense;
            case TYPE_INSPECTION:
                return com.afrinuts.farmos.R.drawable.ic_photo;
            case TYPE_GENERAL:
            default:
                return com.afrinuts.farmos.R.drawable.ic_photo;
        }
    }
}
