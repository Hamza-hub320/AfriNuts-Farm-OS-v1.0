package com.afrinuts.farmos.ui.workers;

import android.os.Bundle;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.TaskAssignmentRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerDashboardActivity extends BaseActivity {

    private AppDatabase database;
    private TaskAssignmentRepository assignmentRepository;
    private TaskRepository taskRepository;
    private long farmId = 1; // Default
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView totalWorkersValue;
    private TextView totalTasksValue;
    private TextView completionRateValue;
    private TextView avgTaskTimeValue;
    private LinearLayout workerCardsContainer;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;

    private List<WorkerPerformance> workerPerformances = new ArrayList<>();

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_dashboard);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup swipe to refresh
        setupSwipeRefresh();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Load farm ID and initialize repositories
        loadFarmData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        totalWorkersValue = findViewById(R.id.totalWorkersValue);
        totalTasksValue = findViewById(R.id.totalTasksValue);
        completionRateValue = findViewById(R.id.completionRateValue);
        avgTaskTimeValue = findViewById(R.id.avgTaskTimeValue);
        workerCardsContainer = findViewById(R.id.workerCardsContainer);
        progressBar = findViewById(R.id.progressBar);
        emptyLayout = findViewById(R.id.emptyView);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(
                    getResources().getColor(android.R.color.white)
            );
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Worker Performance");
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });

        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.primary),
                ContextCompat.getColor(this, R.color.accent),
                ContextCompat.getColor(this, android.R.color.holo_orange_dark),
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
        );
    }

    private void refreshData() {
        swipeRefreshLayout.setRefreshing(true);
        loadWorkerData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            refreshData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFarmData() {
        executor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm != null) {
                farmId = farm.getId();
            }

            // Initialize repositories
            assignmentRepository = new TaskAssignmentRepository(this, farmId);
            taskRepository = new TaskRepository(this, farmId);

            runOnUiThread(this::loadWorkerData);
        });
    }

    private void loadWorkerData() {
        showLoading(true);

        assignmentRepository.getAllAssignments(new BaseRepository.RepositoryCallback<List<TaskAssignmentHistoryEntity>>() {
            @Override
            public void onSuccess(List<TaskAssignmentHistoryEntity> allAssignments) {
                if (allAssignments.isEmpty()) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        showEmpty(true);
                    });
                    return;
                }

                // Group by worker
                Map<String, List<TaskAssignmentHistoryEntity>> workerAssignments = new HashMap<>();
                for (TaskAssignmentHistoryEntity assignment : allAssignments) {
                    String worker = assignment.getAssignedTo();
                    if (!workerAssignments.containsKey(worker)) {
                        workerAssignments.put(worker, new ArrayList<>());
                    }
                    workerAssignments.get(worker).add(assignment);
                }

                // Calculate performance for each worker
                List<WorkerPerformance> performances = new ArrayList<>();
                for (Map.Entry<String, List<TaskAssignmentHistoryEntity>> entry :
                        workerAssignments.entrySet()) {
                    WorkerPerformance performance = new WorkerPerformance(entry.getKey());

                    for (TaskAssignmentHistoryEntity assignment : entry.getValue()) {
                        performance.addAssignment(assignment);

                        // Get task type for this worker
                        executor.execute(() -> {
                            TaskEntity task = database.taskDao().getTaskById(assignment.getTaskId());
                            if (task != null && !performance.getTaskTypes().contains(
                                    task.getType().getDisplayName())) {
                                performance.getTaskTypes().add(task.getType().getDisplayName());
                            }
                        });
                    }
                    performances.add(performance);
                }

                // Sort by completed tasks (descending)
                Collections.sort(performances,
                        (w1, w2) -> Integer.compare(w2.getCompletedTasks(), w1.getCompletedTasks()));

                final List<WorkerPerformance> finalPerformances = performances;

                runOnUiThread(() -> {
                    workerPerformances.clear();
                    workerPerformances.addAll(finalPerformances);

                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);

                    if (workerPerformances.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        updateOverallStats();
                        displayWorkerCards();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    showEmpty(true);
                });
            }
        });
    }

    private void updateOverallStats() {
        int totalWorkers = workerPerformances.size();
        int totalTasks = 0;
        int completedTasks = 0;
        long totalDays = 0;

        for (WorkerPerformance worker : workerPerformances) {
            totalTasks += worker.getTotalTasks();
            completedTasks += worker.getCompletedTasks();
            totalDays += worker.getTotalDays();
        }

        double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0;
        double avgDays = completedTasks > 0 ? (double) totalDays / completedTasks : 0;

        totalWorkersValue.setText(String.valueOf(totalWorkers));
        totalTasksValue.setText(String.valueOf(totalTasks));
        completionRateValue.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));

        // 42: hours/minutes replaced by days.
        avgTaskTimeValue.setText(WorkerPerformance.formatDays(avgDays));
    }

    private void displayWorkerCards() {
        workerCardsContainer.removeAllViews();

        for (WorkerPerformance worker : workerPerformances) {
            View cardView = getLayoutInflater().inflate(
                    R.layout.item_worker_card, workerCardsContainer, false);

            TextView tvWorkerName = cardView.findViewById(R.id.tvWorkerName);
            TextView tvWorkerStats = cardView.findViewById(R.id.tvWorkerStats);
            TextView tvCompletionRate = cardView.findViewById(R.id.tvCompletionRate);
            ProgressBar workerProgress = cardView.findViewById(R.id.workerProgress);
            TextView tvTotalTime = cardView.findViewById(R.id.tvTotalTime);
            TextView tvAvgTime = cardView.findViewById(R.id.tvAvgTime);
            LinearLayout taskTypeContainer = cardView.findViewById(R.id.taskTypeContainer);

            // Set worker info
            tvWorkerName.setText(worker.getWorkerName());
            tvWorkerStats.setText(String.format(Locale.getDefault(),
                    "%d tasks • %d completed",
                    worker.getTotalTasks(), worker.getCompletedTasks()));

            // Set completion rate
            double rate = worker.getCompletionRate();
            tvCompletionRate.setText(String.format(Locale.getDefault(), "%.0f%%", rate));
            workerProgress.setProgress((int) rate);

            // Set time stats
            tvTotalTime.setText(worker.getFormattedTotalTime());
            tvAvgTime.setText(worker.getFormattedAverageTime());

            // Add task type chips
            taskTypeContainer.removeAllViews();
            for (String taskType : worker.getTaskTypes()) {
                Chip chip = new Chip(this);
                chip.setText(taskType);
                chip.setChipBackgroundColorResource(R.color.warm_grey);
                chip.setTextColor(getColor(R.color.navy));
                chip.setChipCornerRadius(16f);
                chip.setClickable(false);
                chip.setFocusable(false);
                taskTypeContainer.addView(chip);
            }

            // Navigate to worker detail screen
            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(WorkerDashboardActivity.this, WorkerDetailActivity.class);
                intent.putExtra(WorkerDetailActivity.EXTRA_WORKER_NAME, worker.getWorkerName());
                startActivity(intent);
            });

            workerCardsContainer.addView(cardView);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        workerCardsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        emptyLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        workerCardsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}