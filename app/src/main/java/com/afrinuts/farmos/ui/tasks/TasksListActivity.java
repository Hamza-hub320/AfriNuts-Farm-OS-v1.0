package com.afrinuts.farmos.ui.tasks;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TasksListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView pendingTasksValue;
    private TextView inProgressTasksValue;
    private TextView completedTasksValue;
    private ChipGroup filterChipGroup;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<TaskWithBlockName> allTasks = new ArrayList<>();
    private List<TaskWithBlockName> filteredTasks = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();

    private String currentFilter = "ALL"; // ALL, PENDING, IN_PROGRESS, COMPLETED, OVERDUE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_list);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Setup filter listeners
        setupFilters();

        // Setup FAB
        setupFab();

        // Load data
        loadData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        pendingTasksValue = findViewById(R.id.pendingTasksValue);
        inProgressTasksValue = findViewById(R.id.inProgressTasksValue);
        completedTasksValue = findViewById(R.id.completedTasksValue);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Farm Tasks");
        }
    }

    private void setupFilters() {
        Chip chipAll = findViewById(R.id.chipAll);
        Chip chipPending = findViewById(R.id.chipPending);
        Chip chipInProgress = findViewById(R.id.chipInProgress);
        Chip chipCompleted = findViewById(R.id.chipCompleted);
        Chip chipOverdue = findViewById(R.id.chipOverdue);

        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                applyFilter();
            }
        });

        chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "PENDING";
                applyFilter();
            }
        });

        chipInProgress.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "IN_PROGRESS";
                applyFilter();
            }
        });

        chipCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "COMPLETED";
                applyFilter();
            }
        });

        chipOverdue.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "OVERDUE";
                applyFilter();
            }
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddTask);
        fab.setOnClickListener(v -> {
            if (currentFarm != null) {
                AddTaskDialog dialog = AddTaskDialog.newInstance(currentFarm.getId());
                dialog.setOnTaskAddedListener(() -> {
                    loadData(); // Refresh list
                });
                dialog.show(getSupportFragmentManager(), "AddTaskDialog");
            } else {
                android.widget.Toast.makeText(this,
                        "Farm not configured",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadData() {
        showLoading(true);

        new Thread(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                // Load all blocks for name mapping
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                blockNameMap.clear();
                for (BlockEntity block : blocks) {
                    blockNameMap.put(block.getId(), block.getBlockName());
                }

                // Load all tasks
                List<TaskEntity> tasks = database.taskDao().getTasksByFarmId(currentFarm.getId());

                allTasks.clear();
                for (TaskEntity task : tasks) {
                    String blockName = null;
                    if (task.getBlockId() != null) {
                        blockName = blockNameMap.get(task.getBlockId());
                    }
                    allTasks.add(new TaskWithBlockName(task, blockName));
                }

                runOnUiThread(() -> {
                    showLoading(false);

                    if (allTasks.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        updateSummaryCounts();
                        applyFilter();
                    }
                });
            }
        }).start();
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

        // Update adapter
        adapter = new TaskAdapter(filteredTasks, task -> {
            // TODO: Open TaskDetailActivity
            android.widget.Toast.makeText(this,
                    "Task detail coming soon",
                    android.widget.Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tasks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_sort_due_date) {
            sortByDueDate();
            return true;
        } else if (item.getItemId() == R.id.action_sort_title) {
            sortByTitle();
            return true;
        } else if (item.getItemId() == R.id.action_sort_block) {
            sortByBlock();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}