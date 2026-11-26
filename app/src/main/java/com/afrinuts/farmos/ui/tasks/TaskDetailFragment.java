package com.afrinuts.farmos.ui.tasks;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;
import com.afrinuts.farmos.ui.compliance.PhotoSourceDialog;
import com.afrinuts.farmos.utils.SessionManager;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Tablet detail pane for a task; phone navigation remains in TaskDetailActivity. */
public class TaskDetailFragment extends Fragment {
    private static final String ARG_TASK_ID = "task_id";
    public interface Listener { void onTaskDeleted(long taskId); }
    public static TaskDetailFragment newInstance(long taskId) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle(); args.putLong(ARG_TASK_ID, taskId); fragment.setArguments(args); return fragment;
    }

    private long taskId, farmId;
    private TaskEntity task;
    private AppDatabase database;
    private TaskRepository taskRepository;
    private PhotoRepository photoRepository;
    private Listener listener;
    private final Map<Long, String> blockNames = new HashMap<>();
    private View root;
    private LinearLayout detailsRows, photoStrip;
    private TextView title, type, heroStatus, statStatus, statDueDate, statAssignee, currentStatus;
    private TextView workerInitials, workerName, assignedDate, photoCount;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri pendingCameraUri;
    private File pendingCameraFile;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) {
            pendingCameraUri = state.getParcelable("pending_task_photo_uri");
            String path = state.getString("pending_task_photo_path");
            if (path != null) pendingCameraFile = new File(path);
        }
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), saved -> {
            if (saved && pendingCameraFile != null) saveTaskPhoto(pendingCameraFile);
            pendingCameraUri = null;
            pendingCameraFile = null;
        });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) importTaskPhoto(uri);
        });
        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) launchCamera();
            else if (isAdded()) Toast.makeText(requireContext(), "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show();
        });
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingCameraUri != null) {
            outState.putParcelable("pending_task_photo_uri", pendingCameraUri);
            if (pendingCameraFile != null) outState.putString("pending_task_photo_path", pendingCameraFile.getAbsolutePath());
        }
    }

    @Override public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof Listener) listener = (Listener) context;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        root = inflater.inflate(R.layout.fragment_task_detail_tablet, container, false);
        database = AppDatabase.getInstance(requireContext());
        detailsRows = root.findViewById(R.id.taskDetailsRows);
        photoStrip = root.findViewById(R.id.taskPhotoStrip);
        title = root.findViewById(R.id.taskTitle);
        type = root.findViewById(R.id.taskType);
        heroStatus = root.findViewById(R.id.taskHeroStatus);
        statStatus = root.findViewById(R.id.statStatus);
        statDueDate = root.findViewById(R.id.statDueDate);
        statAssignee = root.findViewById(R.id.statAssignee);
        currentStatus = root.findViewById(R.id.currentStatus);
        workerInitials = root.findViewById(R.id.workerInitials);
        workerName = root.findViewById(R.id.workerName);
        assignedDate = root.findViewById(R.id.assignedDate);
        photoCount = root.findViewById(R.id.taskPhotoCount);
        root.findViewById(R.id.btnAddTaskPhoto).setOnClickListener(v -> showPhotoSourceDialog());

        long id = getArguments() == null ? -1 : getArguments().getLong(ARG_TASK_ID, -1);
        taskId = id;
        root.findViewById(R.id.btnStatusUpdate).setOnClickListener(v -> showStatusDialog());
        root.findViewById(R.id.btnUpdateStatusBottom).setOnClickListener(v -> showStatusDialog());
        root.findViewById(R.id.btnAssignWorker).setOnClickListener(v -> showAssignDialog());
        root.findViewById(R.id.btnDeleteTask).setOnClickListener(v -> confirmDelete());

        SessionManager session = new SessionManager(requireContext());
        if (session.isInvestor()) {
            root.findViewById(R.id.btnAssignWorker).setVisibility(View.GONE);
            root.findViewById(R.id.btnUpdateStatusBottom).setVisibility(View.GONE);
            root.findViewById(R.id.btnStatusUpdate).setVisibility(View.GONE);
            root.findViewById(R.id.btnDeleteTask).setVisibility(View.GONE);
        } else if (session.isManager()) {
            root.findViewById(R.id.btnDeleteTask).setVisibility(View.GONE);
        }
        load(id);
        return root;
    }

    private void load(long id) {
        if (id < 0) return;
        executor.execute(() -> {
            com.afrinuts.farmos.data.local.entity.FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm == null || !isAdded()) return;
            farmId = farm.getId();
            List<com.afrinuts.farmos.data.local.entity.BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farmId);
            synchronized (blockNames) {
                blockNames.clear();
                if (blocks != null) for (com.afrinuts.farmos.data.local.entity.BlockEntity block : blocks) blockNames.put(block.getId(), block.getBlockName());
            }
            taskRepository = new TaskRepository(requireContext(), farmId);
            photoRepository = new PhotoRepository(requireContext(), farmId);
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                taskRepository.getTaskById(id).observe(getViewLifecycleOwner(), new Observer<TaskEntity>() {
                    @Override public void onChanged(TaskEntity value) {
                        if (value == null) return;
                        task = value;
                        displayTask();
                        loadAssignment();
                    }
                });
                photoRepository.getPhotosForTask(id).observe(getViewLifecycleOwner(), this::displayPhotos);
            });
        });
    }

    private void displayTask() {
        title.setText(task.getTitle());
        type.setText(task.getType().getDisplayName());
        ImageView icon = root.findViewById(R.id.taskTypeIcon);
        icon.setImageResource(R.drawable.ic_clipboard_check);
        String status = task.getStatus().getDisplayName();
        heroStatus.setText(status);
        statStatus.setText(status);
        currentStatus.setText(status);
        statDueDate.setText(dateFormat.format(new Date(task.getDueDate())));
        ((TextView) root.findViewById(R.id.taskDescription)).setText(task.getDescription() == null || task.getDescription().trim().isEmpty() ? "—" : task.getDescription());
        ((View) root.findViewById(R.id.taskStatusChipDot)).setBackground(makeOval(0xFF0E6B3C));
        updateStepper();
        detailsRows.removeAllViews();
        String block = "Farm-Wide";
        if (task.getBlockId() != null) {
            synchronized (blockNames) { block = blockNames.getOrDefault(task.getBlockId(), "—"); }
        }
        addDetailRow("Block", task.getBlockId() == null ? "Farm-Wide" : (block.startsWith("Block ") ? block : "Block " + block));
        addDetailRow("Due Date", dateFormat.format(new Date(task.getDueDate())));
        addDetailRow("Completed", task.getCompletedDate() == null ? "—" : dateFormat.format(new Date(task.getCompletedDate())));
        addDetailRow("Started", task.getStartDate() == null || task.getStartDate() <= 0 ? "—" : dateFormat.format(new Date(task.getStartDate())));
    }

    private void updateStepper() {
        int current = task.getStatus() == TaskEntity.TaskStatus.PENDING ? 0 : task.getStatus() == TaskEntity.TaskStatus.IN_PROGRESS ? 1 : task.getStatus() == TaskEntity.TaskStatus.COMPLETED ? 2 : -1;
        TextView[] dots = {root.findViewById(R.id.stepPending), root.findViewById(R.id.stepProgress), root.findViewById(R.id.stepDone)};
        TextView[] labels = {root.findViewById(R.id.labelPending), root.findViewById(R.id.labelProgress), root.findViewById(R.id.labelDone)};
        View[] lines = {root.findViewById(R.id.stepLine1), root.findViewById(R.id.stepLine2)};
        for (int i = 0; i < dots.length; i++) {
            boolean reached = current >= i && current >= 0;
            GradientDrawable circle = new GradientDrawable(); circle.setShape(GradientDrawable.OVAL);
            if (reached) circle.setColor(0xFF0E6B3C); else { circle.setColor(Color.WHITE); circle.setStroke(dp(2), 0xFFC9C2B4); }
            dots[i].setBackground(circle);
            dots[i].setText(reached ? "✓" : "");
            labels[i].setTextColor(i == current ? 0xFF075C32 : 0xFF514C45);
        }
        lines[0].setBackgroundColor(current > 0 ? 0xFF0E6B3C : 0xFFE9E1CF);
        lines[1].setBackgroundColor(current > 1 ? 0xFF0E6B3C : 0xFFE9E1CF);
    }

    private void addDetailRow(String label, String value) {
        LinearLayout row = new LinearLayout(requireContext()); row.setGravity(android.view.Gravity.CENTER_VERTICAL); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0, dp(13), 0, dp(13));
        TextView left = text(label, 16, 0xFF514C45, false);
        TextView right = text(value, 17, 0xFF1C1B1A, true); right.setGravity(android.view.Gravity.END);
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
        detailsRows.addView(row);
        View divider = new View(requireContext()); divider.setBackgroundColor(0xFFE9E1CF); detailsRows.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void loadAssignment() {
        executor.execute(() -> {
            List<TaskAssignmentHistoryEntity> assignments = database.taskDao().getAssignmentsForTask(taskId);
            TaskAssignmentHistoryEntity current = assignments == null || assignments.isEmpty() ? null : assignments.get(0);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (current == null) {
                    workerName.setText("Unassigned"); workerInitials.setText("—"); assignedDate.setText(""); statAssignee.setText("Unassigned");
                } else {
                    String name = current.getAssignedTo(); workerName.setText(name); workerInitials.setText(initials(name));
                    assignedDate.setText("Assigned: " + dateFormat.format(new Date(current.getAssignedAt())));
                    statAssignee.setText(shortName(name));
                }
            });
        });
    }

    private void displayPhotos(List<PhotoEntity> photos) {
        if (!isAdded() || photoStrip == null) return;
        photoStrip.removeAllViews(); int count = photos == null ? 0 : photos.size();
        photoCount.setText(count + (count == 1 ? " photo" : " photos"));
        if (photos == null) return;
        for (PhotoEntity photo : photos) {
            ImageView image = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(92), dp(92)); params.setMarginEnd(dp(10)); image.setLayoutParams(params);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP); image.setBackground(makePhotoBackground()); image.setClipToOutline(true);
            String path = photo.getThumbnailPath() == null || photo.getThumbnailPath().trim().isEmpty() ? photo.getFilePath() : photo.getThumbnailPath();
            Glide.with(this).load(path).placeholder(R.drawable.ic_photo_placeholder).error(R.drawable.ic_photo_placeholder).centerCrop().into(image);
            image.setOnClickListener(v -> { android.content.Intent intent = new android.content.Intent(requireContext(), com.afrinuts.farmos.ui.photos.PhotoDetailActivity.class); intent.putExtra(com.afrinuts.farmos.ui.photos.PhotoDetailActivity.EXTRA_PHOTO_ID, photo.getId()); startActivity(intent); });
            photoStrip.addView(image);
        }
    }

    private void showPhotoSourceDialog() {
        if (photoRepository == null) {
            Toast.makeText(requireContext(), "Task is still loading", Toast.LENGTH_SHORT).show();
            return;
        }
        PhotoSourceDialog dialog = new PhotoSourceDialog();
        dialog.setListener(source -> {
            if (source == PhotoSourceDialog.SOURCE_CAMERA) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera();
                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else galleryLauncher.launch("image/*");
        });
        dialog.show(getParentFragmentManager(), "TaskPhotoSourceDialog");
    }

    private void launchCamera() {
        if (!isAdded()) return;
        android.content.Intent camera = new android.content.Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera.resolveActivity(requireContext().getPackageManager()) == null) {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            pendingCameraFile = createTaskPhotoFile();
            pendingCameraUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", pendingCameraFile);
            cameraLauncher.launch(pendingCameraUri);
        } catch (IOException error) {
            Toast.makeText(requireContext(), "Could not create photo file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createTaskPhotoFile() throws IOException {
        File directory = requireContext().getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory == null) throw new IOException("Photo storage unavailable");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Could not create photo folder");
        return File.createTempFile("TASK_" + System.currentTimeMillis() + "_", ".jpg", directory);
    }

    private void importTaskPhoto(Uri uri) {
        if (photoRepository == null) return;
        executor.execute(() -> {
            try {
                File image = createTaskPhotoFile();
                try (InputStream input = requireContext().getApplicationContext().getContentResolver().openInputStream(uri);
                     OutputStream output = new FileOutputStream(image)) {
                    if (input == null) throw new IOException("Cannot open selected photo");
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
                saveTaskPhoto(image);
            } catch (Exception error) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Could not prepare photo", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveTaskPhoto(File image) {
        PhotoRepository repository = photoRepository;
        if (repository == null || !image.exists() || image.length() == 0) return;
        PhotoEntity photo = new PhotoEntity(farmId, null, taskId, null, image.getAbsolutePath(), null, System.currentTimeMillis());
        repository.insertPhoto(photo, image, new PhotoRepository.PhotoUploadCallback() {
            @Override public void onSuccess(long id, String downloadUrl) { }
            @Override public void onError(String error) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Photo saved locally; upload failed", Toast.LENGTH_LONG).show());
            }
            @Override public void onProgress(double progress) { }
        });
    }

    private GradientDrawable makePhotoBackground() { GradientDrawable d = new GradientDrawable(); d.setColor(0xFFF5F0E6); d.setCornerRadius(dp(16)); d.setStroke(dp(1), 0xFFE9E1CF); return d; }
    private GradientDrawable makeOval(int color) { GradientDrawable d = new GradientDrawable(); d.setShape(GradientDrawable.OVAL); d.setColor(color); return d; }
    private TextView text(String value, int size, int color, boolean bold) { TextView t = new TextView(requireContext()); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(android.graphics.Typeface.create("sans-serif", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL)); return t; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private String initials(String name) { if (name == null || name.trim().isEmpty()) return "—"; String[] p = name.trim().split("\\s+"); return (p[0].substring(0,1) + (p.length > 1 ? p[p.length-1].substring(0,1) : "")).toUpperCase(Locale.ROOT); }
    private String shortName(String name) { if (name == null) return "—"; String[] p = name.trim().split("\\s+"); return p.length > 1 ? p[0] + " " + p[p.length-1].substring(0,1) + "." : name; }

    private void showStatusDialog() {
        if (task == null) return;
        TaskStatusUpdateDialog dialog = TaskStatusUpdateDialog.newInstance(taskId, task.getTitle(), task.getStatus());
        dialog.setOnStatusUpdatedListener(newStatus -> {
            task.setStatus(newStatus);
            if (newStatus == TaskEntity.TaskStatus.COMPLETED && task.getCompletedDate() == null) task.setCompletedDate(System.currentTimeMillis());
            taskRepository.updateTask(task, new BaseRepository.RepositoryCallback<Boolean>() {
                @Override public void onSuccess(Boolean result) { if (isAdded()) Toast.makeText(requireContext(), "Status updated", Toast.LENGTH_SHORT).show(); }
                @Override public void onError(String error) { if (isAdded()) Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show(); }
            });
        });
        dialog.show(getParentFragmentManager(), "TaskStatusUpdateDialog");
    }

    private void showAssignDialog() {
        if (task == null) return;
        AssignWorkerDialog dialog = AssignWorkerDialog.newInstance(taskId); dialog.setOnWorkerAssignedListener(this::loadAssignment);
        dialog.show(getParentFragmentManager(), "AssignWorkerDialog");
    }

    private void confirmDelete() {
        if (task == null) return;
        TaskDeleteConfirmDialog dialog = TaskDeleteConfirmDialog.newInstance(task.getTitle()); dialog.setOnConfirmListener(() -> taskRepository.deleteTask(task, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean result) { if (listener != null) listener.onTaskDeleted(taskId); }
            @Override public void onError(String error) { if (isAdded()) Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show(); }
        }));
        dialog.show(getParentFragmentManager(), "TaskDeleteConfirmDialog");
    }

    @Override public void onDestroy() { super.onDestroy(); if (taskRepository != null) taskRepository.shutdown(); executor.shutdown(); }
    @Override public void onDetach() { listener = null; super.onDetach(); }
}
