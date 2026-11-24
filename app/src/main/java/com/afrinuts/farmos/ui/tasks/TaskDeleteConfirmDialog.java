package com.afrinuts.farmos.ui.tasks;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.farm.FarmDialogFragment;
import com.google.android.material.button.MaterialButton;

public class TaskDeleteConfirmDialog extends FarmDialogFragment {
    private static final String ARG_TASK_TITLE = "task_title";
    private Runnable onConfirm;

    public static TaskDeleteConfirmDialog newInstance(String taskTitle) {
        TaskDeleteConfirmDialog dialog = new TaskDeleteConfirmDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TASK_TITLE, taskTitle);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnConfirmListener(Runnable listener) {
        onConfirm = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_delete_confirm, null);
        String title = getArguments() == null ? null : getArguments().getString(ARG_TASK_TITLE);
        TextView message = view.findViewById(R.id.tvDeleteTaskMessage);
        message.setText("Delete “" + (title == null || title.trim().isEmpty() ? "this task" : title)
                + "”? This action cannot be undone.");
        MaterialButton cancel = view.findViewById(R.id.btnCancelDeleteTask);
        MaterialButton delete = view.findViewById(R.id.btnConfirmDeleteTask);
        cancel.setOnClickListener(v -> dismiss());
        delete.setOnClickListener(v -> {
            if (onConfirm != null) onConfirm.run();
            dismiss();
        });
        return new AlertDialog.Builder(requireContext()).setView(view).create();
    }
}
