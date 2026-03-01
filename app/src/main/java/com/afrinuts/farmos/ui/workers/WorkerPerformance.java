package com.afrinuts.farmos.ui.workers;

import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;

import java.util.ArrayList;
import java.util.List;

public class WorkerPerformance {
    private String workerName;
    private List<TaskAssignmentHistoryEntity> assignments;
    private int totalTasks;
    private int completedTasks;
    private long totalTimeMs;
    private double averageTimePerTaskMs;
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

    public long getTotalTimeMs() { return totalTimeMs; }
    public double getAverageTimePerTaskMs() { return averageTimePerTaskMs; }

    public String getFormattedTotalTime() {
        long days = totalTimeMs / (24 * 60 * 60 * 1000);
        long hours = (totalTimeMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);

        if (days > 0) {
            return days + "d " + hours + "h";
        } else {
            return hours + " hours";
        }
    }

    public String getFormattedAverageTime() {
        long hours = (long) (averageTimePerTaskMs / (60 * 60 * 1000));
        long minutes = (long) ((averageTimePerTaskMs % (60 * 60 * 1000)) / (60 * 1000));

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + " minutes";
        }
    }

    public double getCompletionRate() {
        return totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0;
    }

    public List<String> getTaskTypes() { return taskTypes; }

    private void calculateStats() {
        totalTasks = assignments.size();
        completedTasks = 0;
        totalTimeMs = 0;
        taskTypes.clear();

        for (TaskAssignmentHistoryEntity assignment : assignments) {
            if (assignment.getUnassignedAt() != null) {
                completedTasks++;
                totalTimeMs += (assignment.getUnassignedAt() - assignment.getAssignedAt());
            }
        }

        averageTimePerTaskMs = completedTasks > 0 ? totalTimeMs / completedTasks : 0;
    }
}