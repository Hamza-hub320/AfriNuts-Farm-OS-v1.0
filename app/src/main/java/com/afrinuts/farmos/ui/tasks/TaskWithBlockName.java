package com.afrinuts.farmos.ui.tasks;

import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskWithBlockName {
    private TaskEntity task;
    private String blockName; // Null if farm-wide
    private String assigneeName;

    public TaskWithBlockName(TaskEntity task, String blockName) {
        this.task = task;
        this.blockName = blockName;
    }

    public TaskWithBlockName(TaskEntity task, String blockName, String assigneeName) {
        this(task, blockName);
        this.assigneeName = assigneeName;
    }

    public TaskEntity getTask() { return task; }
    public String getBlockName() { return blockName; }
    public String getAssigneeName() { return assigneeName; }

    public boolean isFarmWide() {
        return blockName == null;
    }

    public String getScopeDisplay() {
        if (isFarmWide()) {
            return "Farm-Wide";
        } else {
            return "Block " + blockName;
        }
    }

    public String getFormattedDueDate() {
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0); today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0); today.set(java.util.Calendar.MILLISECOND, 0);
        long start = today.getTimeInMillis();
        if (task.getDueDate() >= start && task.getDueDate() < start + 24L * 60 * 60 * 1000) return "Today";
        if (task.getDueDate() >= start + 24L * 60 * 60 * 1000 && task.getDueDate() < start + 48L * 60 * 60 * 1000) return "Tomorrow";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        return sdf.format(new Date(task.getDueDate()));
    }

    public String getStatusIcon() {
        switch (task.getStatus()) {
            case PENDING: return "";
            case IN_PROGRESS: return "";
            case COMPLETED: return "";
            case CANCELLED: return "";
            default: return "";
        }
    }

    public int getStatusColor() {
        switch (task.getStatus()) {
            case PENDING: return android.R.color.holo_orange_dark;
            case IN_PROGRESS: return android.R.color.holo_blue_dark;
            case COMPLETED: return android.R.color.holo_green_dark;
            case CANCELLED: return android.R.color.holo_red_dark;
            default: return R.color.navy;
        }
    }

    public boolean isOverdue() {
        return task.isOverdue();
    }
}
