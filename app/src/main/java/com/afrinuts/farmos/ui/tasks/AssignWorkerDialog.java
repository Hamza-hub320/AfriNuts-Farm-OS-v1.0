package com.afrinuts.farmos.ui.tasks;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.dao.TaskDao;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.TaskAssignmentRepository;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AssignWorkerDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {

    private static final String ARG_TASK_ID = "task_id";

    private long taskId;
    private AppDatabase database;
    private TaskAssignmentRepository assignmentRepository;
    private List<String> existingWorkers;
    private long farmId;
    private TaskEntity task;
    private SyncManager syncManager;
    private MaterialAutoCompleteTextView etWorkerName;
    private TextInputEditText etNewWorker;

    public interface OnWorkerAssignedListener {
        void onWorkerAssigned();
    }

    private OnWorkerAssignedListener listener;

    public static AssignWorkerDialog newInstance(long taskId) {
        AssignWorkerDialog dialog = new AssignWorkerDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID, taskId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnWorkerAssignedListener(OnWorkerAssignedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            taskId = getArguments().getLong(ARG_TASK_ID);
        }

        database = AppDatabase.getInstance(requireContext());

        // Load everything synchronously BEFORE showing the dialog
        new Thread(() -> {
            // Get the task
            task = database.taskDao().getTaskById(taskId);
            if (task != null) {
                farmId = task.getFarmId();
                Log.d("AssignWorkerDialog", "Task found: " + task.getTitle() + ", farmId: " + farmId);
            } else {
                Log.e("AssignWorkerDialog", "Task not found with ID: " + taskId);
            }

            // Initialize repository
            assignmentRepository = new TaskAssignmentRepository(requireContext(), farmId);

            // Load existing workers
            List<TaskDao.WorkerTaskCount> workerCounts = database.taskDao().getWorkerTaskCounts();
            existingWorkers = new java.util.ArrayList<>();
            for (TaskDao.WorkerTaskCount wc : workerCounts) {
                existingWorkers.add(wc.assignedTo);
            }

            syncManager = SyncManager.getInstance(requireContext());

            Log.d("AssignWorkerDialog", "Loaded " + existingWorkers.size() + " existing workers");

            // Now that everything is loaded, show the dialog on UI thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                });
            }
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_assign_worker, null);

        initViews(view);

        // Wait for data to be loaded, but show dialog anyway with loading state
        if (existingWorkers != null && !existingWorkers.isEmpty()) {
            setupWorkerSuggestions();
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        // Get custom buttons from the layout
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnAssign = view.findViewById(R.id.btnAssign);

        // Set click listeners
        btnCancel.setOnClickListener(v -> dismiss());
        btnAssign.setOnClickListener(v -> assignWorker());

        return dialog;
    }

    private void initViews(View view) {
        etWorkerName = view.findViewById(R.id.etWorkerName);
        etNewWorker = view.findViewById(R.id.etNewWorker);
    }

    private void setupWorkerSuggestions() {
        if (etWorkerName != null && existingWorkers != null && !existingWorkers.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    existingWorkers
            );
            etWorkerName.setAdapter(adapter);
        }
    }

    private void assignWorker() {
        // Check if repository is ready
        if (assignmentRepository == null) {
            Toast.makeText(getContext(), "Still loading, please try again", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedWorker = etWorkerName.getText().toString().trim();
        String newWorker = etNewWorker.getText().toString().trim();

        String workerName;
        if (!TextUtils.isEmpty(newWorker)) {
            workerName = newWorker;
        } else if (!TextUtils.isEmpty(selectedWorker)) {
            workerName = selectedWorker;
        } else {
            Toast.makeText(getContext(), "Please enter or select a worker name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable assign button to prevent double click
        if (getDialog() != null) {
            Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);
        }

        Toast.makeText(getContext(), "Assigning worker...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // 42: backdate the assignment window when the task is already
                // COMPLETED (i.e. it was logged as past work). Determine this up
                // front so both branches below — close previous, create new —
                // agree on the window.
                //
                // Fallbacks:
                //   task == null (load race in onCreate)        → normal flow
                //   COMPLETED, completedDate null (shouldn't)   → both = now
                //   COMPLETED, startDate null (pre-42 record)   → assignedAt = completedDate (duration 0)
                boolean taskAlreadyCompleted =
                        task != null && task.getStatus() == TaskEntity.TaskStatus.COMPLETED;
                long taskEnd = (task != null && task.getCompletedDate() != null)
                        ? task.getCompletedDate() : System.currentTimeMillis();
                long taskStart = (task != null && task.getStartDate() != null)
                        ? task.getStartDate() : taskEnd;

                // Check if task already has an active assignment.
                // Note: born-closed assignments (unassignedAt != null) are never
                // returned by this query, so the backdate path below cannot
                // re-close an assignment that was already credited to another
                // worker on an earlier session.
                TaskAssignmentHistoryEntity currentAssignment =
                        database.taskDao().getCurrentAssignment(taskId);

                // Close the current assignment if one exists.
                if (currentAssignment != null) {
                    if (taskAlreadyCompleted) {
                        // Past work: the previous worker's window ends when the
                        // work ended, not today.
                        currentAssignment.setUnassignedAt(taskEnd);
                        database.taskDao().updateAssignment(currentAssignment);
                        Log.d("AssignWorkerDialog",
                                "Closed existing assignment " + currentAssignment.getId()
                                        + " at task completedDate " + taskEnd);
                    } else {
                        currentAssignment.completeAssignment();
                        database.taskDao().updateAssignment(currentAssignment);
                        Log.d("AssignWorkerDialog",
                                "Completed existing assignment: " + currentAssignment.getId());
                    }

                    // Queue the update for sync (unchanged)
                    syncManager.queueForSync("assignments", currentAssignment.getId(),
                            SyncEntity.SyncAction.UPDATE);
                }

                // Create new assignment. In past-work mode, assignedAt is the
                // task's startDate (not now), and the assignment is born closed
                // at completedDate — the real work window.
                TaskAssignmentHistoryEntity newAssignment = new TaskAssignmentHistoryEntity(
                        farmId,
                        taskId,
                        workerName,
                        taskAlreadyCompleted ? taskStart : System.currentTimeMillis()
                );

                if (taskAlreadyCompleted) {
                    newAssignment.setUnassignedAt(taskEnd);
                }

                Log.d("AssignWorkerDialog", "Creating new assignment - taskId: " + taskId +
                        ", worker: " + workerName + ", farmId: " + farmId +
                        (taskAlreadyCompleted
                                ? ", backdated window: " + taskStart + " → " + taskEnd
                                : ", assignedAt: now, open"));

                // Insert using repository
                assignmentRepository.insertAssignment(newAssignment, new BaseRepository.InsertCallback() {
                    @Override
                    public void onSuccess(long id) {
                        Log.d("AssignWorkerDialog", "Assignment inserted successfully with ID: " + id);

                        // ✅ FORCE SYNC IMMEDIATELY AFTER SUCCESS
                        syncManager.forceSyncNow();
                        Log.d("AssignWorkerDialog", "Force sync triggered");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Worker assigned successfully", Toast.LENGTH_SHORT).show();
                                if (listener != null) {
                                    listener.onWorkerAssigned();
                                }
                                dismiss();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("AssignWorkerDialog", "Error inserting assignment: " + error);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Error assigning worker: " + error, Toast.LENGTH_LONG).show();
                                if (getDialog() != null) {
                                    Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
                                    positiveButton.setEnabled(true);
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("AssignWorkerDialog", "Error in assignWorker", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        if (getDialog() != null) {
                            Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
                            positiveButton.setEnabled(true);
                        }
                    });
                }
            }
        }).start();
    }
}