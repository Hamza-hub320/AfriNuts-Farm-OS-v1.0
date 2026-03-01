package com.afrinuts.farmos.ui.tasks;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TaskStatusUpdateDialog extends DialogFragment {

    private static final String ARG_CURRENT_STATUS = "current_status";
    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_TASK_TITLE = "task_title";

    private TaskEntity.TaskStatus currentStatus;
    private long taskId;
    private String taskTitle;

    private RadioGroup radioGroup;
    private RadioButton rbPending;
    private RadioButton rbInProgress;
    private RadioButton rbCompleted;
    private RadioButton rbCancelled;
    private TextView tvCurrentStatus;

    public interface OnStatusUpdatedListener {
        void onStatusUpdated(TaskEntity.TaskStatus newStatus);
    }

    private OnStatusUpdatedListener listener;

    public static TaskStatusUpdateDialog newInstance(long taskId, String taskTitle,
                                                     TaskEntity.TaskStatus currentStatus) {
        TaskStatusUpdateDialog dialog = new TaskStatusUpdateDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID, taskId);
        args.putString(ARG_TASK_TITLE, taskTitle);
        args.putSerializable(ARG_CURRENT_STATUS, currentStatus);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnStatusUpdatedListener(OnStatusUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            taskId = getArguments().getLong(ARG_TASK_ID);
            taskTitle = getArguments().getString(ARG_TASK_TITLE);
            currentStatus = (TaskEntity.TaskStatus) getArguments().getSerializable(ARG_CURRENT_STATUS);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_task_status_update, null);

        initViews(view);
        setCurrentStatus();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Status: " + taskTitle)
                .setView(view)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> updateStatus());
        });

        return dialog;
    }

    private void initViews(View view) {
        radioGroup = view.findViewById(R.id.radioGroup);
        rbPending = view.findViewById(R.id.rbPending);
        rbInProgress = view.findViewById(R.id.rbInProgress);
        rbCompleted = view.findViewById(R.id.rbCompleted);
        rbCancelled = view.findViewById(R.id.rbCancelled);
        tvCurrentStatus = view.findViewById(R.id.tvCurrentStatus);
    }

    private void setCurrentStatus() {
        tvCurrentStatus.setText("Current: " + currentStatus.getIcon() + " " +
                currentStatus.getDisplayName());

        // Pre-select the current status
        switch (currentStatus) {
            case PENDING:
                rbPending.setChecked(true);
                break;
            case IN_PROGRESS:
                rbInProgress.setChecked(true);
                break;
            case COMPLETED:
                rbCompleted.setChecked(true);
                break;
            case CANCELLED:
                rbCancelled.setChecked(true);
                break;
        }
    }

    private void updateStatus() {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        TaskEntity.TaskStatus newStatus = null;

        if (selectedId == R.id.rbPending) {
            newStatus = TaskEntity.TaskStatus.PENDING;
        } else if (selectedId == R.id.rbInProgress) {
            newStatus = TaskEntity.TaskStatus.IN_PROGRESS;
        } else if (selectedId == R.id.rbCompleted) {
            newStatus = TaskEntity.TaskStatus.COMPLETED;
        } else if (selectedId == R.id.rbCancelled) {
            newStatus = TaskEntity.TaskStatus.CANCELLED;
        }

        if (newStatus == null) {
            Toast.makeText(getContext(), "Please select a status", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newStatus == currentStatus) {
            Toast.makeText(getContext(), "Status is already " + newStatus.getDisplayName(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (listener != null) {
            listener.onStatusUpdated(newStatus);
        }
        dismiss();
    }
}