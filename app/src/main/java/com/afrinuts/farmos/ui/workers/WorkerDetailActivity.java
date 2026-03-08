package com.afrinuts.farmos.ui.workers;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkerDetailActivity extends AppCompatActivity {

    public static final String EXTRA_WORKER_NAME = "worker_name";

    private String workerName;
    private AppDatabase database;
    private List<TaskAssignmentHistoryEntity> assignments;
    private Map<Long, TaskEntity> taskCache = new HashMap<>();
    private Map<Long, String> blockNameMap = new HashMap<>();

    // UI Elements
    private TextView workerNameText;
    private TextView workerStatsText;
    private TextView completionRateBadge;
    private TextView totalTasksValue;
    private TextView completedTasksValue;
    private TextView completionRateValue;
    private TextView totalTimeValue;
    private TextView avgTimeValue;
    private ProgressBar efficiencyProgress;
    private LinearLayout taskTypesContainer;
    private LinearLayout recentTasksContainer;
    private TextView noTasksText;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_detail);

        // Get worker name from intent
        workerName = getIntent().getStringExtra(EXTRA_WORKER_NAME);
        if (workerName == null) {
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Load data
        loadWorkerData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        workerNameText = findViewById(R.id.workerNameText);
        workerStatsText = findViewById(R.id.workerStatsText);
        completionRateBadge = findViewById(R.id.completionRateBadge);
        totalTasksValue = findViewById(R.id.totalTasksValue);
        completedTasksValue = findViewById(R.id.completedTasksValue);
        completionRateValue = findViewById(R.id.completionRateValue);
        totalTimeValue = findViewById(R.id.totalTimeValue);
        avgTimeValue = findViewById(R.id.avgTimeValue);
        efficiencyProgress = findViewById(R.id.efficiencyProgress);
        taskTypesContainer = findViewById(R.id.taskTypesContainer);
        recentTasksContainer = findViewById(R.id.recentTasksContainer);
        noTasksText = findViewById(R.id.noTasksText);

        workerNameText.setText(workerName);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Worker Details");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadWorkerData() {
        new Thread(() -> {
            // Get all assignments
            List<TaskAssignmentHistoryEntity> allAssignments = database.taskDao().getAllAssignments();
            List<TaskAssignmentHistoryEntity> workerAssignments = new ArrayList<>();

            // Filter assignments for this worker
            for (TaskAssignmentHistoryEntity assignment : allAssignments) {
                if (assignment.getAssignedTo().equals(workerName)) {
                    workerAssignments.add(assignment);
                    // Cache tasks
                    TaskEntity task = database.taskDao().getTaskById(assignment.getTaskId());
                    if (task != null) {
                        taskCache.put(assignment.getTaskId(), task);
                    }
                }
            }
            assignments = workerAssignments;

            // Load block names using getBlocksByFarmId instead of getAllBlocks
            // Get the first farm (assuming you have one farm)
            com.afrinuts.farmos.data.local.entity.FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm != null) {
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());
                for (BlockEntity block : blocks) {
                    blockNameMap.put(block.getId(), block.getBlockName());
                }
            }

            runOnUiThread(() -> {
                if (assignments.isEmpty()) {
                    showEmptyState();
                } else {
                    calculateAndDisplayStats();
                    displayTaskTypes();
                    displayRecentTasks();
                }
            });
        }).start();
    }

    private void calculateAndDisplayStats() {
        int totalTasks = assignments.size();
        int completedTasks = 0;
        long totalTimeMs = 0;

        for (TaskAssignmentHistoryEntity assignment : assignments) {
            if (assignment.getUnassignedAt() != null) {
                completedTasks++;
                totalTimeMs += (assignment.getUnassignedAt() - assignment.getAssignedAt());
            }
        }

        double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0;
        long avgTimeMs = completedTasks > 0 ? totalTimeMs / completedTasks : 0;

        // Format time
        long totalHours = totalTimeMs / (60 * 60 * 1000);
        long avgHours = avgTimeMs / (60 * 60 * 1000);
        long avgMinutes = (avgTimeMs % (60 * 60 * 1000)) / (60 * 1000);

        // Update UI
        workerStatsText.setText(String.format(Locale.getDefault(),
                "%d total tasks • %d completed", totalTasks, completedTasks));

        completionRateBadge.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));

        totalTasksValue.setText(String.valueOf(totalTasks));
        completedTasksValue.setText(String.valueOf(completedTasks));
        completionRateValue.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));

        totalTimeValue.setText(totalHours + "h");

        if (avgHours > 0) {
            avgTimeValue.setText(avgHours + "h " + avgMinutes + "m");
        } else {
            avgTimeValue.setText(avgMinutes + "m");
        }

        efficiencyProgress.setProgress((int) completionRate);
    }

    private void displayTaskTypes() {
        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Integer> typeIconMap = new HashMap<>();

        for (TaskAssignmentHistoryEntity assignment : assignments) {
            TaskEntity task = taskCache.get(assignment.getTaskId());
            if (task != null) {
                String typeName = task.getType().getDisplayName();
                typeCount.put(typeName, typeCount.getOrDefault(typeName, 0) + 1);
                typeIconMap.putIfAbsent(typeName, getIconForTaskType(task.getType()));
            }
        }

        int maxCount = 0;
        for (int count : typeCount.values()) {
            maxCount = Math.max(maxCount, count);
        }

        taskTypesContainer.removeAllViews();

        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            View chipView = getLayoutInflater().inflate(
                    R.layout.item_task_type_chip, taskTypesContainer, false);

            ImageView iconView = chipView.findViewById(R.id.taskTypeIcon);
            TextView nameView = chipView.findViewById(R.id.taskTypeName);
            TextView countView = chipView.findViewById(R.id.taskTypeCount);
            ProgressBar progressBar = chipView.findViewById(R.id.taskTypeProgress);

            Integer iconRes = typeIconMap.get(entry.getKey());
            if (iconRes != null) {
                iconView.setImageResource(iconRes);
                iconView.setColorFilter(ContextCompat.getColor(this, R.color.primary));
            }

            nameView.setText(entry.getKey());
            countView.setText(String.valueOf(entry.getValue()));

            int progress = maxCount > 0 ? (entry.getValue() * 100 / maxCount) : 0;
            progressBar.setProgress(progress);

            taskTypesContainer.addView(chipView);
        }
    }

    private int getIconForTaskType(TaskEntity.TaskType type) {
        switch (type) {
            case CLEARING: return R.drawable.ic_tractor;
            case PLOWING: return R.drawable.ic_grain;
            case PLANTING: return R.drawable.ic_grass;
            case FERTILIZING: return R.drawable.ic_fertilizer;
            case PRUNING: return R.drawable.ic_cut;
            case HARVEST: return R.drawable.ic_harvest;
            case IRRIGATION: return R.drawable.ic_water_drop;
            case MAINTENANCE: return R.drawable.ic_construction;
            default: return R.drawable.ic_task;
        }
    }

    private void displayRecentTasks() {
        // Sort assignments by date (most recent first)
        List<TaskAssignmentHistoryEntity> sorted = new ArrayList<>(assignments);
        sorted.sort((a1, a2) -> Long.compare(a2.getAssignedAt(), a1.getAssignedAt()));

        // Take top 5
        int limit = Math.min(5, sorted.size());
        sorted = sorted.subList(0, limit);

        recentTasksContainer.removeAllViews();

        for (TaskAssignmentHistoryEntity assignment : sorted) {
            TaskEntity task = taskCache.get(assignment.getTaskId());
            if (task == null) continue;

            View taskView = getLayoutInflater().inflate(
                    R.layout.item_recent_task, recentTasksContainer, false);

            ImageView statusIcon = taskView.findViewById(R.id.taskStatusIcon);
            TextView taskTitle = taskView.findViewById(R.id.taskTitle);
            TextView taskBlockInfo = taskView.findViewById(R.id.taskBlockInfo);
            TextView taskDate = taskView.findViewById(R.id.taskDate);

            // Set status icon
            int iconRes;
            int tintColor;
            if (task.getStatus() == TaskEntity.TaskStatus.COMPLETED) {
                iconRes = R.drawable.ic_check_circle;
                tintColor = ContextCompat.getColor(this, android.R.color.holo_green_dark);
            } else if (task.getStatus() == TaskEntity.TaskStatus.IN_PROGRESS) {
                iconRes = R.drawable.ic_sync;
                tintColor = ContextCompat.getColor(this, android.R.color.holo_blue_dark);
            } else {
                iconRes = R.drawable.ic_hourglass_empty;
                tintColor = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
            }
            statusIcon.setImageResource(iconRes);
            statusIcon.setColorFilter(tintColor);

            taskTitle.setText(task.getTitle());

            if (task.getBlockId() != null) {
                String blockName = blockNameMap.get(task.getBlockId());
                if (blockName != null) {
                    taskBlockInfo.setText("Block " + blockName);
                } else {
                    taskBlockInfo.setText("Unknown Block");
                }
            } else {
                taskBlockInfo.setText("Farm-Wide");
            }

            taskDate.setText(dateFormat.format(new Date(assignment.getAssignedAt())));

            recentTasksContainer.addView(taskView);
        }
    }

    private void showEmptyState() {
        noTasksText.setVisibility(View.VISIBLE);
        recentTasksContainer.setVisibility(View.GONE);
        taskTypesContainer.setVisibility(View.GONE);

        workerStatsText.setText("No tasks assigned yet");
        completionRateBadge.setText("0%");
        totalTasksValue.setText("0");
        completedTasksValue.setText("0");
        completionRateValue.setText("0%");
        totalTimeValue.setText("0h");
        avgTimeValue.setText("0h");
        efficiencyProgress.setProgress(0);
    }
}