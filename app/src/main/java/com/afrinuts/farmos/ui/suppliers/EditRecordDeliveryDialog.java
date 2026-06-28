package com.afrinuts.farmos.ui.suppliers;

import android.app.DatePickerDialog;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.utils.BatchNumberGenerator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditRecordDeliveryDialog extends DialogFragment {

    private static final String ARG_BATCH_ID = "batch_id";

    private long batchId;
    private QualityBatchEntity batch;
    private SupplierEntity supplier;
    private QualityBatchRepository qualityBatchRepository;
    private SupplierRepository supplierRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextInputEditText etBatchNumber;
    private TextInputEditText etSupplierName;
    private TextInputEditText etDeliveryDate;
    private TextInputEditText etQuantity;
    private TextInputEditText etPricePerKg;
    private MaterialAutoCompleteTextView etGrade;
    private MaterialAutoCompleteTextView etSourceType;
    private MaterialAutoCompleteTextView etBatchType;
    private TextInputEditText etNotes;

    private SwitchCompat switchNoChildLabor;
    private SwitchCompat switchFarmOsMonitored;
    private SwitchCompat switchFirebreakProtection;
    private SwitchCompat switchSustainableHarvesting;

    private Calendar selectedDate = Calendar.getInstance();

    // Store current values for comparison
    private String currentBatchNumber;

    public interface OnDeliveryUpdatedListener {
        void onDeliveryUpdated();
    }

    private OnDeliveryUpdatedListener listener;

    public static EditRecordDeliveryDialog newInstance(long batchId) {
        EditRecordDeliveryDialog dialog = new EditRecordDeliveryDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_BATCH_ID, batchId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnDeliveryUpdatedListener(OnDeliveryUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            batchId = getArguments().getLong(ARG_BATCH_ID);
        }

        long farmId = 1; // TODO: Get from current farm context
        qualityBatchRepository = new QualityBatchRepository(requireContext(), farmId);
        supplierRepository = new SupplierRepository(requireContext(), farmId);

        // Load batch and supplier details
        executor.execute(() -> {
            batch = qualityBatchRepository.getBatchByIdSync(batchId);
            if (batch != null && batch.getSupplierId() != null) {
                supplier = supplierRepository.getSupplierByIdSync(batch.getSupplierId());
                currentBatchNumber = batch.getBatchNumber();
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (batch != null) {
                        if (etSupplierName != null && supplier != null) {
                            etSupplierName.setText(supplier.getName());
                        }
                        populateFields();
                    } else {
                        Toast.makeText(getContext(), "Batch not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_record_delivery, null);

        initViews(view);
        setupDatePicker();
        setupGradeDropdown();
        setupSourceTypeDropdown();
        setupBatchTypeDropdown();

        // Add listeners to regenerate batch number when source or type changes
        etSourceType.setOnItemClickListener((parent, view1, position, id) -> {
            regenerateBatchNumber();
        });

        etBatchType.setOnItemClickListener((parent, view1, position, id) -> {
            regenerateBatchNumber();
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)

                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
            view.findViewById(R.id.btnSave).setOnClickListener(v -> updateDelivery());
        });

        return dialog;
    }

    private void initViews(View view) {
        etBatchNumber = view.findViewById(R.id.etBatchNumber);
        etSupplierName = view.findViewById(R.id.etSupplierName);
        etDeliveryDate = view.findViewById(R.id.etDeliveryDate);
        etQuantity = view.findViewById(R.id.etQuantity);
        etPricePerKg = view.findViewById(R.id.etPricePerKg);
        etGrade = view.findViewById(R.id.etGrade);
        etSourceType = view.findViewById(R.id.etSourceType);
        etBatchType = view.findViewById(R.id.etBatchType);
        etNotes = view.findViewById(R.id.etNotes);

        switchNoChildLabor = view.findViewById(R.id.switchNoChildLabor);
        switchFarmOsMonitored = view.findViewById(R.id.switchFarmOsMonitored);
        switchFirebreakProtection = view.findViewById(R.id.switchFirebreakProtection);
        switchSustainableHarvesting = view.findViewById(R.id.switchSustainableHarvesting);
    }

    private void setupSourceTypeDropdown() {
        QualityBatchEntity.SourceType[] types = QualityBatchEntity.SourceType.values();
        String[] typeNames = new String[types.length];

        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getDisplayName(); // Remove icon emoji
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
        );

        etSourceType.setAdapter(adapter);
    }
    private void setupBatchTypeDropdown() {
        String[] types = {"Organic", "Conventional"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                types
        );
        etBatchType.setAdapter(adapter);
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

    private void populateFields() {
        if (batch == null) return;

        // Batch number (read-only)
        currentBatchNumber = batch.getBatchNumber();
        etBatchNumber.setText(currentBatchNumber);
        etBatchNumber.setEnabled(false);

        // Delivery date
        selectedDate.setTimeInMillis(batch.getProcessingDate());
        updateDateLabel();

        // Quantity
        etQuantity.setText(String.valueOf(batch.getWeightKg()));

        // Grade (without emoji)
        etGrade.setText(batch.getQualityGrade().getDisplayName(), false);

        // Source Type (without emoji)
        if (batch.getSourceType() != null) {
            etSourceType.setText(batch.getSourceType().getDisplayName(), false);
        }

        // Batch Type from batch number
        String batchNumber = batch.getBatchNumber();
        if (batchNumber.contains("-ORG-")) {
            etBatchType.setText("Organic", false);
        } else if (batchNumber.contains("-CONV-")) {
            etBatchType.setText("Conventional", false);
        }

        // Notes
        etNotes.setText(batch.getNotes());

        switchNoChildLabor.setChecked(batch.isNoChildLabor());
        switchFarmOsMonitored.setChecked(batch.isFarmOsMonitored());
        switchFirebreakProtection.setChecked(batch.isFirebreakProtection());
        switchSustainableHarvesting.setChecked(batch.isSustainableHarvesting());
    }

    private void regenerateBatchNumber() {
        // Get source code from Source Type dropdown
        String sourceTypeStr = etSourceType.getText().toString().trim();
        String sourceCode = getSourceCode(sourceTypeStr);

        // Get batch type code from Batch Type dropdown
        String batchTypeStr = etBatchType.getText().toString().trim();
        String typeCode = getTypeCode(batchTypeStr);

        // Get current date from selected date
        String date = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(selectedDate.getTime());

        if (sourceCode != null && typeCode != null) {
            // Generate new batch number with source, type, and date
            String newBatchNumber = BatchNumberGenerator.generateBatchNumber(sourceCode, typeCode);
            etBatchNumber.setText(newBatchNumber);
        } else {
            // Fallback: generate with just date
            String fallback = "AFN-" + date + "-" + String.format("%03d", System.currentTimeMillis() % 1000);
            etBatchNumber.setText(fallback);
        }
    }

    private String getSourceCode(String sourceDisplay) {
        if (TextUtils.isEmpty(sourceDisplay)) return null;
        if (sourceDisplay.contains("Own Farm")) return "FARM";
        if (sourceDisplay.contains("Cooperative")) return "COOP";
        if (sourceDisplay.contains("Independent")) return "INDP";
        if (sourceDisplay.contains("Regional Supplier")) return "REGS";
        if (sourceDisplay.contains("Processing Center")) return "PROC";
        return null;
    }

    private String getTypeCode(String typeDisplay) {
        if (TextUtils.isEmpty(typeDisplay)) return null;
        if (typeDisplay.contains("Organic")) return "ORG";
        if (typeDisplay.contains("Conventional")) return "CONV";
        return null;
    }

    private void setupDatePicker() {
        etDeliveryDate.setOnClickListener(v -> showDatePicker());
        etDeliveryDate.setOnFocusChangeListener((v, hasFocus) -> {
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
                    // ✅ Regenerate batch number when date changes
                    regenerateBatchNumber();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etDeliveryDate.setText(sdf.format(selectedDate.getTime()));
    }

    private QualityBatchEntity.QualityGrade getGradeFromDisplay(String display) {
        if (display.contains("Premium")) return QualityBatchEntity.QualityGrade.PREMIUM;
        if (display.contains("Standard")) return QualityBatchEntity.QualityGrade.STANDARD;
        if (display.contains("Processing")) return QualityBatchEntity.QualityGrade.PROCESSING;
        return QualityBatchEntity.QualityGrade.STANDARD;
    }

    private QualityBatchEntity.SourceType getSourceTypeFromDisplay(String display) {
        for (QualityBatchEntity.SourceType type : QualityBatchEntity.SourceType.values()) {
            if (display.contains(type.getDisplayName())) {
                return type;
            }
        }
        return QualityBatchEntity.SourceType.COOPERATIVE;
    }

    private void updateDelivery() {
        String quantityStr = etQuantity.getText().toString().trim();
        if (TextUtils.isEmpty(quantityStr)) {
            etQuantity.setError("Quantity required");
            return;
        }

        try {
            double quantity = Double.parseDouble(quantityStr);
            String gradeStr = etGrade.getText().toString().trim();
            String sourceTypeStr = etSourceType.getText().toString().trim();
            String batchTypeStr = etBatchType.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();
            String newBatchNumber = etBatchNumber.getText().toString().trim();

            if (quantity <= 0) {
                etQuantity.setError("Quantity must be positive");
                return;
            }

            // Get new values
            QualityBatchEntity.QualityGrade grade = getGradeFromDisplay(gradeStr);
            QualityBatchEntity.SourceType sourceType = getSourceTypeFromDisplay(sourceTypeStr);

            // Update batch with all changes
            batch.setWeightKg(quantity);
            batch.setQualityGrade(grade);
            batch.setSourceType(sourceType);
            batch.setBatchNumber(newBatchNumber);
            batch.setProcessingDate(selectedDate.getTimeInMillis());
            batch.setNotes(notes);
            batch.setUpdatedAt(System.currentTimeMillis());
            batch.setNoChildLabor(switchNoChildLabor.isChecked());
            batch.setFarmOsMonitored(switchFarmOsMonitored.isChecked());
            batch.setFirebreakProtection(switchFirebreakProtection.isChecked());
            batch.setSustainableHarvesting(switchSustainableHarvesting.isChecked());

            batch.setNoChildLabor(switchNoChildLabor.isChecked());
            batch.setFarmOsMonitored(switchFarmOsMonitored.isChecked());
            batch.setFirebreakProtection(switchFirebreakProtection.isChecked());
            batch.setSustainableHarvesting(switchSustainableHarvesting.isChecked());
            if (switchNoChildLabor.isChecked() || switchFarmOsMonitored.isChecked() ||
                    switchFirebreakProtection.isChecked() || switchSustainableHarvesting.isChecked()) {
                batch.setVerifiedAt(System.currentTimeMillis());
            }
            batch.setVerifiedAt(System.currentTimeMillis());

            executor.execute(() -> {
                qualityBatchRepository.updateBatch(batch, new QualityBatchRepository.RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Delivery updated!", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onDeliveryUpdated();
                                dismiss();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
            });

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (qualityBatchRepository != null) {
            qualityBatchRepository.shutdown();
        }
        if (supplierRepository != null) {
            supplierRepository.shutdown();
        }
        executor.shutdown();
    }
}