package com.afrinuts.farmos.ui.tasks;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.afrinuts.farmos.data.local.dao.TaskDao;  // Add this import
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AssignWorkerDialog extends DialogFragment {

    private static final String ARG_TASK_ID = "task_id";

    private long taskId;
    private AppDatabase database;
    private List<String> existingWorkers = new ArrayList<>();

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

        // Load existing workers for suggestions
        new Thread(() -> {
            // Use TaskDao.WorkerTaskCount - WorkerTaskCount is an inner class of TaskDao
            List<TaskDao.WorkerTaskCount> workerCounts =
                    database.taskDao().getWorkerTaskCounts();

            existingWorkers.clear();
            for (TaskDao.WorkerTaskCount wc : workerCounts) {
                existingWorkers.add(wc.assignedTo);  // Now 'assignedTo' will be recognized
            }

            // Update UI on main thread if needed
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    setupWorkerSuggestions();
                });
            }
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_assign_worker, null);

        initViews(view);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Assign Worker")
                .setView(view)
                .setPositiveButton("Assign", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> assignWorker());
        });

        return dialog;
    }

    private void initViews(View view) {
        etWorkerName = view.findViewById(R.id.etWorkerName);
        etNewWorker = view.findViewById(R.id.etNewWorker);
    }

    private void setupWorkerSuggestions() {
        if (etWorkerName != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    existingWorkers
            );
            etWorkerName.setAdapter(adapter);
        }
    }

    private void assignWorker() {
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

        // Check if task already has an active assignment
        new Thread(() -> {
            TaskAssignmentHistoryEntity currentAssignment =
                    database.taskDao().getCurrentAssignment(taskId);

            if (currentAssignment != null) {
                // Complete the current assignment
                currentAssignment.completeAssignment();
                database.taskDao().updateAssignment(currentAssignment);
            }

            // Create new assignment
            TaskAssignmentHistoryEntity newAssignment = new TaskAssignmentHistoryEntity(
                    taskId,
                    workerName,
                    System.currentTimeMillis()
            );

            long id = database.taskDao().insertAssignment(newAssignment);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (id > 0) {
                        Toast.makeText(getContext(),
                                "Worker assigned successfully", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onWorkerAssigned();
                        }
                        dismiss();
                    } else {
                        Toast.makeText(getContext(),
                                "Error assigning worker", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
}