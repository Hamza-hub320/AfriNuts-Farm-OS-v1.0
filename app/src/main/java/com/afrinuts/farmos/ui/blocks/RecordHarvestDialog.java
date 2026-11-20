package com.afrinuts.farmos.ui.blocks;

import android.app.DatePickerDialog;
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
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class RecordHarvestDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {

    private static final String TAG = "RecordHarvestDialog";
    private static final String ARG_BLOCK_ID = "block_id";

    private long blockId;
    private BlockEntity block;
    private BlockRepository blockRepository;
    private AppDatabase database;

    private TextInputEditText etHarvestDate;
    private TextInputEditText etQuantity;
    private MaterialAutoCompleteTextView etGrade;  // ✅ Changed to MaterialAutoCompleteTextView
    private TextInputEditText etNotes;

    private Button btnSave;
    private Button btnCancel;

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnHarvestRecordedListener {
        void onHarvestRecorded(BlockEntity updatedBlock);
    }

    private OnHarvestRecordedListener listener;

    public static RecordHarvestDialog newInstance(long blockId) {
        RecordHarvestDialog dialog = new RecordHarvestDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_BLOCK_ID, blockId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnHarvestRecordedListener(OnHarvestRecordedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            blockId = getArguments().getLong(ARG_BLOCK_ID);
        }

        database = AppDatabase.getInstance(requireContext());
        blockRepository = new BlockRepository(requireContext(), 1);

        // Load block
        new Thread(() -> {
            block = blockRepository.getBlockByIdSync(blockId);
            if (block != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Pre-fill date if block has last harvest date
                    if (block.getLastHarvestDate() != null && block.getLastHarvestDate() > 0) {
                        selectedDate.setTimeInMillis(block.getLastHarvestDate());
                        updateDateLabel();
                    }
                });
            }
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_record_harvest, null);

        initViews(view);
        setupDatePicker();
        setupGradeDropdown();  // ✅ Added

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle("Record Harvest")
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnSaveDialog = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancelDialog = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnSaveDialog.setVisibility(View.GONE);
            btnCancelDialog.setVisibility(View.GONE);

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
            view.findViewById(R.id.btnSave).setOnClickListener(v -> saveHarvest());
        });

        return dialog;
    }

    private void initViews(View view) {
        etHarvestDate = view.findViewById(R.id.etHarvestDate);
        etQuantity = view.findViewById(R.id.etQuantity);
        etGrade = view.findViewById(R.id.etGrade);
        etNotes = view.findViewById(R.id.etNotes);

        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);

        // Set default date
        updateDateLabel();

        // ✅ Set default grade if available
        if (block != null && block.getHarvestCount() > 0) {
            // Pre-fill grade from last harvest
        }
    }

    private void setupDatePicker() {
        etHarvestDate.setOnClickListener(v -> showDatePicker());
        etHarvestDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker();
        });
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
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etHarvestDate.setText(sdf.format(selectedDate.getTime()));
    }

    // ✅ Added grade dropdown setup
    private void setupGradeDropdown() {
        String[] grades = {"Premium", "Standard", "Processing"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                grades
        );
        etGrade.setAdapter(adapter);
        etGrade.setText(grades[0], false);
    }

    private void saveHarvest() {
        if (block == null) {
            Toast.makeText(getContext(), "Block not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Validate inputs
        String quantityStr = etQuantity.getText().toString().trim();
        if (TextUtils.isEmpty(quantityStr)) {
            etQuantity.setError("Harvest quantity is required");
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityStr);
            if (quantity <= 0) {
                etQuantity.setError("Quantity must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            etQuantity.setError("Invalid quantity");
            return;
        }

        String grade = etGrade.getText().toString().trim();
        if (TextUtils.isEmpty(grade)) {
            grade = "Standard";
        }

        String notes = etNotes.getText().toString().trim();

        // ✅ Log current state before update
        Log.d(TAG, "=== BEFORE HARVEST ===");
        Log.d(TAG, "Block: " + block.getBlockName());
        Log.d(TAG, "Current harvest count: " + block.getHarvestCount());
        Log.d(TAG, "Current total: " + block.getTotalHarvestQuantity());
        Log.d(TAG, "Current harvestHistory: " + block.getHarvestHistory());

        BlockEntity.HarvestRecord harvest = new BlockEntity.HarvestRecord(
                selectedDate.getTimeInMillis(),
                quantity,
                grade,
                notes.isEmpty() ? null : notes
        );

        // ✅ Add to block
        block.addHarvestRecord(harvest);
        block.setStatus(BlockEntity.BlockStatus.HARVESTED);
        block.setUpdatedAt(System.currentTimeMillis());

        // ✅ Show loading
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        new Thread(() -> {
            try {
                // ✅ 1. Save to Room
                database.blockDao().update(block);
                Log.d(TAG, "✅ Saved to Room");

                // ✅ 2. Convert to map and save to Firestore DIRECTLY
                Map<String, Object> blockMap = EntityMapper.blockToMap(block);

                // ✅ 3. Get FirebaseManager and save
                FirebaseManager firebaseManager = FirebaseManager.getInstance();
                firebaseManager.saveBlockMap(blockMap, String.valueOf(block.getFarmId()), block.getId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✅ Saved to Firestore successfully");
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    btnSave.setEnabled(true);
                                    btnSave.setText("Record Harvest");
                                    Toast.makeText(getContext(),
                                            "Harvest recorded: " + String.format(Locale.getDefault(), "%.0f kg", quantity),
                                            Toast.LENGTH_LONG).show();
                                    if (listener != null) {
                                        listener.onHarvestRecorded(block);
                                    }
                                    dismiss();
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Failed to save to Firestore", e);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    btnSave.setEnabled(true);
                                    btnSave.setText("Record Harvest");
                                    Toast.makeText(getContext(),
                                            "Saved locally but failed to sync: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    if (listener != null) {
                                        listener.onHarvestRecorded(block);
                                    }
                                    dismiss();
                                });
                            }
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error saving harvest", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Record Harvest");
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (blockRepository != null) {
            blockRepository.shutdown();
        }
    }
}