package com.afrinuts.farmos.ui.suppliers;

import static android.content.ContentValues.TAG;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordDeliveryDialog extends DialogFragment {

    private static final String ARG_SUPPLIER_ID = "supplier_id";

    private long supplierId;
    private SupplierEntity supplier;
    private SupplierRepository supplierRepository;
    private QualityBatchRepository qualityBatchRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextInputEditText etBatchNumber;
    private TextInputEditText etSupplierName;
    private TextInputEditText etDeliveryDate;
    private TextInputEditText etQuantity;
    private TextInputEditText etPricePerKg;
    private MaterialAutoCompleteTextView etSourceType;  // Added
    private MaterialAutoCompleteTextView etBatchType;
    private TextInputEditText etNotes;

    private SwitchCompat switchNoChildLabor;
    private SwitchCompat switchFarmOsMonitored;
    private SwitchCompat switchFirebreakProtection;
    private SwitchCompat switchSustainableHarvesting;
    private TextView tvVerifiedStatus;

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnDeliveryRecordedListener {
        void onDeliveryRecorded();
    }

    private OnDeliveryRecordedListener listener;

    public static RecordDeliveryDialog newInstance(long supplierId) {
        RecordDeliveryDialog dialog = new RecordDeliveryDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_SUPPLIER_ID, supplierId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnDeliveryRecordedListener(OnDeliveryRecordedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            supplierId = getArguments().getLong(ARG_SUPPLIER_ID);
        }

        long farmId = 1; // TODO: Get from current farm context
        supplierRepository = new SupplierRepository(requireContext(), farmId);
        qualityBatchRepository = new QualityBatchRepository(requireContext(), farmId);

        // Load supplier details
        executor.execute(() -> {
            supplier = supplierRepository.getSupplierByIdSync(supplierId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (supplier != null) {
                        if (etSupplierName != null) {
                            etSupplierName.setText(supplier.getName());
                        }
                        generateBatchNumber();
                    }
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_record_delivery, null);

        initViews(view);
        setupDatePicker();
        setupSourceTypeDropdown();  // Added
        setupBatchTypeDropdown();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
        });

        return dialog;
    }

    private void initViews(View view) {
        etBatchNumber = view.findViewById(R.id.etBatchNumber);
        etSupplierName = view.findViewById(R.id.etSupplierName);
        etDeliveryDate = view.findViewById(R.id.etDeliveryDate);
        etQuantity = view.findViewById(R.id.etQuantity);
        etPricePerKg = view.findViewById(R.id.etPricePerKg);
        etSourceType = view.findViewById(R.id.etSourceType);  // Added
        etBatchType = view.findViewById(R.id.etBatchType);
        etNotes = view.findViewById(R.id.etNotes);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        // In initViews() - add after other view initializations
        switchNoChildLabor = view.findViewById(R.id.switchNoChildLabor);
        switchFarmOsMonitored = view.findViewById(R.id.switchFarmOsMonitored);
        switchFirebreakProtection = view.findViewById(R.id.switchFirebreakProtection);
        switchSustainableHarvesting = view.findViewById(R.id.switchSustainableHarvesting);

// Set Farm OS Monitored to true by default and disabled
        switchFarmOsMonitored.setChecked(true);
        switchFarmOsMonitored.setEnabled(false);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> recordDelivery());

        // Regenerate batch number when source or type changes
        etSourceType.setOnItemClickListener((parent, view1, position, id) -> {
            generateBatchNumber();
        });

        etBatchType.setOnItemClickListener((parent, view1, position, id) -> {
            generateBatchNumber();
        });

        updateDateLabel();
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

        // ✅ If supplier is OWN_FARM, auto-select OWN_FARM source type
        if (supplier != null && supplier.getType() == SupplierEntity.SupplierType.OWN_FARM) {
            etSourceType.setText("Own Farm", false);
        } else {
            etSourceType.setText(typeNames[0], false);
        }
    }
    private void generateBatchNumber() {
        if (supplier == null) {
            String timestamp = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new java.util.Date());
            etBatchNumber.setText("SUP-" + timestamp + "-" + System.currentTimeMillis() % 1000);
            return;
        }

        // Get source code from Source Type dropdown
        String sourceTypeStr = etSourceType.getText().toString().trim();
        String sourceCode = getSourceCode(sourceTypeStr);

        // Get batch type (organic/conventional)
        String batchTypeStr = etBatchType.getText().toString().trim();
        String typeCode = getTypeCode(batchTypeStr);

        if (sourceCode != null && typeCode != null) {
            etBatchNumber.setText(BatchNumberGenerator.generateBatchNumber(sourceCode, typeCode));
        } else {
            String timestamp = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new java.util.Date());
            etBatchNumber.setText("SUP-" + timestamp + "-" + System.currentTimeMillis() % 1000);
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

    private void setupBatchTypeDropdown() {
        String[] types = {"Organic", "Conventional"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                types
        );

        etBatchType.setAdapter(adapter);
        etBatchType.setText(types[0], false);
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

    private void recordDelivery() {
        String batchNumber = etBatchNumber.getText().toString().trim();
        if (TextUtils.isEmpty(batchNumber)) {
            etBatchNumber.setError("Batch number required");
            return;
        }

        String quantityStr = etQuantity.getText().toString().trim();
        if (TextUtils.isEmpty(quantityStr)) {
            etQuantity.setError("Quantity required");
            return;
        }

        String priceStr = etPricePerKg.getText().toString().trim();
        if (TextUtils.isEmpty(priceStr)) {
            etPricePerKg.setError("Price required");
            return;
        }

        try {
            double quantity = Double.parseDouble(quantityStr);
            double price = Double.parseDouble(priceStr);
            String notes = etNotes.getText().toString().trim();

            if (quantity <= 0) {
                etQuantity.setError("Quantity must be positive");
                return;
            }

            if (price <= 0) {
                etPricePerKg.setError("Price must be positive");
                return;
            }

            // Get SourceType from dropdown
            String sourceTypeStr = etSourceType.getText().toString().trim();
            QualityBatchEntity.SourceType sourceType = getSourceTypeFromDisplay(sourceTypeStr);

            // ✅ Get Batch Type from dropdown
            String batchTypeStr = etBatchType.getText().toString().trim();
            String batchTypeCode = getBatchTypeCode(batchTypeStr);

            boolean noChildLabor = switchNoChildLabor.isChecked();
            boolean farmOsMonitored = switchFarmOsMonitored.isChecked();
            boolean firebreakProtection = switchFirebreakProtection.isChecked();
            boolean sustainableHarvesting = switchSustainableHarvesting.isChecked();
            long verifiedAt = System.currentTimeMillis();

            QualityBatchEntity batch = new QualityBatchEntity(
                    1, // farmId
                    null, // blockId
                    supplierId,
                    sourceType,
                    batchNumber,
                    selectedDate.getTimeInMillis(),
                    0, // moisture - will be filled during quality processing
                    0, // nutCount - will be filled during quality processing
                    0, // defectiveRate - will be filled during quality processing
                    0, // kernelRatio - will be filled during quality processing
                    quantity, // weightKg
                    QualityBatchEntity.QualityGrade.STANDARD,
                    notes
            );

            // ✅ Generate verification code using the batch type code
            String verificationCode = BatchNumberGenerator.generateVerificationCode(
                    sourceType.name(),
                    batchTypeCode,  // ✅ Use the code, not the display name
                    new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(selectedDate.getTime())
            );
            batch.setVerificationCode(verificationCode);

            // ✅ Set sustainability fields
            batch.setNoChildLabor(noChildLabor);
            batch.setFarmOsMonitored(farmOsMonitored);
            batch.setFirebreakProtection(firebreakProtection);
            batch.setSustainableHarvesting(sustainableHarvesting);
            batch.setVerifiedAt(verifiedAt);

            batch.setSynced(false);

            qualityBatchRepository.insertBatch(batch, new QualityBatchRepository.InsertCallback() {
                @Override
                public void onSuccess(long id) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Delivery recorded!", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onDeliveryRecorded();
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

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Add this helper method to get batch type code
    private String getBatchTypeCode(String typeDisplay) {
        if (TextUtils.isEmpty(typeDisplay)) return "ORG";
        if (typeDisplay.contains("Organic")) return "ORG";
        if (typeDisplay.contains("Conventional")) return "CONV";
        return "ORG";
    }

    private QualityBatchEntity.SourceType getSourceTypeFromDisplay(String display) {
        for (QualityBatchEntity.SourceType type : QualityBatchEntity.SourceType.values()) {
            if (display.contains(type.getDisplayName())) {
                return type;
            }
        }
        return QualityBatchEntity.SourceType.COOPERATIVE;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (supplierRepository != null) {
            supplierRepository.shutdown();
        }
        if (qualityBatchRepository != null) {
            qualityBatchRepository.shutdown();
        }
        executor.shutdown();
    }
}