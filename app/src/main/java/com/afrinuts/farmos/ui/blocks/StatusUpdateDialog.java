package com.afrinuts.farmos.ui.blocks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class StatusUpdateDialog extends DialogFragment {

    private static final String ARG_BLOCK_ID = "block_id";
    private static final String ARG_CURRENT_STATUS = "current_status";

    private long blockId;
    private BlockEntity.BlockStatus currentStatus;
    private BlockEntity block;
    private BlockEntity.BlockStatus nextStatus;
    private Calendar selectedDate = Calendar.getInstance();
    private AppDatabase database;

    private TextView currentStatusText;
    private TextView nextStatusText;
    private TextInputEditText etDate;

    public interface OnStatusUpdatedListener {
        void onStatusUpdated(BlockEntity updatedBlock);
    }

    private OnStatusUpdatedListener listener;

    public static StatusUpdateDialog newInstance(BlockEntity block) {
        StatusUpdateDialog dialog = new StatusUpdateDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_BLOCK_ID, block.getId());
        args.putSerializable(ARG_CURRENT_STATUS, block.getStatus());
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
            blockId = getArguments().getLong(ARG_BLOCK_ID);
            currentStatus = (BlockEntity.BlockStatus) getArguments().getSerializable(ARG_CURRENT_STATUS);
        }

        database = AppDatabase.getInstance(requireContext());

        // Load block from database
        new Thread(() -> {
            block = database.blockDao().getBlockById(blockId);
            if (block != null) {
                determineNextStatus();
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (currentStatusText != null && nextStatusText != null) {
                            updateStatusTexts();
                        }
                    });
                }
            }
        }).start();
    }

    private void determineNextStatus() {
        if (block == null) return;

        switch (currentStatus) {
            case NOT_CLEARED:
                nextStatus = BlockEntity.BlockStatus.CLEARED;
                break;
            case CLEARED:
                nextStatus = BlockEntity.BlockStatus.PLOWED;
                break;
            case PLOWED:
                nextStatus = BlockEntity.BlockStatus.PLANTED;
                break;
            case PLANTED:
                nextStatus = null; // No further status
                break;
        }
    }

    private void updateStatusTexts() {
        if (currentStatusText != null) {
            currentStatusText.setText(currentStatus.getDisplayName());
        }
        if (nextStatusText != null) {
            if (nextStatus != null) {
                nextStatusText.setText(nextStatus.getDisplayName());
            } else {
                nextStatusText.setText("No further status");
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_status_update, null);

        initViews(view);

        // If block is already planted, show message and dismiss
        if (currentStatus == BlockEntity.BlockStatus.PLANTED) {
            Toast.makeText(getContext(), "Block is already planted - no further status updates", Toast.LENGTH_LONG).show();
            dismiss();
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Block Status")
                .setView(view)
                .setPositiveButton("Update", null) // We'll override this
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> updateStatus());
        });

        return dialog;
    }

    private void initViews(View view) {
        currentStatusText = view.findViewById(R.id.currentStatusText);
        nextStatusText = view.findViewById(R.id.nextStatusText);
        etDate = view.findViewById(R.id.etDate);

        // Set status texts if block is already loaded
        if (block != null) {
            updateStatusTexts();
        }

        // Set current date as default
        updateDateLabel();

        // Date picker
        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setFocusable(false);
        etDate.setClickable(true);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void updateStatus() {
        if (block == null) {
            Toast.makeText(getContext(), "Block not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nextStatus == null) {
            Toast.makeText(getContext(), "Cannot update status further", Toast.LENGTH_SHORT).show();
            return;
        }

        long date = selectedDate.getTimeInMillis();

        // Update block based on new status
        switch (nextStatus) {
            case CLEARED:
                block.setClearedDate(date);
                break;
            case PLOWED:
                block.setPlowedDate(date);
                break;
            case PLANTED:
                block.setPlantedDate(date);
                break;
        }

        block.setStatus(nextStatus);
        block.setUpdatedAt(System.currentTimeMillis());

        // Save to database
        new Thread(() -> {
            database.blockDao().update(block);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Notify listener
                    if (listener != null) {
                        listener.onStatusUpdated(block);
                    }
                    dismiss();
                });
            }
        }).start();
    }
}