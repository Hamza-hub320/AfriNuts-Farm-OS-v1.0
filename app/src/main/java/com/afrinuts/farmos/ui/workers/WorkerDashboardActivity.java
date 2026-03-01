package com.afrinuts.farmos.ui.workers;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkerDashboardActivity extends AppCompatActivity {

    private AppDatabase database;

    // UI Elements
    private TextView totalWorkersValue;
    private TextView totalTasksValue;
    private TextView completionRateValue;
    private TextView avgTaskTimeValue;
    private LinearLayout workerCardsContainer;
    private ProgressBar progressBar;
    private LinearLayout emptyView;

    private List<WorkerPerformance> workerPerformances = new ArrayList<>();
    private Map<String, List<TaskEntity>> taskCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_dashboard);

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

        totalWorkersValue = findViewById(R.id.totalWorkersValue);
        totalTasksValue = findViewById(R.id.totalTasksValue);
        completionRateValue = findViewById(R.id.completionRateValue);
        avgTaskTimeValue = findViewById(R.id.avgTaskTimeValue);
        workerCardsContainer = findViewById(R.id.workerCardsContainer);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Worker Performance");
        }
    }

    private void loadWorkerData() {
        showLoading(true);

        new Thread(() -> {
            // Get all task assignments
            List<TaskAssignmentHistoryEntity> allAssignments =
                    database.taskDao().getAllAssignments(); // You'll need to add this method to TaskDao

            if (allAssignments.isEmpty()) {
                runOnUiThread(() -> {
                    showLoading(false);
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
            workerPerformances.clear();
            for (Map.Entry<String, List<TaskAssignmentHistoryEntity>> entry :
                    workerAssignments.entrySet()) {
                WorkerPerformance performance = new WorkerPerformance(entry.getKey());
                for (TaskAssignmentHistoryEntity assignment : entry.getValue()) {
                    performance.addAssignment(assignment);

                    // Cache task types for this worker
                    TaskEntity task = database.taskDao().getTaskById(assignment.getTaskId());
                    if (task != null && !performance.getTaskTypes().contains(
                            task.getType().getDisplayName())) {
                        performance.getTaskTypes().add(task.getType().getDisplayName());
                    }
                }
                workerPerformances.add(performance);
            }

            // Sort by completed tasks (descending)
            Collections.sort(workerPerformances,
                    (w1, w2) -> Integer.compare(w2.getCompletedTasks(), w1.getCompletedTasks()));

            runOnUiThread(() -> {
                showLoading(false);
                updateOverallStats();
                displayWorkerCards();
            });
        }).start();
    }

    private void updateOverallStats() {
        int totalWorkers = workerPerformances.size();
        int totalTasks = 0;
        int completedTasks = 0;
        long totalTimeMs = 0;

        for (WorkerPerformance worker : workerPerformances) {
            totalTasks += worker.getTotalTasks();
            completedTasks += worker.getCompletedTasks();
            totalTimeMs += worker.getTotalTimeMs();
        }

        double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0;
        long avgTimeMs = completedTasks > 0 ? totalTimeMs / completedTasks : 0;

        totalWorkersValue.setText(String.valueOf(totalWorkers));
        totalTasksValue.setText(String.valueOf(totalTasks));
        completionRateValue.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));

        long avgHours = avgTimeMs / (60 * 60 * 1000);
        long avgMinutes = (avgTimeMs % (60 * 60 * 1000)) / (60 * 1000);

        if (avgHours > 0) {
            avgTaskTimeValue.setText(avgHours + "h " + avgMinutes + "m");
        } else {
            avgTaskTimeValue.setText(avgMinutes + " minutes");
        }
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

            // Add click listener to show worker details
            cardView.setOnClickListener(v -> {
                // TODO: Show worker detail view with all their tasks
                android.widget.Toast.makeText(this,
                        "Worker details coming soon",
                        android.widget.Toast.LENGTH_SHORT).show();
            });

            workerCardsContainer.addView(cardView);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        workerCardsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        workerCardsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}