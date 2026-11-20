package com.afrinuts.farmos.ui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
import com.afrinuts.farmos.data.repository.TaskAssignmentRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.photos.PhotoGalleryActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.app.DatePickerDialog;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import java.util.Calendar;
import java.util.Objects;

public class TaskDetailActivity extends BaseActivity {

    public static final String EXTRA_TASK_ID = "task_id";
    private static final String TAG = "TaskDetailActivity";

    private long taskId;
    private TaskEntity task;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private TaskRepository taskRepository;
    private TaskAssignmentRepository assignmentRepository;
    private PhotoRepository photoRepository;
    private AppDatabase database;
    private long farmId = -1;
    private boolean isDataLoaded = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements - Photo Gallery
    private LinearLayout btnPhotoGallery;
    private TextView tvPhotoCount;

    // Existing UI Elements
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
    private View layoutStartDate;
    private TextView tvStartDate;

    // Assignment UI
    private LinearLayout layoutCurrentAssignment;
    private TextView tvCurrentWorker;
    private TextView tvAssignedSince;
    private LinearLayout assignmentHistoryContainer;
    private TextView tvNoAssignments;
    private MaterialButton btnAssignWorker;

    private boolean isEditMode = false;
    private MaterialButton btnSave;

    // Action buttons
    private MaterialButton btnEdit;
    private Button btnDelete;
    private FloatingActionButton fabStatus;
    private MaterialButton btnUpdateStatus;

    private android.widget.EditText etTitle;
    private android.widget.EditText etDescription;
    private com.google.android.material.textfield.TextInputLayout layoutTaskType;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView actvTaskType;
    private com.google.android.material.textfield.TextInputLayout layoutBlock;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView actvBlock;

    private long editDueDate;
    private Long editStartDate;                  // nullable: pre-42 task may have none
    private Long editCompletedDate;              // nullable: only when task is COMPLETED

    // 42 pre-edit snapshot for the §4 "actually changed" guard
    private Long preEditStartDate;
    private Long preEditCompletedDate;

    // 42 edit-mode type / block selection
    private TaskEntity.TaskType editSelectedType;
    private Long editSelectedBlockId;            // null = Farm-Wide

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
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

        // Initialize database and views first
        database = AppDatabase.getInstance(this);
        initViews();
        setupToolbar();

        // Load data in background
        loadData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Task Details");
        }

        // Photo Gallery
        btnPhotoGallery = findViewById(R.id.btnPhotoGallery);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

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

        layoutStartDate = findViewById(R.id.layoutStartDate);
        tvStartDate     = findViewById(R.id.tvStartDate);

        // Action buttons
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        fabStatus = findViewById(R.id.fabStatus);

        // Set click listeners
        btnUpdateStatus.setOnClickListener(v -> showStatusUpdateDialog());
        fabStatus.setOnClickListener(v -> showStatusUpdateDialog());
        btnAssignWorker.setOnClickListener(v -> showAssignWorkerDialog());
        btnEdit.setOnClickListener(v -> enableEditMode(true));
        btnDelete.setOnClickListener(v -> confirmDelete());

        // 42 edit mode: input siblings (invisible until enableEditMode)
        etTitle            = findViewById(R.id.etTitle);
        etDescription      = findViewById(R.id.etDescription);
        layoutTaskType     = findViewById(R.id.layoutTaskType);
        actvTaskType       = findViewById(R.id.actvTaskType);
        layoutBlock        = findViewById(R.id.layoutBlock);
        actvBlock          = findViewById(R.id.actvBlock);
        btnSave            = findViewById(R.id.btnSave);

        // 42: date rows become tappable only in edit mode — the listener
        // checks isEditMode and returns early otherwise, so the view-mode
        // behaviour is unchanged (non-tappable rows).
        View layoutDueDate = findViewById(R.id.layoutDueDate);
        if (layoutDueDate != null) {
            layoutDueDate.setOnClickListener(v -> {
                if (isEditMode) showDueDatePicker();
            });
        }
        if (layoutCompletedDate != null) {
            layoutCompletedDate.setOnClickListener(v -> {
                if (isEditMode) showCompletedDatePicker();
            });
        }
        if (layoutStartDate != null) {
            layoutStartDate.setOnClickListener(v -> {
                if (isEditMode) showStartDatePicker();
            });
        }

        btnSave.setOnClickListener(v -> saveChanges());

        // Photo gallery click listener
        btnPhotoGallery.setOnClickListener(v -> {
            Intent intent = new Intent(this, PhotoGalleryActivity.class);
            intent.putExtra("task_id", taskId);
            startActivity(intent);
        });
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

    private void loadData() {
        executor.execute(() -> {
            // Load farm
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

            // Load blocks for name mapping
            allBlocks = database.blockDao().getBlocksByFarmId(farmId);

            // Initialize repositories AFTER we have farmId
            taskRepository = new TaskRepository(TaskDetailActivity.this, farmId);
            assignmentRepository = new TaskAssignmentRepository(TaskDetailActivity.this, farmId);
            photoRepository = new PhotoRepository(TaskDetailActivity.this, farmId);

            // Now load the task data on main thread
            runOnUiThread(() -> {
                if (!isDataLoaded) {
                    loadTaskData();
                    isDataLoaded = true;
                }
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
                    loadPhotoCount();
                    setFieldsEditable(false);
                    setupRoleBasedUI();

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

    // ... rest of your methods (displayTaskData, getTaskIcon, updateStatusDisplay, etc.) remain the same ...

    private void loadPhotoCount() {
        if (photoRepository == null) return;

        photoRepository.getPhotoCountForTaskAsync(taskId)
                .thenAccept(count -> {
                    runOnUiThread(() -> {
                        tvPhotoCount.setText(count + " " + (count == 1 ? "photo" : "photos"));
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error loading photo count: " + throwable.getMessage());
                    runOnUiThread(() -> {
                        tvPhotoCount.setText("0 photos");
                    });
                    return null;
                });
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

            if (task.getCompletedDate() == null) {
                task.setCompletedDate(System.currentTimeMillis());
            }

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

    private void completeCurrentAssignment() {
        if (assignmentRepository == null) {
            Log.e(TAG, "AssignmentRepository is null, cannot complete assignment");
            return;
        }

        executor.execute(() -> {
            TaskAssignmentHistoryEntity currentAssignment =
                    database.taskAssignmentDao().getCurrentAssignment(taskId);

            if (currentAssignment != null && currentAssignment.isActive()) {
                currentAssignment.completeAssignment();
                database.taskAssignmentDao().update(currentAssignment);

                Log.d(TAG, "Completed assignment for task " + taskId +
                        " assigned to " + currentAssignment.getAssignedTo());

                assignmentRepository.updateAssignment(currentAssignment,
                        new BaseRepository.RepositoryCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                Log.d(TAG, "Assignment sync completed for task: " + taskId);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error syncing assignment completion: " + error);
                            }
                        });
            } else {
                Log.d(TAG, "No active assignment found for task: " + taskId);
            }
        });
    }

    private void showAssignWorkerDialog() {
        if (task == null) return;

        AssignWorkerDialog dialog = AssignWorkerDialog.newInstance(taskId);
        dialog.setOnWorkerAssignedListener(() -> {
            loadAssignmentHistory();
        });
        dialog.show(getSupportFragmentManager(), "AssignWorkerDialog");
    }

    // ============================================================
    // 42: TASK EDIT MODE
    // ============================================================

    /**
     * Enter or exit edit mode. Entering snapshots the pre-edit dates (for
     * the §4 'actually changed' guard), populates the inputs from the task,
     * and swaps the action buttons. Exiting without saving discards —
     * the inputs are UI-only until saveChanges().
     */
    private void enableEditMode(boolean enable) {
        if (task == null) return;

        if (enable && !sessionManager.canEditData()) {
            com.google.android.material.snackbar.Snackbar.make(
                    findViewById(android.R.id.content),
                    "You don't have permission to edit",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            return;
        }

        isEditMode = enable;

        if (enable) {
            // Snapshot pre-edit dates for the §4 guard
            preEditStartDate = task.getStartDate();
            preEditCompletedDate = task.getCompletedDate();

            // Seed editable state from the task
            editDueDate = task.getDueDate();
            editStartDate = task.getStartDate();
            editCompletedDate = task.getCompletedDate();
            editSelectedType = task.getType();
            editSelectedBlockId = task.getBlockId();

            // Populate inputs
            etTitle.setText(task.getTitle());
            etDescription.setText(task.getDescription() != null ? task.getDescription() : "");
            setupTypeDropdown();
            setupBlockDropdown();
            if (task.getType() != null) {
                actvTaskType.setText(task.getType().getDisplayName(), false);
            }
            if (task.getBlockId() != null) {
                String blockName = getBlockName(task.getBlockId());
                actvBlock.setText(blockName, false);
            } else {
                actvBlock.setText("Farm-Wide", false);
            }

            // §3: start row is always visible in edit mode (so a pre-42 task
            // can be given a start date). Completed row only when COMPLETED.
            layoutStartDate.setVisibility(View.VISIBLE);
            tvStartDate.setText(editStartDate != null
                    ? dateFormat.format(new Date(editStartDate))
                    : "Not set");
            layoutCompletedDate.setVisibility(
                    task.getStatus() == TaskEntity.TaskStatus.COMPLETED
                            ? View.VISIBLE : View.GONE);
            if (editCompletedDate != null) {
                tvCompletedDate.setText(dateFormat.format(new Date(editCompletedDate)));
            }

            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
        } else {
            btnSave.setVisibility(View.GONE);
            btnEdit.setVisibility(sessionManager.isInvestor() ? View.GONE : View.VISIBLE);
            displayTaskData();
        }

        setFieldsEditable(enable);
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

        // 42: start date. Only present on backdated tasks.
        if (task.getStartDate() != null && task.getStartDate() > 0) {
            layoutStartDate.setVisibility(View.VISIBLE);
            tvStartDate.setText(dateFormat.format(new Date(task.getStartDate())));
        } else {
            layoutStartDate.setVisibility(View.GONE);
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

    private String getBlockName(Long blockId) {
        if (blockId == null || allBlocks == null) return null;
        for (BlockEntity block : allBlocks) {
            if (block.getId() == blockId) {
                return block.getBlockName();
            }
        }
        return null;
    }

    /**
     * Toggle visibility of the TextView / input pairs. In edit mode the
     * input shows and the display TextView hides; in view mode the reverse.
     * Dates use the tappable-row pattern (no separate input), so they are
     * not toggled here — the row's click listener guards on isEditMode.
     */
    private void setFieldsEditable(boolean editable) {
        // Title
        tvTaskTitle.setVisibility(editable ? View.GONE : View.VISIBLE);
        etTitle.setVisibility(editable ? View.VISIBLE : View.GONE);

        // Description
        tvDescription.setVisibility(editable ? View.GONE : View.VISIBLE);
        etDescription.setVisibility(editable ? View.VISIBLE : View.GONE);

        // Type
        tvTaskType.setVisibility(editable ? View.GONE : View.VISIBLE);
        layoutTaskType.setVisibility(editable ? View.VISIBLE : View.GONE);

        // Block
        tvBlockName.setVisibility(editable ? View.GONE : View.VISIBLE);
        layoutBlock.setVisibility(editable ? View.VISIBLE : View.GONE);
    }

    // ---------- Date pickers (edit mode) ----------

    private void showDueDatePicker() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(editDueDate);
        new android.app.DatePickerDialog(this,
                (view, y, m, d) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(y, m, d);
                    editDueDate = picked.getTimeInMillis();
                    tvDueDate.setText(dateFormat.format(new Date(editDueDate)));
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showCompletedDatePicker() {
        if (task.getStatus() != TaskEntity.TaskStatus.COMPLETED) return;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(editCompletedDate != null
                ? editCompletedDate : System.currentTimeMillis());
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(this,
                (view, y, m, d) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(y, m, d);
                    editCompletedDate = picked.getTimeInMillis();
                    tvCompletedDate.setText(dateFormat.format(new Date(editCompletedDate)));
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dp.getDatePicker().setMaxDate(System.currentTimeMillis());
        dp.show();
    }

    private void showStartDatePicker() {
        Calendar c = Calendar.getInstance();
        long seed = editStartDate != null ? editStartDate
                : (editCompletedDate != null ? editCompletedDate : System.currentTimeMillis());
        c.setTimeInMillis(seed);
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(this,
                (view, y, m, d) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(y, m, d);
                    editStartDate = picked.getTimeInMillis();
                    tvStartDate.setText(dateFormat.format(new Date(editStartDate)));
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dp.getDatePicker().setMaxDate(System.currentTimeMillis());
        dp.show();
    }

    // ---------- Type / block dropdowns (edit mode) ----------

    private void setupTypeDropdown() {
        TaskEntity.TaskType[] types = TaskEntity.TaskType.values();
        String[] labels = new String[types.length];
        for (int i = 0; i < types.length; i++) labels[i] = types[i].getDisplayName();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, labels);
        actvTaskType.setAdapter(adapter);
        actvTaskType.setOnItemClickListener((p, v, pos, id) -> {
            editSelectedType = types[pos];
            actvTaskType.setText(types[pos].getDisplayName(), false);
        });
    }

    private void setupBlockDropdown() {
        List<String> labels = new ArrayList<>();
        labels.add("Farm-Wide");
        if (allBlocks != null) {
            for (BlockEntity b : allBlocks) labels.add(b.getBlockName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, labels);
        actvBlock.setAdapter(adapter);
        actvBlock.setOnItemClickListener((p, v, pos, id) -> {
            if (pos == 0) {
                editSelectedBlockId = null;
                actvBlock.setText("Farm-Wide", false);
            } else {
                editSelectedBlockId = allBlocks.get(pos - 1).getId();
                actvBlock.setText(allBlocks.get(pos - 1).getBlockName(), false);
            }
        });
    }

    // ---------- Save ----------

    private void saveChanges() {
        if (task == null || taskRepository == null) return;

        // §3: validate — title, start ≤ completed, completed ≤ today, start ≤ today
        String newTitle = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        if (newTitle.isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }
        long now = System.currentTimeMillis();
        if (editStartDate != null && editStartDate > now) {
            Toast.makeText(this, "Start date cannot be in the future", Toast.LENGTH_LONG).show();
            return;
        }
        if (editCompletedDate != null && editCompletedDate > now) {
            Toast.makeText(this, "Completed date cannot be in the future", Toast.LENGTH_LONG).show();
            return;
        }
        if (editStartDate != null && editCompletedDate != null
                && editStartDate > editCompletedDate) {
            Toast.makeText(this, "Start date must be on or before the completed date", Toast.LENGTH_LONG).show();
            return;
        }

        // Apply to entity
        task.setTitle(newTitle);
        String desc = etDescription.getText() == null ? "" : etDescription.getText().toString().trim();
        task.setDescription(desc.isEmpty() ? null : desc);
        if (editSelectedType != null) task.setType(editSelectedType);
        task.setBlockId(editSelectedBlockId);
        task.setDueDate(editDueDate);
        task.setStartDate(editStartDate);
        task.setCompletedDate(editCompletedDate);
        task.setUpdatedAt(System.currentTimeMillis());

        // §4: capture whether the work window actually changed
        boolean startChanged = !java.util.Objects.equals(preEditStartDate, editStartDate);
        boolean completedChanged = !java.util.Objects.equals(preEditCompletedDate, editCompletedDate);
        boolean windowChanged = startChanged || completedChanged;
        boolean shouldReDerive = windowChanged
                && task.getStatus() == TaskEntity.TaskStatus.COMPLETED
                && editCompletedDate != null;

        btnSave.setEnabled(false);
        btnSave.setText("Saving…");

        taskRepository.updateTask(task, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                // §4: re-derive assignments inside the success callback —
                // task is now committed, assignments are derived.
                if (shouldReDerive) {
                    reDeriveAssignments(editStartDate, editCompletedDate);
                }
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    enableEditMode(false);
                    Toast.makeText(TaskDetailActivity.this, "Changes saved", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    Toast.makeText(TaskDetailActivity.this,
                            "Error saving: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 42 §4: when the task's window changed, every closed assignment of this
     * task gets the new window. Mirrors completeCurrentAssignment()'s pattern
     * — DAO update, then updateAssignment() (which queues the sync), so a
     * failure on one assignment is a logged follow-up, not a rollback.
     *
     * Active assignments (unassignedAt == null) are not touched: on a
     * COMPLETED task there should be none, and if one exists the next
     * re-assign closes it at the new window (d891811).
     */
    private void reDeriveAssignments(Long newStart, Long newCompleted) {
        if (newCompleted == null || assignmentRepository == null) return;
        long assigned = newStart != null ? newStart : newCompleted;

        executor.execute(() -> {
            List<TaskAssignmentHistoryEntity> all =
                    database.taskDao().getAssignmentsForTask(taskId);
            for (TaskAssignmentHistoryEntity a : all) {
                if (a.getUnassignedAt() == null) continue;   // active — skip
                a.setAssignedAt(assigned);
                a.setUnassignedAt(newCompleted);
                database.taskAssignmentDao().update(a);
                assignmentRepository.updateAssignment(a,
                        new BaseRepository.RepositoryCallback<Boolean>() {
                            @Override public void onSuccess(Boolean r) {
                                Log.d(TAG, "Re-derived assignment " + a.getId());
                            }
                            @Override public void onError(String e) {
                                Log.e(TAG, "Re-derive failed for " + a.getId() + ": " + e);
                            }
                        });
            }
            runOnUiThread(this::loadAssignmentHistory);
        });
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
        if (isEditMode) {
            enableEditMode(false);   // discard pending inputs, redisplay
            return;
        }
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskRepository != null) {
            taskRepository.shutdown();
        }
        if (photoRepository != null) {
            photoRepository.shutdown();
        }
        if (assignmentRepository != null) {
            assignmentRepository.shutdown();
        }
        executor.shutdown();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}