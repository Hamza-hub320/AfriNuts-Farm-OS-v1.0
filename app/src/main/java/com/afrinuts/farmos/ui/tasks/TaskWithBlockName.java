package com.afrinuts.farmos.ui.tasks;

import com.afrinuts.farmos.data.local.entity.TaskEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskWithBlockName {
    private TaskEntity task;
    private String blockName; // Null if farm-wide

    public TaskWithBlockName(TaskEntity task, String blockName) {
        this.task = task;
        this.blockName = blockName;
    }

    public TaskEntity getTask() { return task; }
    public String getBlockName() { return blockName; }

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
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(task.getDueDate()));
    }

    public String getStatusIcon() {
        switch (task.getStatus()) {
            case PENDING: return "⏳";
            case IN_PROGRESS: return "🔄";
            case COMPLETED: return "✅";
            case CANCELLED: return "❌";
            default: return "📌";
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