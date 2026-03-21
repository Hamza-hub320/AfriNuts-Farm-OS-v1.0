package com.afrinuts.farmos.ui.tasks;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.TaskRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TasksListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView pendingTasksValue;
    private TextView inProgressTasksValue;
    private TextView completedTasksValue;
    private ChipGroup filterChipGroup;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private TaskRepository taskRepository;
    private long farmId = -1;
    private List<TaskWithBlockName> allTasks = new ArrayList<>();
    private List<TaskWithBlockName> filteredTasks = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String currentFilter = "ALL"; // ALL, PENDING, IN_PROGRESS, COMPLETED, OVERDUE

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_list);

        database = AppDatabase.getInstance(this);

        initViews();
        setupToolbar();
        setupFilters();
        setupFab();
        loadFarmData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyLayout = findViewById(R.id.emptyView);

        // Summary cards - these exist in your layout
        pendingTasksValue = findViewById(R.id.pendingTasksValue);
        inProgressTasksValue = findViewById(R.id.inProgressTasksValue);
        completedTasksValue = findViewById(R.id.completedTasksValue);

        filterChipGroup = findViewById(R.id.filterChipGroup);

        // FAB - this exists in your layout
        FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
            getSupportActionBar().setTitle("Farm Tasks");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_sort_due_date) {
            sortByDueDate();
            return true;
        } else if (itemId == R.id.action_sort_title) {
            sortByTitle();
            return true;
        } else if (itemId == R.id.action_sort_block) {
            sortByBlock();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupFilters() {
        // Get chips from the ChipGroup
        Chip chipAll = findViewById(R.id.chipAll);

        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                applyFilter();
            }
        });

        if (filterChipGroup.getChildCount() >= 5) {
            View pendingView = filterChipGroup.getChildAt(1);
            if (pendingView instanceof Chip) {
                ((Chip) pendingView).setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        currentFilter = "PENDING";
                        applyFilter();
                    }
                });
            }

            // Position 2 should be "In Progress"
            View inProgressView = filterChipGroup.getChildAt(2);
            if (inProgressView instanceof Chip) {
                ((Chip) inProgressView).setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        currentFilter = "IN_PROGRESS";
                        applyFilter();
                    }
                });
            }

            // Position 3 should be "Completed"
            View completedView = filterChipGroup.getChildAt(3);
            if (completedView instanceof Chip) {
                ((Chip) completedView).setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        currentFilter = "COMPLETED";
                        applyFilter();
                    }
                });
            }

            // Position 4 should be "Overdue"
            View overdueView = filterChipGroup.getChildAt(4);
            if (overdueView instanceof Chip) {
                ((Chip) overdueView).setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        currentFilter = "OVERDUE";
                        applyFilter();
                    }
                });
            }
        }
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddTask);

        if (sessionManager.canEditData()) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                if (currentFarm != null) {
                    AddTaskDialog dialog = AddTaskDialog.newInstance(currentFarm.getId());
                    dialog.setOnTaskAddedListener(() -> {
                        loadData();
                    });
                    dialog.show(getSupportFragmentManager(), "AddTaskDialog");
                } else {
                    Toast.makeText(this,
                            "Farm not configured",
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    private void loadFarmData() {
        showLoading(true);

        executor.execute(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                farmId = currentFarm.getId();
                taskRepository = new TaskRepository(TasksListActivity.this, farmId);
                loadData();
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true, "No farm data", "Please check your connection");
                });
            }
        });
    }

    private void loadData() {
        executor.execute(() -> {
            try {
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                Map<Long, String> blockMap = new HashMap<>();
                for (BlockEntity block : blocks) {
                    blockMap.put(block.getId(), block.getBlockName());
                }

                final Map<Long, String> finalBlockMap = blockMap;

                runOnUiThread(() -> {
                    blockNameMap.clear();
                    blockNameMap.putAll(finalBlockMap);

                    // Use LiveData from repository
                    taskRepository.getAllTasks().observe(this, new Observer<List<TaskEntity>>() {
                        @Override
                        public void onChanged(List<TaskEntity> tasks) {
                            if (tasks != null) {
                                processTasks(tasks);
                            } else {
                                showError("No tasks data received");
                            }
                        }
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading data", e);
                runOnUiThread(() -> showError("Failed to load tasks: " + e.getMessage()));
            }
        });
    }

    private void processTasks(List<TaskEntity> tasks) {
        allTasks.clear();
        for (TaskEntity task : tasks) {
            String blockName = null;
            if (task.getBlockId() != null) {
                blockName = blockNameMap.get(task.getBlockId());
            }
            allTasks.add(new TaskWithBlockName(task, blockName));
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
        int pending = 0;
        int inProgress = 0;
        int completed = 0;

        for (TaskWithBlockName item : allTasks) {
            switch (item.getTask().getStatus()) {
                case PENDING:
                    pending++;
                    break;
                case IN_PROGRESS:
                    inProgress++;
                    break;
                case COMPLETED:
                    completed++;
                    break;
            }
        }

        pendingTasksValue.setText(String.valueOf(pending));
        inProgressTasksValue.setText(String.valueOf(inProgress));
        completedTasksValue.setText(String.valueOf(completed));
    }

    private void applyFilter() {
        filteredTasks.clear();
        long currentTime = System.currentTimeMillis();

        for (TaskWithBlockName item : allTasks) {
            TaskEntity task = item.getTask();

            switch (currentFilter) {
                case "ALL":
                    filteredTasks.add(item);
                    break;
                case "PENDING":
                    if (task.getStatus() == TaskEntity.TaskStatus.PENDING) {
                        filteredTasks.add(item);
                    }
                    break;
                case "IN_PROGRESS":
                    if (task.getStatus() == TaskEntity.TaskStatus.IN_PROGRESS) {
                        filteredTasks.add(item);
                    }
                    break;
                case "COMPLETED":
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

        adapter = new TaskAdapter(filteredTasks, task -> {
            Intent intent = new Intent(this, TaskDetailActivity.class);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (emptyLayout != null) {
            emptyLayout.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show, String title, String subtitle) {
        if (emptyLayout != null) {
            emptyLayout.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show) {
                // Find and update the TextViews inside the empty layout
                if (emptyLayout.getChildCount() >= 3) {
                    // The empty layout has ImageView, then two TextViews
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

        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        showLoading(false);
        showEmpty(true, "Error", message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tasks, menu);
        return true;
    }

    private void sortByDueDate() {
        filteredTasks.sort((t1, t2) ->
                Long.compare(t1.getTask().getDueDate(), t2.getTask().getDueDate()));
        adapter.notifyDataSetChanged();
    }

    private void sortByTitle() {
        filteredTasks.sort((t1, t2) ->
                t1.getTask().getTitle().compareToIgnoreCase(t2.getTask().getTitle()));
        adapter.notifyDataSetChanged();
    }

    private void sortByBlock() {
        filteredTasks.sort((t1, t2) -> {
            String block1 = t1.getBlockName() != null ? t1.getBlockName() : "AAAA";
            String block2 = t2.getBlockName() != null ? t2.getBlockName() : "AAAA";
            return block1.compareToIgnoreCase(block2);
        });
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentFarm != null) {
            loadData();
        } else {
            loadFarmData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskRepository != null) {
            taskRepository.shutdown();
        }
        executor.shutdown();
    }
}