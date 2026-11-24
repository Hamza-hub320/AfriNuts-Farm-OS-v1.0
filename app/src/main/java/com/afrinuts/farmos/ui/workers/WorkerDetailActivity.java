package com.afrinuts.farmos.ui.workers;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.lifecycle.Observer;

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
import java.util.LinkedHashMap;
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
    private TextView workerAvatar;
    private TextView completionRateBadge;
    private TextView totalTasksValue;
    private TextView completedTasksValue;
    private TextView completionRateValue;
    private TextView efficiencyValue;
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

            // Load worker data on main thread (LiveData requires it)
            runOnUiThread(this::loadWorkerData);
        });
    }

    private void initViews() {

        workerNameText = findViewById(R.id.workerNameText);
        workerStatsText = findViewById(R.id.workerStatsText);
        workerAvatar = findViewById(R.id.workerAvatar);
        completionRateBadge = findViewById(R.id.completionRateBadge);
        totalTasksValue = findViewById(R.id.totalTasksValue);
        completedTasksValue = findViewById(R.id.completedTasksValue);
        completionRateValue = findViewById(R.id.completionRateValue);
        efficiencyValue = findViewById(R.id.efficiencyValue);
        totalTimeValue = findViewById(R.id.totalTimeValue);
        avgTimeValue = findViewById(R.id.avgTimeValue);
        efficiencyProgress = findViewById(R.id.efficiencyProgress);
        taskTypesContainer = findViewById(R.id.taskTypesContainer);
        recentTasksContainer = findViewById(R.id.recentTasksContainer);
        noTasksText = findViewById(R.id.noTasksText);

        workerNameText.setText(workerName);
        workerAvatar.setText(workerInitials(workerName));
        workerAvatar.setBackgroundResource(R.drawable.bg_supplier_avatar_sand);
    }

    private void setupToolbar() {
        com.afrinuts.farmos.ui.farm.FarmDetailChrome.apply(this,
                findViewById(R.id.farmDetailTitle), findViewById(R.id.farmDetailBack),
                findViewById(R.id.farmDetailMore), "Worker details", R.id.workerDetailStatsRow);
        androidx.cardview.widget.CardView hero = findViewById(R.id.workerHeroCard);
        hero.setRadius(28 * getResources().getDisplayMetrics().density);
        hero.setCardElevation(0);
        efficiencyProgress.setProgressDrawable(createEfficiencyProgressDrawable());
    }

    private String workerInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "•";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
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
        // Use LiveData from repository instead of callback
        assignmentRepository.getAssignmentsForWorkerLiveData(workerName)
                .observe(this, assignments -> {
                    if (assignments == null || assignments.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    // Create WorkerPerformance object
                    workerPerformance = new WorkerPerformance(workerName);

                    long joinedAt = Long.MAX_VALUE;
                    for (TaskAssignmentHistoryEntity assignment : assignments) {
                        joinedAt = Math.min(joinedAt, assignment.getAssignedAt());
                    }
                    final String joinedLabel = "Worker · Joined " + new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(joinedAt));

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
                            workerStatsText.setText(joinedLabel);
                            displayStats();
                            displayTaskTypes();
                            displayRecentTasks(assignments);
                        });
                    });
                });
    }

    private void displayStats() {
        if (workerPerformance == null) return;

        double rate = workerPerformance.getCompletionRate();
        completionRateBadge.setText("✓  " + String.format(Locale.getDefault(), "%.0f%% completion", rate));
        completionRateBadge.setTypeface(getResources().getFont(R.font.manrope_extrabold));

        totalTasksValue.setText(String.valueOf(workerPerformance.getTotalTasks()));
        completedTasksValue.setText(String.valueOf(workerPerformance.getCompletedTasks()));
        completionRateValue.setText(String.format(Locale.getDefault(), "%.0f%%", rate));
        totalTimeValue.setText(workerPerformance.getFormattedTotalTime());
        avgTimeValue.setText(workerPerformance.getFormattedAverageTime());
        efficiencyProgress.setProgress((int) rate);
        efficiencyValue.setText(String.format(Locale.getDefault(), "%.0f%%", rate));
    }

    private void displayTaskTypes() {
        if (workerPerformance == null || workerPerformance.getTaskTypes().isEmpty()) {
            taskTypesContainer.setVisibility(View.GONE);
            return;
        }

        taskTypesContainer.setVisibility(View.VISIBLE);
        taskTypesContainer.removeAllViews();

        Map<String, Integer> typeCount = new LinkedHashMap<>();
        for (TaskAssignmentHistoryEntity assignment : workerPerformance.getAssignments()) {
            if (assignment.getUnassignedAt() == null) continue;
            TaskEntity task = taskCache.get(assignment.getTaskId());
            if (task != null) {
                String type = task.getType().getDisplayName();
                typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
            }
        }

        for (String taskType : workerPerformance.getTaskTypes()) {
            int count = typeCount.getOrDefault(taskType, 0);
            if (count == 0) continue;
            View chipView = getLayoutInflater().inflate(
                    R.layout.item_task_type_chip, taskTypesContainer, false);

            ImageView iconView = chipView.findViewById(R.id.taskTypeIcon);
            TextView nameView = chipView.findViewById(R.id.taskTypeName);
            TextView countView = chipView.findViewById(R.id.taskTypeCount);
            ProgressBar progressBar = chipView.findViewById(R.id.taskTypeProgress);

            ViewGroup row = (ViewGroup) chipView;
            row.setPadding(0, dp(11), 0, dp(11));
            row.setBackgroundColor(Color.TRANSPARENT);
            hideParent(iconView);
            hideParent(progressBar);

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
            nameView.setTextColor(Color.parseColor("#1C1B1A"));
            nameView.setTextSize(14.5f);
            nameView.setTypeface(getResources().getFont(R.font.manrope_bold));
            countView.setText(String.valueOf(count));
            countView.setTextColor(Color.parseColor("#0A5530"));
            countView.setTextSize(13.5f);
            countView.setTypeface(getResources().getFont(R.font.manrope_extrabold));
            if (countView.getParent() instanceof View) {
                View countPill = (View) countView.getParent();
                countPill.setBackground(rounded(Color.parseColor("#E3F0E7"), 999));
            }

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

            hideParent(statusIcon);
            taskTitle.setTextColor(Color.parseColor("#1C1B1A"));
            taskTitle.setTextSize(14.5f);
            taskTitle.setTypeface(getResources().getFont(R.font.manrope_extrabold));

            taskBlockInfo.setText(formatAssignmentDateRange(assignment));
            taskBlockInfo.setTextColor(Color.parseColor("#59544C"));
            taskBlockInfo.setTextSize(13f);
            taskBlockInfo.setTypeface(getResources().getFont(R.font.manrope_semibold));
            taskBlockInfo.setMaxLines(1);
            taskBlockInfo.setEllipsize(android.text.TextUtils.TruncateAt.END);
            taskTitle.setText(task.getTitle() + " — " + (task.getBlockId() != null
                    ? "Block " + blockNameMap.getOrDefault(task.getBlockId(), "Unknown Block")
                    : "Farm-Wide"));
            taskTitle.setMaxLines(1);
            taskTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            taskDate.setText(task.getStatus() == TaskEntity.TaskStatus.COMPLETED ? "COMPLETED" : task.getStatus().name().replace('_', ' '));
            taskDate.setTextColor(Color.parseColor("#0A5530"));
            taskDate.setTextSize(10.5f);
            taskDate.setTypeface(getResources().getFont(R.font.manrope_extrabold));
            taskDate.setPadding(dp(9), dp(3), dp(9), dp(3));
            taskDate.setBackground(rounded(Color.parseColor("#E3F0E7"), 999));
            ViewGroup dateContainer = (ViewGroup) taskDate.getParent();
            View dateIcon = dateContainer.getChildAt(0);
            dateIcon.setVisibility(View.GONE);
            dateContainer.setBackgroundColor(Color.TRANSPARENT);
            dateContainer.setPadding(0, 0, 0, 0);
            taskDate.setContentDescription("Task status");
            androidx.cardview.widget.CardView rowCard = (androidx.cardview.widget.CardView) taskView;
            rowCard.setCardElevation(0);
            rowCard.setRadius(0);
            rowCard.setCardBackgroundColor(Color.TRANSPARENT);
            ViewGroup inner = (ViewGroup) rowCard.getChildAt(0);
            inner.setBackgroundColor(Color.TRANSPARENT);
            inner.setPadding(0, dp(11), 0, dp(11));
            taskView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            recentTasksContainer.addView(taskView);
        }
    }

    private String formatAssignmentDateRange(TaskAssignmentHistoryEntity assignment) {
        SimpleDateFormat sameYear = new SimpleDateFormat("MMM d", Locale.getDefault());
        SimpleDateFormat full = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        long start = assignment.getAssignedAt();
        Long end = assignment.getUnassignedAt();
        if (end == null) return full.format(new Date(start));
        java.util.Calendar startCal = java.util.Calendar.getInstance();
        java.util.Calendar endCal = java.util.Calendar.getInstance();
        startCal.setTimeInMillis(start);
        endCal.setTimeInMillis(end);
        String startText = (startCal.get(java.util.Calendar.YEAR) == endCal.get(java.util.Calendar.YEAR))
                ? sameYear.format(new Date(start)) : full.format(new Date(start));
        return startText + " – " + full.format(new Date(end));
    }

    private Drawable createEfficiencyProgressDrawable() {
        GradientDrawable track = rounded(Color.parseColor("#F3EAD8"), 99);
        GradientDrawable progress = rounded(Color.parseColor("#0E6B3C"), 99);
        android.graphics.drawable.ClipDrawable clipped = new android.graphics.drawable.ClipDrawable(
                progress, Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL);
        return new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{track, clipped}) {
            { setId(0, android.R.id.background); setId(1, android.R.id.progress); }
        };
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideParent(View child) {
        if (child != null && child.getParent() instanceof View) {
            ((View) child.getParent()).setVisibility(View.GONE);
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
        // Navigate back to WorkerDashboardFragment
        finish();
        super.onBackPressed();  // Add this line
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
        efficiencyValue.setText("0%");
        completionRateBadge.setText("✓  0% completion");
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
