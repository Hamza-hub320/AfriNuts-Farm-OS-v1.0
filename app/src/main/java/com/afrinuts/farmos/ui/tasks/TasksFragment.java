package com.afrinuts.farmos.ui.tasks;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.TaskRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TasksFragment extends Fragment {

    private static final String TAG = "TasksFragment";

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView pendingTasksValue;
    private TextView inProgressTasksValue;
    private TextView completedTasksValue;
    private ChipGroup filterChipGroup;
    private TextInputEditText searchInput;
    private String searchQuery = "";

    private AppDatabase database;
    private FarmEntity currentFarm;
    private TaskRepository taskRepository;
    private long farmId = -1;
    private List<TaskWithBlockName> allTasks = new ArrayList<>();
    private List<TaskWithBlockName> filteredTasks = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String currentFilter = "ALL"; // ALL, PENDING, IN_PROGRESS, COMPLETED, OVERDUE

    // ✅ Flag to track if observer is already registered
    private boolean isObserverRegistered = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        database = AppDatabase.getInstance(requireContext());
        initViews(view);
        loadFarmData();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyLayout = view.findViewById(R.id.emptyView);

        pendingTasksValue = view.findViewById(R.id.pendingTasksValue);
        inProgressTasksValue = view.findViewById(R.id.inProgressTasksValue);
        completedTasksValue = view.findViewById(R.id.completedTasksValue);

        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        searchInput = view.findViewById(R.id.etTaskSearch);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                applyFilter();
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        setupFilters();
    }

    private void setupFilters() {
        com.afrinuts.farmos.ui.farm.FarmTabChips.style(filterChipGroup);
        // Get chips from the ChipGroup
        Chip chipAll = filterChipGroup.findViewById(R.id.chipAll);

        if (chipAll != null) {
            chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "ALL";
                    applyFilter();
                }
            });
        }

        Chip chipPending = filterChipGroup.findViewById(R.id.chipPending);
        if (chipPending != null) {
            chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "TODAY";
                    applyFilter();
                }
            });
        }

        Chip chipInProgress = filterChipGroup.findViewById(R.id.chipInProgress);
        if (chipInProgress != null) {
            chipInProgress.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "IN_PROGRESS";
                    applyFilter();
                }
            });
        }

        Chip chipCompleted = filterChipGroup.findViewById(R.id.chipCompleted);
        if (chipCompleted != null) {
            chipCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "DONE";
                    applyFilter();
                }
            });
        }

        Chip chipOverdue = filterChipGroup.findViewById(R.id.chipOverdue);
        if (chipOverdue != null) {
            chipOverdue.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "OVERDUE";
                    applyFilter();
                }
            });
        }
    }

    private void loadFarmData() {
        showLoading(true);

        executor.execute(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                farmId = currentFarm.getId();
                taskRepository = new TaskRepository(requireContext(), farmId);
                loadTasks();
            } else {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true, "No farm data", "Please check your connection");
                });
            }
        });
    }

    private void loadTasks() {
        executor.execute(() -> {
            try {
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                Map<Long, String> blockMap = new HashMap<>();
                for (BlockEntity block : blocks) {
                    blockMap.put(block.getId(), block.getBlockName());
                }

                final Map<Long, String> finalBlockMap = blockMap;

                requireActivity().runOnUiThread(() -> {
                    blockNameMap.clear();
                    blockNameMap.putAll(finalBlockMap);

                    // Use LiveData from repository
                    taskRepository.getAllTasks().observe(getViewLifecycleOwner(), new Observer<List<TaskEntity>>() {
                        @Override
                        public void onChanged(List<TaskEntity> tasks) {
                            if (tasks != null) {
                                List<TaskEntity> snapshot = new ArrayList<>(tasks);
                                executor.execute(() -> {
                                    Map<Long, String> assignedNames = new HashMap<>();
                                    for (TaskEntity task : snapshot) {
                                        com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity assignment =
                                                database.taskAssignmentDao().getCurrentAssignment(task.getId());
                                        if (assignment != null) assignedNames.put(task.getId(), assignment.getAssignedTo());
                                    }
                                    if (getActivity() != null) getActivity().runOnUiThread(() -> processTasks(snapshot, assignedNames));
                                });
                            } else {
                                showError("No tasks data received");
                            }
                        }
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading tasks", e);
                requireActivity().runOnUiThread(() -> showError("Failed to load tasks: " + e.getMessage()));
            }
        });
    }

    private void processTasks(List<TaskEntity> tasks, Map<Long, String> assignedNames) {
        allTasks.clear();
        for (TaskEntity task : tasks) {
            String blockName = null;
            if (task.getBlockId() != null) {
                blockName = blockNameMap.get(task.getBlockId());
            }
            allTasks.add(new TaskWithBlockName(task, blockName, assignedNames.get(task.getId())));
        }

        showLoading(false);

        if (allTasks.isEmpty()) {
            showEmpty(true, "No tasks yet", "Tap + to create your first task");
        } else {
            showEmpty(false, null, null);
            updateSummaryCounts();
            applyFilter();
        }
    }

    private void updateSummaryCounts() {
        int open = 0;
        int completedThisWeek = 0;
        java.util.Calendar monday = java.util.Calendar.getInstance();
        monday.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
        monday.set(java.util.Calendar.HOUR_OF_DAY, 0);
        monday.set(java.util.Calendar.MINUTE, 0);
        monday.set(java.util.Calendar.SECOND, 0);
        monday.set(java.util.Calendar.MILLISECOND, 0);
        long weekStart = monday.getTimeInMillis();
        java.util.Calendar friday = (java.util.Calendar) monday.clone();
        friday.add(java.util.Calendar.DAY_OF_MONTH, 5);
        long weekEnd = friday.getTimeInMillis();

        for (TaskWithBlockName item : allTasks) {
            TaskEntity task = item.getTask();
            if (task.getStatus() == TaskEntity.TaskStatus.PENDING || task.getStatus() == TaskEntity.TaskStatus.IN_PROGRESS) open++;
            if (task.getStatus() == TaskEntity.TaskStatus.COMPLETED) {
                long completedAt = task.getCompletedDate() == null ? task.getUpdatedAt() : task.getCompletedDate();
                if (completedAt >= weekStart && completedAt < weekEnd) completedThisWeek++;
            }
        }

        if (pendingTasksValue != null) pendingTasksValue.setText(String.valueOf(open));
        if (inProgressTasksValue != null) inProgressTasksValue.setText("0");
        if (completedTasksValue != null) completedTasksValue.setText(String.valueOf(completedThisWeek));
    }

    private void applyFilter() {
        filteredTasks.clear();
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);
        long todayStart = today.getTimeInMillis();
        long tomorrow = todayStart + 24L * 60 * 60 * 1000;

        for (TaskWithBlockName item : allTasks) {
            TaskEntity task = item.getTask();

            switch (currentFilter) {
                case "ALL":
                    filteredTasks.add(item);
                    break;
                case "TODAY":
                    if (task.getStatus() != TaskEntity.TaskStatus.COMPLETED
                            && task.getDueDate() >= todayStart && task.getDueDate() < tomorrow) {
                        filteredTasks.add(item);
                    }
                    break;
                case "IN_PROGRESS":
                    if (task.getStatus() == TaskEntity.TaskStatus.IN_PROGRESS) {
                        filteredTasks.add(item);
                    }
                    break;
                case "COMPLETED":
                case "DONE":
                    if (task.getStatus() == TaskEntity.TaskStatus.COMPLETED) {
                        filteredTasks.add(item);
                    }
                    break;
                case "OVERDUE":
                    if (task.isOverdue()) {
                        filteredTasks.add(item);
                    }
                    break;
            }
        }

        if (!searchQuery.isEmpty()) {
            filteredTasks.removeIf(item -> {
                String title = item.getTask().getTitle() == null ? "" : item.getTask().getTitle().toLowerCase(Locale.ROOT);
                String scope = item.getScopeDisplay().toLowerCase(Locale.ROOT);
                return !title.contains(searchQuery) && !scope.contains(searchQuery);
            });
        }
        filteredTasks.sort((a, b) -> Boolean.compare(!a.isOverdue(), !b.isOverdue()));

        adapter = new TaskAdapter(filteredTasks, task -> {
            Intent intent = new Intent(getContext(), TaskDetailActivity.class);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    public void showAddTaskDialog() {
        if (currentFarm != null && getView() != null) {
            AddTaskDialog dialog = AddTaskDialog.newInstance(currentFarm.getId());
            dialog.setOnTaskAddedListener(() -> {
                loadFarmData();
            });
            dialog.show(getParentFragmentManager(), "AddTaskDialog");
        }
    }

    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
            });
        }
    }

    private void showEmpty(boolean show, String title, String subtitle) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (emptyLayout != null) {
                    emptyLayout.setVisibility(show ? View.VISIBLE : View.GONE);

                    if (show) {
                        // Find and update the TextViews inside the empty layout
                        if (emptyLayout.getChildCount() >= 3) {
                            View titleView = emptyLayout.getChildAt(1);
                            View subtitleView = emptyLayout.getChildAt(2);

                            if (titleView instanceof TextView && title != null) {
                                ((TextView) titleView).setText(title);
                            }

                            if (subtitleView instanceof TextView && subtitle != null) {
                                ((TextView) subtitleView).setText(subtitle);
                            }
                        }
                    }
                }

                if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            });
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
        showLoading(false);
        showEmpty(true, "Error", message);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentFarm != null) {
            loadTasks();
        } else {
            loadFarmData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (taskRepository != null) {
            taskRepository.shutdown();
        }
        executor.shutdown();
    }
}
