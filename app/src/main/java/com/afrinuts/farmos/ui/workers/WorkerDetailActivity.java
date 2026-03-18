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
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.TaskAssignmentRepository;
import com.afrinuts.farmos.ui.workers.WorkerPerformance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerDetailActivity extends AppCompatActivity {

    public static final String EXTRA_WORKER_NAME = "worker_name";

    private String workerName;
    private long farmId = 1;
    private AppDatabase database;
    private TaskAssignmentRepository assignmentRepository;
    private WorkerPerformance workerPerformance;
    private Map<Long, TaskEntity> taskCache = new HashMap<>();
    private Map<Long, String> blockNameMap = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

        // Get farm ID
        executor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm != null) {
                farmId = farm.getId();
            }

            // Initialize repository
            assignmentRepository = new TaskAssignmentRepository(this, farmId);

            // Load block names
            loadBlockNames();

            // Load worker data
            loadWorkerData();
        });
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
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Worker Details");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadBlockNames() {
        FarmEntity farm = database.farmDao().getFirstFarm();
        if (farm != null) {
            List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());
            for (BlockEntity block : blocks) {
                blockNameMap.put(block.getId(), block.getBlockName());
            }
        }
    }

    private void loadWorkerData() {
        assignmentRepository.getAssignmentsForWorker(workerName, new BaseRepository.RepositoryCallback<List<TaskAssignmentHistoryEntity>>() {
            @Override
            public void onSuccess(List<TaskAssignmentHistoryEntity> assignments) {
                if (assignments.isEmpty()) {
                    runOnUiThread(() -> showEmptyState());
                    return;
                }

                // Create WorkerPerformance object
                workerPerformance = new WorkerPerformance(workerName);

                // Load tasks into cache
                executor.execute(() -> {
                    for (TaskAssignmentHistoryEntity assignment : assignments) {
                        workerPerformance.addAssignment(assignment);

                        TaskEntity task = database.taskDao().getTaskById(assignment.getTaskId());
                        if (task != null) {
                            taskCache.put(assignment.getTaskId(), task);

                            // Add task type
                            if (!workerPerformance.getTaskTypes().contains(task.getType().getDisplayName())) {
                                workerPerformance.getTaskTypes().add(task.getType().getDisplayName());
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        displayStats();
                        displayTaskTypes();
                        displayRecentTasks(assignments);
                    });
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> showEmptyState());
            }
        });
    }

    private void displayStats() {
        if (workerPerformance == null) return;

        workerStatsText.setText(String.format(Locale.getDefault(),
                "%d total tasks • %d completed",
                workerPerformance.getTotalTasks(),
                workerPerformance.getCompletedTasks()));

        double rate = workerPerformance.getCompletionRate();
        completionRateBadge.setText(String.format(Locale.getDefault(), "%.0f%%", rate));

        totalTasksValue.setText(String.valueOf(workerPerformance.getTotalTasks()));
        completedTasksValue.setText(String.valueOf(workerPerformance.getCompletedTasks()));
        completionRateValue.setText(String.format(Locale.getDefault(), "%.0f%%", rate));
        totalTimeValue.setText(workerPerformance.getFormattedTotalTime());
        avgTimeValue.setText(workerPerformance.getFormattedAverageTime());
        efficiencyProgress.setProgress((int) rate);
    }

    private void displayTaskTypes() {
        if (workerPerformance == null || workerPerformance.getTaskTypes().isEmpty()) {
            taskTypesContainer.setVisibility(View.GONE);
            return;
        }

        taskTypesContainer.setVisibility(View.VISIBLE);
        taskTypesContainer.removeAllViews();

        // Create a map of task type to count
        Map<String, Integer> typeCount = new HashMap<>();
        for (String taskType : workerPerformance.getTaskTypes()) {
            typeCount.put(taskType, 1); // Simplified - you could count actual occurrences
        }

        int maxCount = 1; // Simplified

        for (String taskType : workerPerformance.getTaskTypes()) {
            View chipView = getLayoutInflater().inflate(
                    R.layout.item_task_type_chip, taskTypesContainer, false);

            ImageView iconView = chipView.findViewById(R.id.taskTypeIcon);
            TextView nameView = chipView.findViewById(R.id.taskTypeName);
            TextView countView = chipView.findViewById(R.id.taskTypeCount);
            ProgressBar progressBar = chipView.findViewById(R.id.taskTypeProgress);

            // Find a sample task of this type to get icon
            for (Map.Entry<Long, TaskEntity> entry : taskCache.entrySet()) {
                if (entry.getValue().getType().getDisplayName().equals(taskType)) {
                    int iconRes = getIconForTaskType(entry.getValue().getType());
                    iconView.setImageResource(iconRes);
                    iconView.setColorFilter(ContextCompat.getColor(this, R.color.primary));
                    break;
                }
            }

            nameView.setText(taskType);
            countView.setText("1"); // Simplified count

            int progress = 100; // Simplified
            progressBar.setProgress(progress);

            taskTypesContainer.addView(chipView);
        }
    }

    private void displayRecentTasks(List<TaskAssignmentHistoryEntity> assignments) {
        // Sort by date (most recent first)
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
            case REPLACEMENT: return R.drawable.ic_replacement;
            case WEEDING: return R.drawable.ic_grass;
            case OTHER: return R.drawable.ic_task;
            default: return R.drawable.ic_task;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, WorkerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();

        super.onBackPressed();
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
        avgTimeValue.setText("0m");
        efficiencyProgress.setProgress(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (assignmentRepository != null) {
            assignmentRepository.shutdown();
        }
    }
}