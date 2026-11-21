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
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditHarvestDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {

    private static final String TAG = "EditHarvestDialog";
    private static final String ARG_BLOCK_ID = "block_id";
    private static final String ARG_RECORD_INDEX = "record_index";

    private long blockId;
    private int recordIndex;
    private BlockEntity block;
    private BlockEntity.HarvestRecord originalRecord;
    private BlockRepository blockRepository;
    private AppDatabase database;

    private TextInputEditText etHarvestDate;
    private TextInputEditText etQuantity;
    private MaterialAutoCompleteTextView etGrade;
    private TextInputEditText etNotes;

    private Button btnSave;
    private Button btnCancel;
    private Button btnDelete;

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnHarvestUpdatedListener {
        void onHarvestUpdated(BlockEntity updatedBlock);
    }

    private OnHarvestUpdatedListener listener;

    public static EditHarvestDialog newInstance(long blockId, int recordIndex) {
        EditHarvestDialog dialog = new EditHarvestDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_BLOCK_ID, blockId);
        args.putInt(ARG_RECORD_INDEX, recordIndex);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnHarvestUpdatedListener(OnHarvestUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            blockId = getArguments().getLong(ARG_BLOCK_ID);
            recordIndex = getArguments().getInt(ARG_RECORD_INDEX);
        }

        database = AppDatabase.getInstance(requireContext());
        blockRepository = new BlockRepository(requireContext(), 1);

        // Load block
        new Thread(() -> {
            block = blockRepository.getBlockByIdSync(blockId);
            if (block != null && getActivity() != null) {
                List<BlockEntity.HarvestRecord> records = block.getHarvestRecords();
                if (recordIndex >= 0 && recordIndex < records.size()) {
                    originalRecord = records.get(recordIndex);
                    getActivity().runOnUiThread(() -> populateFields());
                } else {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Record not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            }
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_harvest, null);

        initViews(view);
        setupDatePicker();
        setupGradeDropdown();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle("Edit Harvest Record")
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        return dialog;
    }

    private void initViews(View view) {
        etHarvestDate = view.findViewById(R.id.etHarvestDate);
        etQuantity = view.findViewById(R.id.etQuantity);
        etGrade = view.findViewById(R.id.etGrade);
        etNotes = view.findViewById(R.id.etNotes);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveHarvest());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populateFields() {
        if (originalRecord == null) return;

        // Set date
        selectedDate.setTimeInMillis(originalRecord.getHarvestDate());
        updateDateLabel();

        // Set quantity
        etQuantity.setText(String.valueOf(originalRecord.getQuantityKg()));

        // Set grade
        if (!TextUtils.isEmpty(originalRecord.getGrade())) {
            etGrade.setText(originalRecord.getGrade(), false);
        }

        // Set notes
        if (originalRecord.getNotes() != null) {
            etNotes.setText(originalRecord.getNotes());
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

    private void setupGradeDropdown() {
        String[] grades = {"Premium", "Standard", "Processing"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                grades
        );
        etGrade.setAdapter(adapter);
    }

    private void saveHarvest() {
        if (block == null || originalRecord == null) {
            Toast.makeText(getContext(), "Block not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate inputs
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

        // Create updated record
        BlockEntity.HarvestRecord updatedRecord = new BlockEntity.HarvestRecord(
                selectedDate.getTimeInMillis(),
                quantity,
                grade,
                notes.isEmpty() ? null : notes
        );

        // Update the record
        block.updateHarvestRecord(recordIndex, updatedRecord);
        block.setUpdatedAt(System.currentTimeMillis());

        // Show loading
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Save to database
        new Thread(() -> {
            try {
                // Save to Room
                database.blockDao().update(block);

                // Sync to Firestore
                blockRepository.updateBlock(block)
                        .thenRun(() -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    btnSave.setEnabled(true);
                                    btnSave.setText("Save Changes");

                                    Toast.makeText(getContext(),
                                            "Harvest updated successfully!",
                                            Toast.LENGTH_LONG).show();

                                    if (listener != null) {
                                        listener.onHarvestUpdated(block);
                                    }
                                    dismiss();
                                });
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "Error saving harvest", throwable);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    btnSave.setEnabled(true);
                                    btnSave.setText("Save Changes");
                                    Toast.makeText(getContext(),
                                            "Error: " + throwable.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                            return null;
                        });
            } catch (Exception e) {
                Log.e(TAG, "Exception saving harvest", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Harvest Record")
                .setMessage("Are you sure you want to delete this harvest record?\n\n" +
                        "Quantity: " + originalRecord.getQuantityKg() + " kg\n" +
                        "Date: " + originalRecord.getFormattedDate() + "\n" +
                        "This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteHarvest())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteHarvest() {
        if (block == null) return;

        block.deleteHarvestRecord(recordIndex);
        block.setUpdatedAt(System.currentTimeMillis());

        btnDelete.setEnabled(false);
        btnDelete.setText("Deleting...");

        new Thread(() -> {
            try {
                database.blockDao().update(block);

                blockRepository.updateBlock(block)
                        .thenRun(() -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(),
                                            "Harvest record deleted",
                                            Toast.LENGTH_LONG).show();
                                    if (listener != null) {
                                        listener.onHarvestUpdated(block);
                                    }
                                    dismiss();
                                });
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "Error deleting harvest", throwable);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    btnDelete.setEnabled(true);
                                    btnDelete.setText("Delete");
                                    Toast.makeText(getContext(),
                                            "Error: " + throwable.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                            return null;
                        });
            } catch (Exception e) {
                Log.e(TAG, "Exception deleting harvest", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnDelete.setEnabled(true);
                        btnDelete.setText("Delete");
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
