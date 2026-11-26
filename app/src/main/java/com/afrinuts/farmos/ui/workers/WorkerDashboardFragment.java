package com.afrinuts.farmos.ui.workers;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    public interface WorkerSelectionListener { boolean onWorkerSelected(String workerName); }

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
    private TextInputEditText searchInput;
    private String workerSearchQuery = "";
    private String workerFilter = "ALL";
    private boolean tablet;
    private String selectedWorkerName;
    private WorkerSelectionListener selectionListener;

    private List<WorkerPerformance> workerPerformances = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
    }

    @Override public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof WorkerSelectionListener) selectionListener = (WorkerSelectionListener) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_worker_dashboard, container, false);
        tablet = requireActivity().findViewById(R.id.detailContainer) != null;
        if (tablet) {
            view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background));
            view.setPadding(view.getPaddingLeft(), 0, view.getPaddingRight(), view.getPaddingBottom());
        }
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
        if (tablet) {
            View search = view.findViewById(R.id.layoutWorkerSearch);
            if (search != null) search.setVisibility(View.GONE);
            View summaryRow = totalWorkersValue.getParent() == null ? null : (View) totalWorkersValue.getParent().getParent();
            if (summaryRow != null) summaryRow.setVisibility(View.GONE);
            View filterScroller = (View) view.findViewById(R.id.workerFilterGroup).getParent();
            if (filterScroller != null) filterScroller.setVisibility(View.GONE);
            android.graphics.drawable.GradientDrawable listBackground = new android.graphics.drawable.GradientDrawable();
            listBackground.setColor(android.graphics.Color.WHITE);
            listBackground.setCornerRadius(dp(32));
            workerCardsContainer.setBackground(listBackground);
            workerCardsContainer.setClipToOutline(true);
            workerCardsContainer.setPadding(dp(12), dp(12), dp(12), dp(100));
        }
        progressBar = view.findViewById(R.id.progressBar);
        emptyLayout = view.findViewById(R.id.emptyView);
        searchInput = view.findViewById(R.id.etWorkerSearch);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                workerSearchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                displayWorkerCards();
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
        com.google.android.material.chip.ChipGroup filters = view.findViewById(R.id.workerFilterGroup);
        int[] filterIds = {R.id.chipWorkersAll, R.id.chipWorkersActive, R.id.chipWorkersNoActive};
        String[] filterKeys = {"ALL", "ACTIVE", "NO_ACTIVE"};
        for (int i = 0; i < filterIds.length; i++) {
            final String key = filterKeys[i];
            com.google.android.material.chip.Chip chip = filters.findViewById(filterIds[i]);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked) { workerFilter = key; displayWorkerCards(); }
            });
        }
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
        int activeTasks = 0;
        int completedTasks = 0;
        long totalDays = 0;

        for (WorkerPerformance worker : workerPerformances) {
            totalTasks += worker.getTotalTasks();
            activeTasks += worker.getInProgressTasks();
            completedTasks += worker.getCompletedTasks();
            totalDays += worker.getTotalDays();
        }

        double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0;
        double avgDays = completedTasks > 0 ? (double) totalDays / completedTasks : 0;

        if (totalWorkersValue != null) {
            totalWorkersValue.setText(String.valueOf(totalWorkers));
        }
        if (totalTasksValue != null) {
            totalTasksValue.setText(String.valueOf(activeTasks));
        }
        if (completionRateValue != null) {
            completionRateValue.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));
        }

        // 42: hours/minutes replaced by days.
        if (avgTaskTimeValue != null) {
            avgTaskTimeValue.setText(WorkerPerformance.formatDays(avgDays));
        }
    }

    private void displayWorkerCards() {
        if (workerCardsContainer == null) return;

        workerCardsContainer.removeAllViews();

        int rowIndex = 0;
        for (WorkerPerformance worker : workerPerformances) {
            String searchableName = worker.getWorkerName() == null ? "" : worker.getWorkerName().toLowerCase(Locale.ROOT);
            if (!workerSearchQuery.isEmpty() && !searchableName.contains(workerSearchQuery)) continue;
            if ("ACTIVE".equals(workerFilter) && worker.getInProgressTasks() == 0) continue;
            if ("NO_ACTIVE".equals(workerFilter) && worker.getInProgressTasks() > 0) continue;
            View cardView = getLayoutInflater().inflate(
                    R.layout.item_farm_worker_row, workerCardsContainer, false);
            if (tablet) {
                android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
                background.setColor(worker.getWorkerName().equals(selectedWorkerName) ? 0xFFE3F0E7 : android.graphics.Color.TRANSPARENT);
                background.setCornerRadius(dp(22));
                cardView.setBackground(background);
                LinearLayout.LayoutParams rowParams = (LinearLayout.LayoutParams) cardView.getLayoutParams();
                rowParams.setMargins(0, 0, 0, dp(4));
                cardView.setLayoutParams(rowParams);
            }

            TextView tvWorkerName = cardView.findViewById(R.id.tvWorkerName);
            TextView tvWorkerStats = cardView.findViewById(R.id.tvWorkerStats);
            TextView tvCompletionRate = cardView.findViewById(R.id.tvCompletionRate);
            TextView tvWorkerAvatar = cardView.findViewById(R.id.tvWorkerAvatar);
            TextView tvActiveTasksLabel = cardView.findViewById(R.id.tvActiveTasksLabel);
            View statusDot = cardView.findViewById(R.id.workerStatusDot);
            ProgressBar workerProgress = cardView.findViewById(R.id.workerProgress);
            TextView tvTotalTime = cardView.findViewById(R.id.tvTotalTime);
            TextView tvAvgTime = cardView.findViewById(R.id.tvAvgTime);
            LinearLayout taskTypeContainer = cardView.findViewById(R.id.taskTypeContainer);

            // Set worker info
            String workerName = worker.getWorkerName() == null ? "" : worker.getWorkerName().trim();
            tvWorkerName.setText(workerName.isEmpty() ? "Unnamed worker" : (tablet ? titleCase(workerName) : workerName));
            if (tablet) tvWorkerAvatar.setVisibility(View.GONE);
            String[] initials = workerName.split("\\s+");
            tvWorkerAvatar.setText((initials.length > 1 ? initials[0].substring(0, 1) + initials[1].substring(0, 1)
                    : (workerName.isEmpty() ? "?" : workerName.substring(0, 1))).toUpperCase(Locale.ROOT));
            int[] avatarBackgrounds = {R.drawable.bg_supplier_avatar_green, R.drawable.bg_supplier_avatar_sand, R.drawable.bg_supplier_avatar_plain};
            int[] avatarColors = {0xFF0A5530, 0xFF7A5F22, 0xFF6B675F};
            tvWorkerAvatar.setBackgroundResource(avatarBackgrounds[rowIndex % avatarBackgrounds.length]);
            tvWorkerAvatar.setTextColor(avatarColors[rowIndex % avatarColors.length]);
            rowIndex++;
            tvWorkerStats.setText(String.format(Locale.getDefault(), "Worker · %d tasks · %d completed",
                    worker.getTotalTasks(), worker.getCompletedTasks()));
            statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(worker.getInProgressTasks() > 0 ? 0xFF0E6B3C : 0xFFC9C2B4));

            // Set completion rate
            double rate = worker.getCompletionRate();
            tvCompletionRate.setText(String.valueOf(worker.getInProgressTasks()));
            tvActiveTasksLabel.setText("Active tasks");
            workerProgress.setProgress((int) rate);

            // Set time stats
            tvTotalTime.setText(worker.getFormattedTotalTime());
            tvAvgTime.setText(worker.getFormattedAverageTime());

            if (tablet) {
                long joinedAt = Long.MAX_VALUE;
                for (TaskAssignmentHistoryEntity assignment : worker.getAssignments()) joinedAt = Math.min(joinedAt, assignment.getAssignedAt());
                String joined = joinedAt == Long.MAX_VALUE ? "—" : new java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new java.util.Date(joinedAt));
                tvWorkerStats.setText("Field Worker · Joined " + joined);
                tvWorkerStats.setTextSize(14);
                tvWorkerStats.setTextColor(0xFF514C45);
                tvWorkerStats.setMaxLines(2);
                tvCompletionRate.setText("ON SITE");
                tvCompletionRate.setTextSize(12);
                tvCompletionRate.setTextColor(0xFF075C32);
                tvCompletionRate.setPadding(dp(10), dp(4), dp(10), dp(4));
                android.graphics.drawable.GradientDrawable statusBg = new android.graphics.drawable.GradientDrawable();
                statusBg.setColor(0xFFE3F0E7);
                statusBg.setCornerRadius(dp(99));
                tvCompletionRate.setBackground(statusBg);
                int completedCount = worker.getCompletedTasks();
                tvActiveTasksLabel.setText(completedCount + (completedCount == 1 ? " task completed" : " tasks completed"));
                tvActiveTasksLabel.setTextSize(12);
                tvActiveTasksLabel.setTextColor(0xFF514C45);
            }

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
                if (tablet) selectWorker(worker.getWorkerName());
                else {
                    Intent intent = new Intent(requireContext(), WorkerDetailActivity.class);
                    intent.putExtra(WorkerDetailActivity.EXTRA_WORKER_NAME, worker.getWorkerName());
                    startActivity(intent);
                }
            });

            workerCardsContainer.addView(cardView);
        }
        if (tablet && selectedWorkerName == null && !workerPerformances.isEmpty()) {
            selectedWorkerName = workerPerformances.get(0).getWorkerName();
            displayWorkerCards();
            showSelectedTabletDetail();
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (result.length() > 0) result.append(' ');
            if (!part.isEmpty()) result.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return result.toString();
    }

    private void selectWorker(String name) {
        if (name == null) return;
        selectedWorkerName = name;
        displayWorkerCards();
        if (selectionListener != null) selectionListener.onWorkerSelected(name);
    }

    public void showSelectedTabletDetail() {
        if (!tablet || workerPerformances.isEmpty() || selectionListener == null) return;
        if (selectedWorkerName == null) selectedWorkerName = workerPerformances.get(0).getWorkerName();
        displayWorkerCards();
        selectionListener.onWorkerSelected(selectedWorkerName);
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

    @Override public void onDetach() {
        selectionListener = null;
        super.onDetach();
    }
}
