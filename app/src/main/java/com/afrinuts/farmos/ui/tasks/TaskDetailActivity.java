package com.afrinuts.farmos.ui.tasks;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.data.repository.TaskAssignmentRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.photos.PhotoGalleryActivity;
import com.afrinuts.farmos.ui.photos.PhotoDetailActivity;
import com.afrinuts.farmos.ui.compliance.PhotoSourceDialog;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.app.DatePickerDialog;
import android.widget.EditText;
import java.util.Objects;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public class TaskDetailActivity extends BaseActivity {

    public static final String EXTRA_TASK_ID = "task_id";
    private static final String TAG = "TaskDetailActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 7401;
    private static final int REQUEST_IMAGE_PICK = 7402;
    private static final int REQUEST_CAMERA_PERMISSION = 7403;

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
    private MaterialButton btnAddPhoto;
    private HorizontalScrollView photoThumbnailScroller;
    private LinearLayout photoThumbnailStrip;
    private String currentPhotoPath;

    // Existing UI Elements
    private ImageView tvTaskIcon;
    private TextView tvTaskTitle;
    private TextView tvTaskType;
    private TextView tvCurrentStatus;
    private TextView tvHeroStatus;
    private TextView statStatusValue;
    private TextView statDueDateValue;
    private TextView statAssignedWorkerValue;
    private TextView tvWorkerInitials;
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
    private MaterialButton btnUpdateStatusBottom;

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Task Details");
        }

        // Photo Gallery
        btnPhotoGallery = findViewById(R.id.btnPhotoGallery);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        photoThumbnailScroller = findViewById(R.id.photoThumbnailScroller);
        photoThumbnailStrip = findViewById(R.id.photoThumbnailStrip);

        // Header
        tvTaskIcon = findViewById(R.id.tvTaskIcon);
        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        tvTaskType = findViewById(R.id.tvTaskType);
        tvHeroStatus = findViewById(R.id.tvHeroStatus);
        statStatusValue = findViewById(R.id.statStatusValue);
        statDueDateValue = findViewById(R.id.statDueDateValue);
        statAssignedWorkerValue = findViewById(R.id.statAssignedWorkerValue);

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
        tvWorkerInitials = findViewById(R.id.tvWorkerInitials);
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
        btnUpdateStatusBottom = findViewById(R.id.btnUpdateStatusBottom);

        // Set click listeners
        btnUpdateStatus.setOnClickListener(v -> showStatusUpdateDialog());
        btnUpdateStatusBottom.setOnClickListener(v -> showStatusUpdateDialog());
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
        styleTaskBottomAction(btnAssignWorker);
        styleTaskBottomAction(btnUpdateStatusBottom);
        styleTaskBottomAction(btnSave);
        if (btnDelete instanceof MaterialButton) {
            styleTaskBottomAction((MaterialButton) btnDelete);
        }

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
        btnAddPhoto.setOnClickListener(v -> showPhotoSourceDialog());
    }

    private void styleTaskBottomAction(MaterialButton button) {
        if (button == null) return;
        button.setElevation(0f);
        button.setStateListAnimator(null);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setCornerRadius(dp(999));
    }

    private void setupToolbar() {
        TextView title = findViewById(R.id.farmDetailTitle);
        View back = findViewById(R.id.farmDetailBack);
        View edit = findViewById(R.id.farmDetailEdit);
        View more = findViewById(R.id.farmDetailMore);
        title.setText("Task details");
        back.setOnClickListener(v -> onBackPressed());
        edit.setVisibility(View.GONE);
        btnEdit.setVisibility(View.GONE);
        more.setVisibility(View.VISIBLE);
        more.setOnClickListener(anchor -> {
            androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(this, anchor);
            if (sessionManager.canEditData() && !isEditMode) {
                menu.getMenu().add("Edit").setOnMenuItemClickListener(item -> {
                    enableEditMode(true);
                    return true;
                });
            }
            if (isEditMode) {
                menu.getMenu().add("Cancel editing").setOnMenuItemClickListener(item -> {
                    enableEditMode(false);
                    return true;
                });
            }
            menu.show();
        });
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
            btnUpdateStatusBottom.setVisibility(View.GONE);
            btnAssignWorker.setVisibility(View.GONE);
        }

        if (sessionManager.isManager()) {
            btnDelete.setVisibility(View.GONE);
        }
    }

    // ... rest of your methods (displayTaskData, getTaskIcon, updateStatusDisplay, etc.) remain the same ...

    private void loadPhotoCount() {
        if (photoRepository == null) return;
        photoRepository.getPhotosForTask(taskId).observe(this, this::displayTaskPhotos);
    }

    private void displayTaskPhotos(List<PhotoEntity> photos) {
        photoThumbnailStrip.removeAllViews();
        int count = photos == null ? 0 : photos.size();
        tvPhotoCount.setText(count + " " + (count == 1 ? "photo" : "photos"));
        photoThumbnailScroller.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        if (count == 0) return;

        int size = dp(88);
        for (PhotoEntity photo : photos) {
            ImageView thumbnail = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(dp(10));
            thumbnail.setLayoutParams(params);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setContentDescription("Task photo");
            GradientDrawable background = new GradientDrawable();
            background.setColor(0xFFF3EAD8);
            background.setCornerRadius(dp(14));
            background.setStroke(dp(1), 0xFFE9E1CF);
            thumbnail.setBackground(background);
            thumbnail.setClipToOutline(true);
            String path = photo.getThumbnailPath();
            if (path == null || path.trim().isEmpty()) path = photo.getFilePath();
            Glide.with(this).load(path).placeholder(R.drawable.ic_photo_placeholder)
                    .error(R.drawable.ic_photo_placeholder).centerCrop().into(thumbnail);
            thumbnail.setOnClickListener(v -> {
                Intent intent = new Intent(this, PhotoDetailActivity.class);
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, photo.getId());
                startActivity(intent);
            });
            photoThumbnailStrip.addView(thumbnail);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showPhotoSourceDialog() {
        if (photoRepository == null) {
            Toast.makeText(this, "Task is still loading", Toast.LENGTH_SHORT).show();
            return;
        }
        PhotoSourceDialog dialog = new PhotoSourceDialog();
        dialog.setListener(source -> {
            if (source == PhotoSourceDialog.SOURCE_CAMERA) dispatchTakePictureIntent();
            else dispatchPickPictureIntent();
        });
        dialog.show(getSupportFragmentManager(), "TaskPhotoSourceDialog");
    }

    private void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (capture.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File image = createTaskPhotoFile();
            Uri uri = FileProvider.getUriForFile(this,
                    "com.afrinuts.farmos.fileprovider", image);
            capture.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            capture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(capture, REQUEST_IMAGE_CAPTURE);
        } catch (IOException error) {
            Log.e(TAG, "Unable to create task photo file", error);
            Toast.makeText(this, "Could not create photo file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createTaskPhotoFile() throws IOException {
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory == null) throw new IOException("Photo storage is unavailable");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create photo folder");
        }
        File file = File.createTempFile("TASK_" + System.currentTimeMillis() + "_", ".jpg", directory);
        currentPhotoPath = file.getAbsolutePath();
        return file;
    }

    private void dispatchPickPictureIntent() {
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("image/*");
        startActivityForResult(pick, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMAGE_CAPTURE && requestCode != REQUEST_IMAGE_PICK) return;
        if (resultCode != RESULT_OK) return;
        try {
            File image;
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                image = currentPhotoPath == null ? null : new File(currentPhotoPath);
                if ((image == null || !image.exists() || image.length() == 0)
                        && data != null && data.getExtras() != null) {
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    if (bitmap != null) image = saveTaskPhotoBitmap(bitmap);
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                image = copyPickedTaskPhoto(data.getData());
            } else {
                return;
            }
            if (image == null || !image.exists() || image.length() == 0) {
                Toast.makeText(this, "Could not read the selected photo", Toast.LENGTH_SHORT).show();
                return;
            }
            saveTaskPhoto(image);
        } catch (Exception error) {
            Log.e(TAG, "Could not prepare task photo", error);
            Toast.makeText(this, "Could not prepare photo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File saveTaskPhotoBitmap(Bitmap bitmap) throws IOException {
        File file = createTaskPhotoFile();
        try (FileOutputStream output = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output);
        }
        return file;
    }

    private File copyPickedTaskPhoto(Uri uri) throws IOException {
        File file = createTaskPhotoFile();
        try (InputStream input = getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(file)) {
            if (input == null) throw new IOException("Cannot open selected photo");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        return file;
    }

    private void saveTaskPhoto(File file) {
        PhotoEntity photo = new PhotoEntity(farmId, null, taskId, null,
                file.getAbsolutePath(), null, System.currentTimeMillis());
        photoRepository.insertPhoto(photo, file, new PhotoRepository.PhotoUploadCallback() {
            @Override public void onSuccess(long id, String downloadUrl) { }
            @Override public void onError(String error) {
                Log.e(TAG, "Task photo upload failed", new IllegalStateException(error));
                runOnUiThread(() -> Toast.makeText(TaskDetailActivity.this,
                        "Photo saved locally; upload failed", Toast.LENGTH_LONG).show());
            }
            @Override public void onProgress(double progress) { }
        });
    }

    private void loadAssignmentHistory() {
        executor.execute(() -> {
            List<TaskAssignmentHistoryEntity> assignments =
                    database.taskDao().getAssignmentsForTask(taskId);

            runOnUiThread(() -> {
                assignmentHistoryContainer.removeAllViews();

                if (assignments == null || assignments.isEmpty()) {
                    layoutCurrentAssignment.setVisibility(View.VISIBLE);
                    tvNoAssignments.setVisibility(View.GONE);
                    tvCurrentWorker.setText("Unassigned");
                    tvWorkerInitials.setText("—");
                    tvAssignedSince.setText("");
                    statAssignedWorkerValue.setText("Unassigned");
                    return;
                }

                tvNoAssignments.setVisibility(View.GONE);

                TaskAssignmentHistoryEntity current = assignments.get(0);
                if (current != null && current.isActive()) {
                    layoutCurrentAssignment.setVisibility(View.VISIBLE);
                    tvCurrentWorker.setText(current.getAssignedTo());
                    tvAssignedSince.setText("Assigned: " +
                            dateFormat.format(new Date(current.getAssignedAt())));
                    tvWorkerInitials.setText(initials(current.getAssignedTo()));
                    tvNoAssignments.setVisibility(View.GONE);
                    statAssignedWorkerValue.setText(shortWorkerName(current.getAssignedTo()));
                } else {
                    layoutCurrentAssignment.setVisibility(View.VISIBLE);
                    tvNoAssignments.setVisibility(View.GONE);
                    if (current != null) {
                        // Completed tasks have no active assignment. Show the
                        // most recent worker who carried out the work instead.
                        tvCurrentWorker.setText(current.getAssignedTo());
                        tvWorkerInitials.setText(initials(current.getAssignedTo()));
                        tvAssignedSince.setText("Assigned: " +
                                dateFormat.format(new Date(current.getAssignedAt())));
                        statAssignedWorkerValue.setText(shortWorkerName(current.getAssignedTo()));
                    } else {
                        tvCurrentWorker.setText("Unassigned");
                        tvWorkerInitials.setText("—");
                        tvAssignedSince.setText("");
                        statAssignedWorkerValue.setText("Unassigned");
                    }
                }

                for (int i = (current != null && current.isActive()) ? 1 : 0; i < assignments.size(); i++) {
                    TaskAssignmentHistoryEntity assignment = assignments.get(i);
                    addAssignmentToHistory(assignment);
                }
            });
        });
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "—";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String shortWorkerName(String name) {
        if (name == null) return "—";
        String[] parts = name.trim().split("\\s+");
        if (parts.length < 2) return name;
        return parts[0] + " " + parts[parts.length - 1].substring(0, 1) + ".";
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

            if (newStatus == TaskEntity.TaskStatus.COMPLETED && task.getCompletedDate() == null) {
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
            btnEdit.setVisibility(View.GONE);
            displayTaskData();
        }

        setFieldsEditable(enable);
    }

    private void confirmDelete() {
        if (task == null) return;

        TaskDeleteConfirmDialog dialog = TaskDeleteConfirmDialog.newInstance(task.getTitle());
        dialog.setOnConfirmListener(this::deleteTask);
        dialog.show(getSupportFragmentManager(), "TaskDeleteConfirmDialog");
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

        tvTaskIcon.setImageResource(R.drawable.ic_clipboard_check);
        tvTaskIcon.setColorFilter(Color.WHITE);

        tvTaskTitle.setText(task.getTitle());
        tvTaskType.setText(task.getType().getDisplayName());
        statDueDateValue.setText(new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(new Date(task.getDueDate())));

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
            layoutCompletedDate.setVisibility(View.VISIBLE);
            tvCompletedDate.setText("—");
        }

        // 42: start date. Only present on backdated tasks.
        if (task.getStartDate() != null && task.getStartDate() > 0) {
            layoutStartDate.setVisibility(View.VISIBLE);
            tvStartDate.setText(dateFormat.format(new Date(task.getStartDate())));
        } else {
            layoutStartDate.setVisibility(View.VISIBLE);
            tvStartDate.setText("—");
        }

        tvDescription.setText(task.getDescription() == null ? "" : task.getDescription());
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
        tvHeroStatus.setText(task.getStatus().getDisplayName());
        statStatusValue.setText(task.getStatus().getDisplayName());
        int currentStep = task.getStatus() == TaskEntity.TaskStatus.PENDING ? 0
                : task.getStatus() == TaskEntity.TaskStatus.IN_PROGRESS ? 1
                : task.getStatus() == TaskEntity.TaskStatus.COMPLETED ? 2 : -1;
        updateStep(tvPendingDot, findViewById(R.id.tvPendingLabel), 0, currentStep);
        updateStep(tvInProgressDot, findViewById(R.id.tvInProgressLabel), 1, currentStep);
        updateStep(tvCompletedDot, findViewById(R.id.tvCompletedLabel), 2, currentStep);
        View firstLine = findViewById(R.id.viewPendingInProgressLine);
        View secondLine = findViewById(R.id.viewInProgressCompletedLine);
        firstLine.setBackgroundColor(currentStep > 0 ? 0xFF0E6B3C : 0xFFE9E1CF);
        secondLine.setBackgroundColor(currentStep > 1 ? 0xFF0E6B3C : 0xFFE9E1CF);

        tvCurrentStatus.setTextColor(0xFF1C1B1A);
    }

    private void updateStep(TextView dot, TextView label, int index, int currentStep) {
        boolean reached = currentStep >= index && currentStep >= 0;
        dot.setText(reached ? "✓" : "");
        GradientDrawable outer = new GradientDrawable();
        outer.setShape(GradientDrawable.OVAL);
        outer.setColor(reached && index == currentStep ? 0xFFE3F0E7 : Color.TRANSPARENT);
        GradientDrawable inner = new GradientDrawable();
        inner.setShape(GradientDrawable.OVAL);
        if (reached) inner.setColor(0xFF0E6B3C);
        else {
            inner.setColor(Color.WHITE);
            inner.setStroke(dp(2), 0xFFC9C2B4);
        }
        android.graphics.drawable.LayerDrawable layers =
                new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{outer, inner});
        layers.setLayerInset(1, dp(5), dp(5), dp(5), dp(5));
        dot.setBackground(layers);
        dot.setTextColor(reached ? Color.WHITE : Color.TRANSPARENT);
        dot.setPadding(0, 0, 0, 0);
        label.setTextColor(index == currentStep ? 0xFF0A5530 : 0xFF59544C);
        label.setTypeface(android.graphics.Typeface.create(
                androidx.core.content.res.ResourcesCompat.getFont(this,
                        index == currentStep ? R.font.manrope_extrabold : R.font.manrope_bold),
                android.graphics.Typeface.NORMAL));
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
