package com.afrinuts.farmos.ui.workers;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.TaskAssignmentRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerDashboardFragment extends Fragment {

    private AppDatabase database;
    private TaskAssignmentRepository assignmentRepository;
    private TaskRepository taskRepository;
    private long farmId = 1;
    private FarmEntity currentFarm;  // ADD THIS FIELD
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_worker_dashboard, container, false);
        initViews(view);
        setupSwipeRefresh();
        loadFarmData();
        return view;
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        totalWorkersValue = view.findViewById(R.id.totalWorkersValue);
        totalTasksValue = view.findViewById(R.id.totalTasksValue);
        completionRateValue = view.findViewById(R.id.completionRateValue);
        avgTaskTimeValue = view.findViewById(R.id.avgTaskTimeValue);
        workerCardsContainer = view.findViewById(R.id.workerCardsContainer);
        progressBar = view.findViewById(R.id.progressBar);
        emptyLayout = view.findViewById(R.id.emptyView);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshData();
            });

            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(requireContext(), R.color.primary),
                    ContextCompat.getColor(requireContext(), R.color.accent),
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark),
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            );
        }
    }

    public void refreshData() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        forceRefreshWorkerData();
    }

    private void loadFarmData() {
        executor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm != null) {
                currentFarm = farm;  // STORE the farm
                farmId = farm.getId();
            }

            // Initialize repositories
            assignmentRepository = new TaskAssignmentRepository(requireContext(), farmId);
            taskRepository = new TaskRepository(requireContext(), farmId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(this::loadWorkerData);
            }
        });
    }

    private void loadWorkerData() {
        showLoading(true);

        assignmentRepository.getAllAssignments(new BaseRepository.RepositoryCallback<List<TaskAssignmentHistoryEntity>>() {
            @Override
            public void onSuccess(List<TaskAssignmentHistoryEntity> allAssignments) {
                if (allAssignments == null || allAssignments.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                            showEmpty(true);
                        });
                    }
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

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        workerPerformances.clear();
                        workerPerformances.addAll(finalPerformances);

                        showLoading(false);
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }

                        if (workerPerformances.isEmpty()) {
                            showEmpty(true);
                        } else {
                            showEmpty(false);
                            updateOverallStats();
                            displayWorkerCards();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        showEmpty(true);
                    });
                }
            }
        });
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

        if (totalWorkersValue != null) {
            totalWorkersValue.setText(String.valueOf(totalWorkers));
        }
        if (totalTasksValue != null) {
            totalTasksValue.setText(String.valueOf(totalTasks));
        }
        if (completionRateValue != null) {
            completionRateValue.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));
        }

        long avgHours = avgTimeMs / (60 * 60 * 1000);
        long avgMinutes = (avgTimeMs % (60 * 60 * 1000)) / (60 * 1000);

        if (avgTaskTimeValue != null) {
            if (avgHours > 0) {
                avgTaskTimeValue.setText(avgHours + "h " + avgMinutes + "m");
            } else {
                avgTaskTimeValue.setText(avgMinutes + "m");
            }
        }
    }

    private void displayWorkerCards() {
        if (workerCardsContainer == null) return;

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
                Chip chip = new Chip(requireContext());
                chip.setText(taskType);
                chip.setChipBackgroundColorResource(R.color.warm_grey);
                chip.setTextColor(requireContext().getColor(R.color.navy));
                chip.setChipCornerRadius(16f);
                chip.setClickable(false);
                chip.setFocusable(false);
                taskTypeContainer.addView(chip);
            }

            // Navigate to worker detail screen
            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), WorkerDetailActivity.class);
                intent.putExtra(WorkerDetailActivity.EXTRA_WORKER_NAME, worker.getWorkerName());
                startActivity(intent);
            });

            workerCardsContainer.addView(cardView);
        }
    }

    private void forceRefreshWorkerData() {
        if (currentFarm == null) {
            Log.d(TAG, "currentFarm is null, loading from local DB only");
            loadWorkerData();
            return;
        }

        // Get all assignments from Firestore and update local DB
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String farmIdStr = String.valueOf(currentFarm.getId());

        db.collection("farms")
                .document(farmIdStr)
                .collection("task_assignments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // ✅ Run database operations on background thread
                    executor.execute(() -> {
                        int updatedCount = 0;
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            TaskAssignmentHistoryEntity assignment =
                                    EntityMapper.fromFirestoreAssignment(doc);
                            if (assignment != null) {
                                // ✅ Now safe - on background thread
                                TaskAssignmentHistoryEntity existing =
                                        database.taskAssignmentDao().getAssignmentById(assignment.getId());
                                if (existing == null) {
                                    database.taskAssignmentDao().insert(assignment);
                                    updatedCount++;
                                } else {
                                    // Update if newer
                                    if (assignment.getAssignedAt() > existing.getAssignedAt()) {
                                        database.taskAssignmentDao().update(assignment);
                                        updatedCount++;
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "Force refresh: Updated " + updatedCount + " assignments");

                        // ✅ Back to UI thread to reload data
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> loadWorkerData());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to force refresh assignments", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> loadWorkerData());
                    }
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (workerCardsContainer != null) {
            workerCardsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (emptyLayout != null) {
            emptyLayout.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show) {
        if (emptyLayout != null) {
            emptyLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (workerCardsContainer != null) {
            workerCardsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentFarm != null) {
            forceRefreshWorkerData();
        } else {
            loadWorkerData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}