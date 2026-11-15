package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_assignments",
        foreignKeys = {
                @ForeignKey(
                        entity = TaskEntity.class,
                        parentColumns = "id",
                        childColumns = "taskId",
                        onDelete = ForeignKey.CASCADE
                )
        })
public class TaskAssignmentHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;
    private long taskId;
    private String assignedTo;  // Worker name/ID
    private long assignedAt;
    private Long unassignedAt;  // null if still assigned

    // Add these fields
    private long createdAt;
    private long updatedAt;

    public TaskAssignmentHistoryEntity() {
    }

    // Constructor
    public TaskAssignmentHistoryEntity(long farmId, long taskId, String assignedTo, long assignedAt) {
        this.farmId = farmId;
        this.taskId = taskId;
        this.assignedTo = assignedTo;
        this.assignedAt = assignedAt;
        this.unassignedAt = null;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public long getTaskId() { return taskId; }
    public void setTaskId(long taskId) { this.taskId = taskId; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public long getAssignedAt() { return assignedAt; }

    public void setAssignedAt(long assignedAt) {
        this.assignedAt = assignedAt;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getUnassignedAt() { return unassignedAt; }
    public void setUnassignedAt(Long unassignedAt) {
        this.unassignedAt = unassignedAt;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Helper method to complete assignment
    public void completeAssignment() {
        this.unassignedAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isActive() {
        return unassignedAt == null;
    }

    /**
     * 42: assignment duration in calendar days, minimum 1.
     *
     * Both timestamps are truncated to local midnight before subtracting, so a
     * 14-day contract (7 Aug 13:11 → 21 Aug 12:57) reads 14 days, not 13.
     * An active assignment (unassignedAt == null) measures to now, matching the
     * behaviour of the previous getDuration().
     *
     * Same-day start-and-finish is 1 day, never 0 — a full labour day.
     */
    public long getDurationDays() {
        long end = (unassignedAt != null) ? unassignedAt : System.currentTimeMillis();

        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(assignedAt);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        long startDayMs = c.getTimeInMillis();

        c.setTimeInMillis(end);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        long endDayMs = c.getTimeInMillis();

        long days = (endDayMs - startDayMs) / 86_400_000L;
        return Math.max(1, days);
    }

    /**
     * 42: human label for the assignment duration — "14 days", "1 day".
     * The days arithmetic lives in getDurationDays(); this method only formats.
     */
    public String getDuration() {
        long d = getDurationDays();
        return d == 1 ? "1 day" : d + " days";
    }
}