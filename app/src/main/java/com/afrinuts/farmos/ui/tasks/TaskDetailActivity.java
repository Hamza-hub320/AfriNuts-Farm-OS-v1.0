package com.afrinuts.farmos.ui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.photos.PhotoGalleryActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskDetailActivity extends BaseActivity {

    public static final String EXTRA_TASK_ID = "task_id";
    private static final String TAG = "TaskDetailActivity";

    private long taskId;
    private TaskEntity task;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private TaskRepository taskRepository;
    private AppDatabase database;
    private long farmId = -1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private LinearLayout btnPhotoGallery;
    private TextView tvPhotoCount;
    private ImageView tvTaskIcon;
    private TextView tvTaskTitle;
    private TextView tvTaskType;
    private TextView tvCurrentStatus;
    private TextView tvPendingDot;
    private TextView tvInProgressDot;
    private TextView tvCompletedDot;
    private TextView tvBlockName;
    private TextView tvDueDate;
    private TextView tvCompletedDate;
    private TextView tvDescription;
    private LinearLayout layoutCompletedDate;

    // Assignment UI
    private LinearLayout layoutCurrentAssignment;
    private TextView tvCurrentWorker;
    private TextView tvAssignedSince;
    private LinearLayout assignmentHistoryContainer;
    private TextView tvNoAssignments;
    private MaterialButton btnAssignWorker;

    // Action buttons
    private MaterialButton btnEdit;
    private Button btnDelete;
    private FloatingActionButton fabStatus;
    private MaterialButton btnUpdateStatus;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        if (taskId == -1) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize database
        database = AppDatabase.getInstance(this);

        initViews();
        setupToolbar();

        // Load everything sequentially
        loadFarmAndTask();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Task Details");
        }

        // Header
        tvTaskIcon = findViewById(R.id.tvTaskIcon);
        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        tvTaskType = findViewById(R.id.tvTaskType);

        // Status
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        tvPendingDot = findViewById(R.id.tvPendingDot);
        tvInProgressDot = findViewById(R.id.tvInProgressDot);
        tvCompletedDot = findViewById(R.id.tvCompletedDot);

        // Details
        tvBlockName = findViewById(R.id.tvBlockName);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvCompletedDate = findViewById(R.id.tvCompletedDate);
        layoutCompletedDate = findViewById(R.id.layoutCompletedDate);
        tvDescription = findViewById(R.id.tvDescription);

        // Assignment
        layoutCurrentAssignment = findViewById(R.id.layoutCurrentAssignment);
        tvCurrentWorker = findViewById(R.id.tvCurrentWorker);
        tvAssignedSince = findViewById(R.id.tvAssignedSince);
        assignmentHistoryContainer = findViewById(R.id.assignmentHistoryContainer);
        tvNoAssignments = findViewById(R.id.tvNoAssignments);
        btnAssignWorker = findViewById(R.id.btnAssignWorker);

        // Action buttons
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        fabStatus = findViewById(R.id.fabStatus);

        // Set click listeners
        btnUpdateStatus.setOnClickListener(v -> showStatusUpdateDialog());
        fabStatus.setOnClickListener(v -> showStatusUpdateDialog());
        btnAssignWorker.setOnClickListener(v -> showAssignWorkerDialog());
        btnEdit.setOnClickListener(v -> enableEditMode());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupRoleBasedUI() {
        if (sessionManager.isInvestor()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            fabStatus.setVisibility(View.GONE);
            btnUpdateStatus.setVisibility(View.GONE);
            btnAssignWorker.setVisibility(View.GONE);
        }

        if (sessionManager.isManager()) {
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Task Details");
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

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    private void loadFarmAndTask() {
        executor.execute(() -> {
            // Step 1: Load farm
            FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            farmId = farm.getId();
            currentFarm = farm;

            // Step 2: Load blocks for name mapping
            allBlocks = database.blockDao().getBlocksByFarmId(farmId);

            // Step 3: Initialize repository with correct farmId
            taskRepository = new TaskRepository(TaskDetailActivity.this, farmId);

            // Step 4: Now load the task using LiveData
            runOnUiThread(() -> {
                loadTaskData();
            });
        });
    }

    private void loadTaskData() {
        if (taskRepository == null) {
            Log.e(TAG, "TaskRepository is null, cannot load task");
            Toast.makeText(this, "Error loading task", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Use LiveData from repository
        taskRepository.getTaskById(taskId).observe(this, new Observer<TaskEntity>() {
            @Override
            public void onChanged(TaskEntity loadedTask) {
                if (loadedTask != null) {
                    task = loadedTask;
                    displayTaskData();
                    loadAssignmentHistory();
                    setFieldsEditable(false);

                    if (sessionManager.isInvestor()) {
                        btnEdit.setVisibility(View.GONE);
                        btnDelete.setVisibility(View.GONE);
                        fabStatus.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(TaskDetailActivity.this, "Task not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void displayTaskData() {
        if (task == null) return;

        int iconRes = getTaskIcon(task.getType());
        tvTaskIcon.setImageResource(iconRes);
        tvTaskIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary));

        tvTaskTitle.setText(task.getTitle());
        tvTaskType.setText(task.getType().getDisplayName());

        updateStatusDisplay();

        if (task.getBlockId() != null) {
            String blockName = getBlockName(task.getBlockId());
            tvBlockName.setText("Block " + blockName);
        } else {
            tvBlockName.setText("Farm-Wide");
        }

        tvDueDate.setText(dateFormat.format(new Date(task.getDueDate())));

        if (task.getCompletedDate() != null) {
            layoutCompletedDate.setVisibility(View.VISIBLE);
            tvCompletedDate.setText(dateFormat.format(new Date(task.getCompletedDate())));
        } else {
            layoutCompletedDate.setVisibility(View.GONE);
        }

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            tvDescription.setText(task.getDescription());
        } else {
            tvDescription.setText("No description provided");
        }
    }

    private int getTaskIcon(TaskEntity.TaskType type) {
        switch (type) {
            case CLEARING: return R.drawable.ic_tractor;
            case PLOWING: return R.drawable.ic_grain;
            case PLANTING: return R.drawable.ic_grass;
            case REPLACEMENT: return R.drawable.ic_sync;
            case FERTILIZING: return R.drawable.ic_fertilizer;
            case PRUNING: return R.drawable.ic_cut;
            case WEEDING: return R.drawable.ic_grass;
            case HARVEST: return R.drawable.ic_download;
            case IRRIGATION: return R.drawable.ic_water_drop;
            case MAINTENANCE: return R.drawable.ic_construction;
            case OTHER: return R.drawable.ic_more_horiz;
            default: return R.drawable.ic_more_horiz;
        }
    }

    private void updateStatusDisplay() {
        if (task == null) return;

        tvCurrentStatus.setText(task.getStatus().getDisplayName());

        switch (task.getStatus()) {
            case PENDING:
                tvPendingDot.setBackgroundResource(R.drawable.circle_green);
                tvInProgressDot.setBackgroundResource(R.drawable.circle_grey);
                tvCompletedDot.setBackgroundResource(R.drawable.circle_grey);
                break;
            case IN_PROGRESS:
                tvPendingDot.setBackgroundResource(R.drawable.circle_green);
                tvInProgressDot.setBackgroundResource(R.drawable.circle_blue);
                tvCompletedDot.setBackgroundResource(R.drawable.circle_grey);
                break;
            case COMPLETED:
                tvPendingDot.setBackgroundResource(R.drawable.circle_green);
                tvInProgressDot.setBackgroundResource(R.drawable.circle_blue);
                tvCompletedDot.setBackgroundResource(R.drawable.circle_green);
                break;
            case CANCELLED:
                tvPendingDot.setBackgroundResource(R.drawable.circle_grey);
                tvInProgressDot.setBackgroundResource(R.drawable.circle_grey);
                tvCompletedDot.setBackgroundResource(R.drawable.circle_grey);
                break;
        }

        int statusColor;
        switch (task.getStatus()) {
            case PENDING:
                statusColor = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
                break;
            case IN_PROGRESS:
                statusColor = ContextCompat.getColor(this, android.R.color.holo_blue_dark);
                break;
            case COMPLETED:
                statusColor = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                break;
            case CANCELLED:
                statusColor = ContextCompat.getColor(this, android.R.color.holo_red_dark);
                break;
            default:
                statusColor = ContextCompat.getColor(this, R.color.primary);
        }
        tvCurrentStatus.setTextColor(statusColor);
    }

    private void loadAssignmentHistory() {
        executor.execute(() -> {
            List<TaskAssignmentHistoryEntity> assignments =
                    database.taskDao().getAssignmentsForTask(taskId);

            runOnUiThread(() -> {
                assignmentHistoryContainer.removeAllViews();

                if (assignments == null || assignments.isEmpty()) {
                    tvNoAssignments.setVisibility(View.VISIBLE);
                    layoutCurrentAssignment.setVisibility(View.GONE);
                    return;
                }

                tvNoAssignments.setVisibility(View.GONE);

                TaskAssignmentHistoryEntity current = assignments.get(0);
                if (current != null && current.isActive()) {
                    layoutCurrentAssignment.setVisibility(View.VISIBLE);
                    tvCurrentWorker.setText(current.getAssignedTo());
                    tvAssignedSince.setText("Assigned: " +
                            dateTimeFormat.format(new Date(current.getAssignedAt())));
                } else {
                    layoutCurrentAssignment.setVisibility(View.GONE);
                }

                for (int i = (current != null && current.isActive()) ? 1 : 0; i < assignments.size(); i++) {
                    TaskAssignmentHistoryEntity assignment = assignments.get(i);
                    addAssignmentToHistory(assignment);
                }
            });
        });
    }

    private void addAssignmentToHistory(TaskAssignmentHistoryEntity assignment) {
        View historyView = getLayoutInflater().inflate(
                R.layout.item_assignment_history, assignmentHistoryContainer, false);

        TextView tvHistoryWorker = historyView.findViewById(R.id.tvHistoryWorker);
        TextView tvHistoryDates = historyView.findViewById(R.id.tvHistoryDates);
        TextView tvHistoryDuration = historyView.findViewById(R.id.tvHistoryDuration);

        tvHistoryWorker.setText(assignment.getAssignedTo());

        String startDate = dateFormat.format(new Date(assignment.getAssignedAt()));
        if (assignment.getUnassignedAt() != null) {
            String endDate = dateFormat.format(new Date(assignment.getUnassignedAt()));
            tvHistoryDates.setText(startDate + " → " + endDate);
            tvHistoryDuration.setText(assignment.getDuration());
        } else {
            tvHistoryDates.setText(startDate + " → Present");
            tvHistoryDuration.setText("Active");
        }

        assignmentHistoryContainer.addView(historyView);
    }

    private void showStatusUpdateDialog() {
        if (task == null) return;

        TaskStatusUpdateDialog dialog = TaskStatusUpdateDialog.newInstance(
                taskId, task.getTitle(), task.getStatus());
        dialog.setOnStatusUpdatedListener(newStatus -> {
            task.setStatus(newStatus);

            if (newStatus == TaskEntity.TaskStatus.COMPLETED) {
                task.setCompletedDate(System.currentTimeMillis());
            }

            // Use CompletableFuture pattern for update
            taskRepository.updateTask(task, new BaseRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    runOnUiThread(() -> {
                        updateStatusDisplay();
                        displayTaskData();
                        Toast.makeText(TaskDetailActivity.this,
                                "Status updated to " + newStatus.getDisplayName(),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            Toast.makeText(TaskDetailActivity.this,
                                    "Error updating status: " + error,
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            });
        });
        dialog.show(getSupportFragmentManager(), "TaskStatusUpdateDialog");
    }

    private void showAssignWorkerDialog() {
        if (task == null) return;

        AssignWorkerDialog dialog = AssignWorkerDialog.newInstance(taskId);
        dialog.setOnWorkerAssignedListener(() -> {
            loadAssignmentHistory();
        });
        dialog.show(getSupportFragmentManager(), "AssignWorkerDialog");
    }

    private void enableEditMode() {
        Toast.makeText(this, "Edit mode coming soon", Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete() {
        if (task == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete \"" + task.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask() {
        Toast.makeText(this, "Deleting task...", Toast.LENGTH_SHORT).show();

        taskRepository.deleteTask(task, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(TaskDetailActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(TaskDetailActivity.this,
                                "Error deleting task: " + error,
                                Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private String getBlockName(Long blockId) {
        if (blockId == null || allBlocks == null) return null;
        for (BlockEntity block : allBlocks) {
            if (block.getId() == blockId) {
                return block.getBlockName();
            }
        }
        return null;
    }

    private void setFieldsEditable(boolean editable) {
        // Implement if needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskRepository != null) {
            taskRepository.shutdown();
        }
        executor.shutdown();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}