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

    private long taskId;
    private String assignedTo;  // Worker name/ID
    private long assignedAt;
    private Long unassignedAt;  // null if still assigned

    // Constructor
    public TaskAssignmentHistoryEntity(long taskId, String assignedTo, long assignedAt) {
        this.taskId = taskId;
        this.assignedTo = assignedTo;
        this.assignedAt = assignedAt;
        this.unassignedAt = null;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getTaskId() { return taskId; }
    public void setTaskId(long taskId) { this.taskId = taskId; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public long getAssignedAt() { return assignedAt; }
    public void setAssignedAt(long assignedAt) { this.assignedAt = assignedAt; }

    public Long getUnassignedAt() { return unassignedAt; }
    public void setUnassignedAt(Long unassignedAt) { this.unassignedAt = unassignedAt; }

    // Helper method to complete assignment
    public void completeAssignment() {
        this.unassignedAt = System.currentTimeMillis();
    }

    public boolean isActive() {
        return unassignedAt == null;
    }

    public String getDuration() {
        long end = unassignedAt != null ? unassignedAt : System.currentTimeMillis();
        long durationMs = end - assignedAt;

        long days = durationMs / (24 * 60 * 60 * 1000);
        long hours = (durationMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);

        if (days > 0) {
            return days + " days " + hours + " hrs";
        } else {
            return hours + " hours";
        }
    }
}