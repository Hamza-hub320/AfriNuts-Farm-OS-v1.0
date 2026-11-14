package com.afrinuts.farmos.ui.workers;

import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;

import java.util.ArrayList;
import java.util.List;

public class WorkerPerformance {
    private String workerName;
    private List<TaskAssignmentHistoryEntity> assignments;
    private int totalTasks;
    private int completedTasks;
    private long totalDays;
    private double averageDaysPerTask;
    private List<String> taskTypes = new ArrayList<>();

    public WorkerPerformance(String workerName) {
        this.workerName = workerName;
        this.assignments = new ArrayList<>();
    }

    public String getWorkerName() { return workerName; }

    public List<TaskAssignmentHistoryEntity> getAssignments() { return assignments; }
    public void addAssignment(TaskAssignmentHistoryEntity assignment) {
        this.assignments.add(assignment);
        calculateStats();
    }

    public int getTotalTasks() { return totalTasks; }
    public int getCompletedTasks() { return completedTasks; }
    public int getInProgressTasks() { return totalTasks - completedTasks; }

    public long getTotalDays() { return totalDays; }
    public double getAverageDaysPerTask() { return averageDaysPerTask; }

    /**
     * 42: total work, in days. The only time unit the app displays —
     * farm labour is contract or monthly, not hourly.
     */
    public String getFormattedTotalTime() {
        return formatDays(totalDays);
    }

    /**
     * 42: average per completed task, in days.
     */
    public String getFormattedAverageTime() {
        return formatDays(averageDaysPerTask);
    }

    /**
     * 42: the one formatter for every days display — worker cards, dashboard
     * aggregate, detail screen. Whole days print without a decimal
     * ("14 days", "1 day"); fractional averages print one decimal
     * ("7.5 days"); trailing ".0" is trimmed.
     */
    public static String formatDays(double days) {
        if (days == 1.0) return "1 day";
        String s;
        if (days == Math.floor(days)) {
            s = String.valueOf((long) days);
        } else {
            s = String.format(java.util.Locale.getDefault(), "%.1f", days);
        }
        return s + " days";
    }

    public double getCompletionRate() {
        return totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0;
    }

    public List<String> getTaskTypes() { return taskTypes; }

    private void calculateStats() {
        totalTasks = assignments.size();
        completedTasks = 0;
        totalDays = 0;
        taskTypes.clear();

        for (TaskAssignmentHistoryEntity assignment : assignments) {
            if (assignment.getUnassignedAt() != null) {
                completedTasks++;
                totalDays += assignment.getDurationDays();
            }
        }

        averageDaysPerTask = completedTasks > 0
                ? (double) totalDays / completedTasks
                : 0;
    }
}