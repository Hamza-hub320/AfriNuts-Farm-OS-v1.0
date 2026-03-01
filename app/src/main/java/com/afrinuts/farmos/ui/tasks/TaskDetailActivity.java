package com.afrinuts.farmos.ui.tasks;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    private long taskId;
    private TaskEntity task;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;

    // UI Elements
    private TextView tvTaskIcon;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        // Get task ID from intent
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        if (taskId == -1) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Load data
        loadData();
    }

    private void initViews() {
        // Toolbar
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

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadData() {
        new Thread(() -> {
            // Get task
            task = database.taskDao().getTaskById(taskId);

            if (task == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            // Get farm and blocks for context
            currentFarm = database.farmDao().getFirstFarm();
            allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());

            runOnUiThread(() -> {
                displayTaskData();
                loadAssignmentHistory();
            });
        }).start();
    }

    private void displayTaskData() {
        // Header
        tvTaskIcon.setText(task.getType().getIcon());
        tvTaskTitle.setText(task.getTitle());
        tvTaskType.setText(task.getType().getDisplayName());

        // Status
        updateStatusDisplay();

        // Block info
        if (task.getBlockId() != null) {
            String blockName = getBlockName(task.getBlockId());
            tvBlockName.setText("Block " + blockName);
        } else {
            tvBlockName.setText("Farm-Wide");
        }

        // Dates
        tvDueDate.setText(dateFormat.format(new Date(task.getDueDate())));

        if (task.getCompletedDate() != null) {
            layoutCompletedDate.setVisibility(View.VISIBLE);
            tvCompletedDate.setText(dateFormat.format(new Date(task.getCompletedDate())));
        } else {
            layoutCompletedDate.setVisibility(View.GONE);
        }

        // Description
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            tvDescription.setText(task.getDescription());
        } else {
            tvDescription.setText("No description provided");
        }
    }

    private void updateStatusDisplay() {
        tvCurrentStatus.setText(task.getStatus().getIcon() + " " + task.getStatus().getDisplayName());

        // Update status dots
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

        // Set status color
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
        new Thread(() -> {
            List<TaskAssignmentHistoryEntity> assignments =
                    database.taskDao().getAssignmentsForTask(taskId);

            runOnUiThread(() -> {
                assignmentHistoryContainer.removeAllViews();

                if (assignments.isEmpty()) {
                    tvNoAssignments.setVisibility(View.VISIBLE);
                    layoutCurrentAssignment.setVisibility(View.GONE);
                    return;
                }

                tvNoAssignments.setVisibility(View.GONE);

                // Show current assignment (first item, if unassignedAt is null)
                TaskAssignmentHistoryEntity current = assignments.get(0);
                if (current.isActive()) {
                    layoutCurrentAssignment.setVisibility(View.VISIBLE);
                    tvCurrentWorker.setText(current.getAssignedTo());
                    tvAssignedSince.setText("Assigned: " +
                            dateTimeFormat.format(new Date(current.getAssignedAt())));
                } else {
                    layoutCurrentAssignment.setVisibility(View.GONE);
                }

                // Show history (skip first if it's current)
                for (int i = current.isActive() ? 1 : 0; i < assignments.size(); i++) {
                    TaskAssignmentHistoryEntity assignment = assignments.get(i);
                    addAssignmentToHistory(assignment);
                }
            });
        }).start();
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
        TaskStatusUpdateDialog dialog = TaskStatusUpdateDialog.newInstance(
                taskId, task.getTitle(), task.getStatus());
        dialog.setOnStatusUpdatedListener(newStatus -> {
            // Update task status
            task.setStatus(newStatus);

            // If completed, set completion date
            if (newStatus == TaskEntity.TaskStatus.COMPLETED) {
                task.setCompletedDate(System.currentTimeMillis());
            }

            // Save to database
            new Thread(() -> {
                database.taskDao().updateTask(task);
                runOnUiThread(() -> {
                    updateStatusDisplay();
                    displayTaskData(); // Refresh to show completed date if needed
                    Toast.makeText(this, "Status updated to " + newStatus.getDisplayName(),
                            Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
        dialog.show(getSupportFragmentManager(), "TaskStatusUpdateDialog");
    }

    private void showAssignWorkerDialog() {
        AssignWorkerDialog dialog = AssignWorkerDialog.newInstance(taskId);
        dialog.setOnWorkerAssignedListener(() -> {
            loadAssignmentHistory(); // Refresh assignment history
        });
        dialog.show(getSupportFragmentManager(), "AssignWorkerDialog");
    }

    private void enableEditMode() {
        // TODO: Implement edit mode for task details
        Toast.makeText(this, "Edit mode coming soon", Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete \"" + task.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask() {
        Toast.makeText(this, "Deleting task...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            database.taskDao().deleteTask(task);

            runOnUiThread(() -> {
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                finish(); // Go back to tasks list
            });
        }).start();
    }

    private String getBlockName(Long blockId) {
        if (blockId == null) return null;
        for (BlockEntity block : allBlocks) {
            if (block.getId() == blockId) {
                return block.getBlockName();
            }
        }
        return null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}