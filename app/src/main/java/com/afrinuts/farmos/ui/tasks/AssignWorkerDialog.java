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
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.TaskAssignmentRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AssignWorkerDialog extends DialogFragment {

    private static final String ARG_TASK_ID = "task_id";

    private long taskId;
    private AppDatabase database;
    private TaskAssignmentRepository assignmentRepository;
    private List<String> existingWorkers;
    private long farmId;
    private TaskEntity task;

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

            Log.d("AssignWorkerDialog", "Loaded " + existingWorkers.size() + " existing workers");

            // Now that everything is loaded, show the dialog on UI thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Dialog will be created when onCreateDialog is called
                    // But we need to ensure everything is ready
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
                // Check if task already has an active assignment
                TaskAssignmentHistoryEntity currentAssignment =
                        database.taskDao().getCurrentAssignment(taskId);

                // Complete current assignment if exists
                if (currentAssignment != null) {
                    currentAssignment.completeAssignment();
                    database.taskDao().updateAssignment(currentAssignment);
                    Log.d("AssignWorkerDialog", "Completed existing assignment: " + currentAssignment.getId());
                }

                // Create new assignment
                TaskAssignmentHistoryEntity newAssignment = new TaskAssignmentHistoryEntity(
                        farmId,
                        taskId,
                        workerName,
                        System.currentTimeMillis()
                );

                Log.d("AssignWorkerDialog", "Creating new assignment - taskId: " + taskId +
                        ", worker: " + workerName + ", farmId: " + farmId);

                // Insert using repository
                assignmentRepository.insertAssignment(newAssignment, new BaseRepository.InsertCallback() {
                    @Override
                    public void onSuccess(long id) {
                        Log.d("AssignWorkerDialog", "Assignment inserted successfully with ID: " + id);
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