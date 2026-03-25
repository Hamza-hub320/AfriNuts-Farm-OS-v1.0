package com.afrinuts.farmos.ui.blocks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockDialog extends DialogFragment {

    private String suggestedBlockName;
    private static final String ARG_FARM_ID = "farm_id";

    private long farmId;
    private FarmEntity currentFarm;
    private List<BlockEntity> existingBlocks;
    private BlockRepository blockRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private Button btnCancel;
    private Button btnSave;
    private TextInputEditText etBlockName;
    private TextInputEditText etHectareSize;
    private MaterialAutoCompleteTextView etStatus;
    private TextInputEditText etDate;
    private TextInputEditText etSurvivalRate;
    private TextInputEditText etReplacementCount;
    private TextInputEditText etNotes;

    // New layout containers
    private View dateCard;
    private View plantingDetailsCard;

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

    public void setNextBlockName(String blockName) {
        this.suggestedBlockName = blockName;
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

        // Initialize repository with long farmId
        blockRepository = new BlockRepository(requireContext(), farmId);

        // Load existing blocks in background
        executor.execute(() -> {
            existingBlocks = blockRepository.getAllBlocksSync();
        });
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
                .setTitle("")
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            // We're using custom buttons, so no need to override dialog buttons
        });

        return dialog;
    }

    private void initViews(View view) {
        etBlockName = view.findViewById(R.id.etBlockName);
        if (suggestedBlockName != null) {
            etBlockName.setText(suggestedBlockName);
            etBlockName.setEnabled(false);
            etBlockName.setFocusable(false);
            etBlockName.setBackgroundResource(R.drawable.suggested_block_background);
        }
        etHectareSize = view.findViewById(R.id.etHectareSize);
        etStatus = view.findViewById(R.id.etStatus);

        // New layout references
        dateCard = view.findViewById(R.id.dateCard);
        plantingDetailsCard = view.findViewById(R.id.plantingDetailsCard);
        etDate = view.findViewById(R.id.etDate);
        etSurvivalRate = view.findViewById(R.id.etSurvivalRate);
        etReplacementCount = view.findViewById(R.id.etReplacementCount);
        etNotes = view.findViewById(R.id.etNotes);

        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

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
        etStatus.setText(statuses[0], false);
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

    // Single version of updateUIBasedOnStatus (REMOVED THE DUPLICATE)
    private void updateUIBasedOnStatus(BlockEntity.BlockStatus status) {
        boolean isPlanted = status == BlockEntity.BlockStatus.PLANTED;
        boolean isPrePlanting = status == BlockEntity.BlockStatus.CLEARED ||
                status == BlockEntity.BlockStatus.PLOWED;

        // Handle date card
        if (isPrePlanting || isPlanted) {
            if (dateCard != null && dateCard.getVisibility() != View.VISIBLE) {
                TextView title = getDialog().findViewById(R.id.dateSectionTitle);
                if (title != null) {
                    title.setText(isPrePlanting ?
                            status.getDisplayName() + " Date" : "Planting Date");
                }
                animateCardTransition(null, dateCard);
            }
        } else {
            if (dateCard != null && dateCard.getVisibility() == View.VISIBLE) {
                animateCardTransition(dateCard, null);
            }
        }

        // Handle planting details card
        if (isPlanted) {
            if (plantingDetailsCard != null && plantingDetailsCard.getVisibility() != View.VISIBLE) {
                animateCardTransition(null, plantingDetailsCard);
            }
        } else {
            if (plantingDetailsCard != null && plantingDetailsCard.getVisibility() == View.VISIBLE) {
                animateCardTransition(plantingDetailsCard, null);
            }
        }
    }

    // Animation methods
    private void animateCardTransition(View oldCard, View newCard) {
        if (oldCard != null && oldCard.getVisibility() == View.VISIBLE) {
            oldCard.animate()
                    .alpha(0f)
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        oldCard.setVisibility(View.GONE);
                        if (newCard != null) {
                            showNewCard(newCard);
                        }
                    })
                    .start();
        } else if (newCard != null) {
            showNewCard(newCard);
        }
    }

    private void showNewCard(View card) {
        card.setVisibility(View.VISIBLE);
        card.setAlpha(0f);
        card.setScaleX(0.95f);
        card.setScaleY(0.95f);
        card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private boolean isBlockNameExists(String blockName) {
        if (existingBlocks == null) return false;
        for (BlockEntity block : existingBlocks) {
            if (block.getBlockName().equalsIgnoreCase(blockName)) {
                return true;
            }
        }
        return false;
    }

    private void saveBlock() {
        String blockName = etBlockName.getText().toString().trim();
        String hectareSizeStr = etHectareSize.getText().toString().trim();
        String statusDisplay = etStatus.getText().toString().trim();
        String survivalRateStr = etSurvivalRate.getText().toString().trim();
        String replacementCountStr = etReplacementCount.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(blockName)) {
            etBlockName.setError("Block name is required");
            return;
        }

        // Check for duplicate block name
        if (isBlockNameExists(blockName)) {
            etBlockName.setError("Block " + blockName + " already exists!");
            Toast.makeText(getContext(),
                    "Block " + blockName + " already exists! Please use a different name.",
                    Toast.LENGTH_LONG).show();
            return;
        }

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

        if (status != BlockEntity.BlockStatus.NOT_CLEARED) {
            if (etDate.getText() == null || TextUtils.isEmpty(etDate.getText().toString())) {
                Toast.makeText(getContext(), "Date is required for " + status.getDisplayName(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        double survivalRate = 0;
        int replacementCount = 0;

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
                replacementCount = TextUtils.isEmpty(replacementCountStr) ? 0 : Integer.parseInt(replacementCountStr);
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

        // Create block object
        BlockEntity block = new BlockEntity(farmId, blockName, hectareSize, notes.isEmpty() ? null : notes);
        block.setStatus(status);

        switch (status) {
            case CLEARED:
                block.setClearedDate(selectedDate);
                break;
            case PLOWED:
                block.setPlowedDate(selectedDate);
                break;
            case PLANTED:
                block.setPlantedDate(selectedDate);
                block.setSurvivalRate(survivalRate);
                block.setReplacementCount(replacementCount);
                break;
        }

        // Show loading state
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        blockRepository.insertBlock(block)
                .thenAccept(id -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Block added successfully!", Toast.LENGTH_SHORT).show();
                            if (listener != null) {
                                listener.onBlockAdded();
                            }
                            dismiss();
                        });
                    }
                })
                .exceptionally(throwable -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Save Block");
                            Toast.makeText(getContext(), "Error adding block: " + throwable.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                    return null;
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (blockRepository != null) {
            blockRepository.shutdown();
        }
        executor.shutdown();
    }
}