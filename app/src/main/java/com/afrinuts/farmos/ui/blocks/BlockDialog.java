package com.afrinuts.farmos.ui.blocks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BlockDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";

    private long farmId;
    private AppDatabase database;
    private FarmEntity currentFarm;

    // UI Elements
    private TextInputEditText etBlockName;
    private TextInputEditText etHectareSize;
    private MaterialAutoCompleteTextView etStatus;
    private TextInputEditText etDate;
    private TextInputEditText etSurvivalRate;
    private TextInputEditText etReplacementCount;
    private TextInputEditText etNotes;

    // Layout containers for conditional visibility
    private LinearLayout layoutDate;
    private LinearLayout layoutPlantingDetails;
    private TextInputLayout dateTextInputLayout;

    private Calendar selectedDateCalendar = Calendar.getInstance();

    public interface OnBlockAddedListener {
        void onBlockAdded();
    }

    private OnBlockAddedListener listener;

    public static BlockDialog newInstance(long farmId) {
        BlockDialog dialog = new BlockDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnBlockAddedListener(OnBlockAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
        }

        database = AppDatabase.getInstance(requireContext());

        // Load farm data
        new Thread(() -> {
            currentFarm = database.farmDao().getFirstFarm();
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_block, null);

        initViews(view);
        setupStatusDropdown();
        setupDatePicker();
        setupConditionalFields();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        // Set up button clicks after dialog is created
        dialog.setOnShowListener(dialogInterface -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            // We're using custom buttons, so hide the default ones
            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
        });

        return dialog;
    }

    private void initViews(View view) {
        etBlockName = view.findViewById(R.id.etBlockName);
        etHectareSize = view.findViewById(R.id.etHectareSize);
        etStatus = view.findViewById(R.id.etStatus);

        // Layout containers for conditional visibility
        layoutDate = view.findViewById(R.id.layoutDate);
        layoutPlantingDetails = view.findViewById(R.id.layoutPlantingDetails);

        // Date field (now inside layoutDate)
        etDate = view.findViewById(R.id.etDate);
        dateTextInputLayout = view.findViewById(R.id.layoutDate);

        // Planting details fields
        etSurvivalRate = view.findViewById(R.id.etSurvivalRate);
        etReplacementCount = view.findViewById(R.id.etReplacementCount);
        etNotes = view.findViewById(R.id.etNotes);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveBlock());

        // Set default date to today
        updateDateLabel();
    }

    private void setupStatusDropdown() {
        String[] statuses = new String[]{
                BlockEntity.BlockStatus.NOT_CLEARED.getDisplayName(),
                BlockEntity.BlockStatus.CLEARED.getDisplayName(),
                BlockEntity.BlockStatus.PLOWED.getDisplayName(),
                BlockEntity.BlockStatus.PLANTED.getDisplayName()
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                statuses
        );

        etStatus.setAdapter(adapter);
        etStatus.setText(statuses[0], false); // Default to first item
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateCalendar.set(Calendar.YEAR, year);
                    selectedDateCalendar.set(Calendar.MONTH, month);
                    selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel();
                },
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
        );

        // Can't set future date
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etDate.setText(sdf.format(selectedDateCalendar.getTime()));
    }

    private void setupConditionalFields() {
        etStatus.setOnItemClickListener((parent, view, position, id) -> {
            String selectedStatus = (String) parent.getItemAtPosition(position);

            BlockEntity.BlockStatus status = null;
            for (BlockEntity.BlockStatus s : BlockEntity.BlockStatus.values()) {
                if (s.getDisplayName().equals(selectedStatus)) {
                    status = s;
                    break;
                }
            }

            if (status != null) {
                updateUIBasedOnStatus(status);
            }
        });
    }

    private void updateUIBasedOnStatus(BlockEntity.BlockStatus status) {
        boolean isPlanted = status == BlockEntity.BlockStatus.PLANTED;
        boolean isPrePlanting = status == BlockEntity.BlockStatus.CLEARED ||
                status == BlockEntity.BlockStatus.PLOWED;

        // Handle date layout visibility
        if (isPrePlanting || isPlanted) {
            layoutDate.setVisibility(View.VISIBLE);

            // Update hint based on status
            if (isPrePlanting) {
                dateTextInputLayout.setHint(status.getDisplayName() + " Date");
            } else {
                dateTextInputLayout.setHint("Planting Date");
            }
        } else {
            layoutDate.setVisibility(View.GONE);
        }

        // Handle planting details visibility
        layoutPlantingDetails.setVisibility(isPlanted ? View.VISIBLE : View.GONE);
    }

    private void saveBlock() {
        String blockName = etBlockName.getText().toString().trim();
        String hectareSizeStr = etHectareSize.getText().toString().trim();
        String statusDisplay = etStatus.getText().toString().trim();
        String survivalRateStr = etSurvivalRate.getText().toString().trim();
        String replacementCountStr = etReplacementCount.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Validate block name
        if (TextUtils.isEmpty(blockName)) {
            etBlockName.setError("Block name is required");
            return;
        }

        // Validate hectare size
        if (TextUtils.isEmpty(hectareSizeStr)) {
            etHectareSize.setError("Hectare size is required");
            return;
        }

        double hectareSize;
        try {
            hectareSize = Double.parseDouble(hectareSizeStr);
            if (hectareSize <= 0) {
                etHectareSize.setError("Hectare size must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            etHectareSize.setError("Invalid hectare size");
            return;
        }

        // Get status enum from display name
        BlockEntity.BlockStatus status = null;
        for (BlockEntity.BlockStatus s : BlockEntity.BlockStatus.values()) {
            if (s.getDisplayName().equals(statusDisplay)) {
                status = s;
                break;
            }
        }

        if (status == null) {
            Toast.makeText(getContext(), "Invalid status selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate date is present for statuses that require it
        if (status != BlockEntity.BlockStatus.NOT_CLEARED) {
            if (etDate.getText() == null || TextUtils.isEmpty(etDate.getText().toString())) {
                Toast.makeText(getContext(), "Date is required for " + status.getDisplayName(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        double survivalRate = 0;
        int replacementCount = 0;

        // Validate planting details if status is PLANTED
        if (status == BlockEntity.BlockStatus.PLANTED) {

            if (TextUtils.isEmpty(survivalRateStr)) {
                etSurvivalRate.setError("Survival rate is required");
                return;
            }

            try {
                survivalRate = Double.parseDouble(survivalRateStr);
                if (survivalRate < 0 || survivalRate > 100) {
                    etSurvivalRate.setError("Survival rate must be between 0 and 100");
                    return;
                }
            } catch (NumberFormatException e) {
                etSurvivalRate.setError("Invalid survival rate");
                return;
            }

            try {
                replacementCount = TextUtils.isEmpty(replacementCountStr)
                        ? 0
                        : Integer.parseInt(replacementCountStr);
                if (replacementCount < 0) {
                    etReplacementCount.setError("Replacement count cannot be negative");
                    return;
                }
            } catch (NumberFormatException e) {
                etReplacementCount.setError("Invalid replacement count");
                return;
            }
        }

        long selectedDate = selectedDateCalendar.getTimeInMillis();

        Toast.makeText(getContext(), "Saving block...", Toast.LENGTH_SHORT).show();

        final double finalSurvivalRate = survivalRate;
        final int finalReplacementCount = replacementCount;
        final BlockEntity.BlockStatus finalStatus = status;

        new Thread(() -> {
            // Create new block with basic info
            BlockEntity block = new BlockEntity(
                    farmId,
                    blockName,
                    hectareSize,
                    notes.isEmpty() ? null : notes
            );

            // Set status and dates based on selection
            block.setStatus(finalStatus);

            switch (finalStatus) {
                case CLEARED:
                    block.setClearedDate(selectedDate);
                    break;
                case PLOWED:
                    block.setPlowedDate(selectedDate);
                    break;
                case PLANTED:
                    block.setPlantedDate(selectedDate);
                    block.setSurvivalRate(finalSurvivalRate);
                    block.setReplacementCount(finalReplacementCount);
                    break;
                // NOT_CLEARED: no date set
            }

            long id = database.blockDao().insert(block);

            requireActivity().runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(getContext(), "Block added successfully!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onBlockAdded();
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Error adding block", Toast.LENGTH_SHORT).show();
                }
            });

        }).start();
    }
}