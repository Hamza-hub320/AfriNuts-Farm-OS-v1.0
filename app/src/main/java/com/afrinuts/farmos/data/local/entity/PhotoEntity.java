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
                )
        })
public class PhotoEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;
    private Long blockId;      // null if not attached to a block
    private Long taskId;       // null if not attached to a task
    private Long expenseId;    // null if not attached to an expense

    private String filePath;   // Path to the image file
    private String thumbnailPath; // Path to thumbnail for faster loading
    private String caption;
    private long takenAt;      // When the photo was taken
    private double latitude;    // GPS coordinates (optional)
    private double longitude;   // GPS coordinates (optional)

    private long createdAt;
    private long updatedAt;

    public enum PhotoType {
        BLOCK("Block", R.drawable.ic_photo_block),
        TASK("Task", R.drawable.ic_photo_task),
        EXPENSE("Expense", R.drawable.ic_photo_expense),
        GENERAL("General", R.drawable.ic_photo);

        private final String displayName;
        private final int iconResId;

        PhotoType(String displayName, int iconResId) {
            this.displayName = displayName;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }
    }

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
    public void setBlockId(Long blockId) { this.blockId = blockId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }

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

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public PhotoType getType() {
        if (blockId != null) return PhotoType.BLOCK;
        if (taskId != null) return PhotoType.TASK;
        if (expenseId != null) return PhotoType.EXPENSE;
        return PhotoType.GENERAL;
    }
}