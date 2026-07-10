package com.afrinuts.farmos.ui.quality;

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
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.utils.BatchNumberGenerator;
import com.afrinuts.farmos.utils.TraceabilityAutoCreator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.widget.SwitchCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QualityBatchDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_EDIT_MODE = "edit_mode";
    private static final String ARG_BATCH_ID = "batch_id";

    private long farmId;
    private boolean isEditMode = false;
    private long batchId = -1;
    private QualityBatchEntity existingBatch;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private QualityBatchRepository qualityBatchRepository;
    private SupplierRepository supplierRepository;

    // UI Elements
    private TextInputEditText etBatchNumber;
    private TextInputEditText etProcessingDate;
    private TextInputEditText etMoistureContent;
    private TextInputEditText etNutCount;
    private TextInputEditText etDefectiveRate;
    private TextInputEditText etKernelRatio;
    private TextInputEditText etWeightKg;
    private MaterialAutoCompleteTextView etQualityGrade;
    private TextInputEditText etAflatoxin;
    private TextInputEditText etPesticideResidue;
    private TextInputEditText etHeavyMetals;
    private TextInputEditText etNotes;

    // Selection fields
    private MaterialAutoCompleteTextView etSupplier;
    private MaterialAutoCompleteTextView etDeliveryBatch;

    // Auto-populated fields (read-only)
    private TextInputEditText etBatchNumberDisplay;
    private TextInputEditText etProcessingDateDisplay;
    private TextInputEditText etWeightKgDisplay;
    private MaterialAutoCompleteTextView etQualityGradeDisplay;
    private MaterialAutoCompleteTextView etSourceTypeDisplay;
    private MaterialAutoCompleteTextView etBatchTypeDisplay;

    private List<SupplierEntity> supplierList;
    private List<QualityBatchEntity> deliveryBatches = new ArrayList<>();

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnBatchAddedListener {
        void onBatchAdded();
    }

    private OnBatchAddedListener listener;

    public static QualityBatchDialog newInstance(long farmId) {
        QualityBatchDialog dialog = new QualityBatchDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        args.putBoolean(ARG_EDIT_MODE, false);
        dialog.setArguments(args);
        return dialog;
    }

    public static QualityBatchDialog newEditInstance(long farmId, long batchId) {
        QualityBatchDialog dialog = new QualityBatchDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        args.putBoolean(ARG_EDIT_MODE, true);
        args.putLong(ARG_BATCH_ID, batchId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnBatchAddedListener(OnBatchAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
            isEditMode = getArguments().getBoolean(ARG_EDIT_MODE, false);
            if (isEditMode) {
                batchId = getArguments().getLong(ARG_BATCH_ID, -1);
            }
        }

        qualityBatchRepository = new QualityBatchRepository(requireContext(), farmId);
        supplierRepository = new SupplierRepository(requireContext(), farmId);

        if (isEditMode && batchId != -1) {
            executor.execute(() -> {
                existingBatch = qualityBatchRepository.getBatchByIdSync(batchId);
                if (existingBatch != null) {
                    selectedDate.setTimeInMillis(existingBatch.getProcessingDate());
                }
            });
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quality_batch, null);

        initViews(view);
        setupDatePicker();
        setupGradeDropdown();
        setupSourceTypeDropdown();
        setupBatchTypeDropdown();
        loadSuppliersForDropdown();

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
        // Selection fields
        etSupplier = view.findViewById(R.id.etSupplier);
        etDeliveryBatch = view.findViewById(R.id.etDeliveryBatch);

        // Read-only display fields (from delivery)
        etBatchNumberDisplay = view.findViewById(R.id.etBatchNumberDisplay);
        etProcessingDateDisplay = view.findViewById(R.id.etProcessingDateDisplay);
        etWeightKgDisplay = view.findViewById(R.id.etWeightKgDisplay);
        etQualityGradeDisplay = view.findViewById(R.id.etQualityGradeDisplay);
        etSourceTypeDisplay = view.findViewById(R.id.etSourceTypeDisplay);
        etBatchTypeDisplay = view.findViewById(R.id.etBatchTypeDisplay);

        // Editable quality fields
        etMoistureContent = view.findViewById(R.id.etMoistureContent);
        etNutCount = view.findViewById(R.id.etNutCount);
        etDefectiveRate = view.findViewById(R.id.etDefectiveRate);
        etKernelRatio = view.findViewById(R.id.etKernelRatio);
        etAflatoxin = view.findViewById(R.id.etAflatoxin);
        etPesticideResidue = view.findViewById(R.id.etPesticideResidue);
        etHeavyMetals = view.findViewById(R.id.etHeavyMetals);
        etNotes = view.findViewById(R.id.etNotes);


        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());

        if (isEditMode) {
            btnSave.setText("Update Batch");
            btnSave.setOnClickListener(v -> updateBatch());
            loadExistingData();
        } else {
            btnSave.setOnClickListener(v -> saveBatch());
        }

        // Load deliveries when supplier is selected
        etSupplier.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedSupplier = (String) parent.getItemAtPosition(position);
            if (supplierList != null) {
                for (SupplierEntity supplier : supplierList) {
                    String displayName = supplier.getName() + " (" +
                            supplier.getType().getDisplayName() + ")";
                    if (displayName.equals(selectedSupplier)) {
                        loadDeliveryBatchesForSupplier(supplier.getId());
                        break;
                    }
                }
            }
        });

        updateDateLabel();
    }

    private void loadDeliveryBatchesForSupplier(long supplierId) {
        executor.execute(() -> {
            if (qualityBatchRepository != null) {
                deliveryBatches = qualityBatchRepository.getBatchesBySupplierIdSync(supplierId);

                if (getActivity() != null) {
                    final List<QualityBatchEntity> finalBatches = deliveryBatches;

                    getActivity().runOnUiThread(() -> {
                        List<String> batchDisplayNames = new ArrayList<>();
                        for (QualityBatchEntity batch : finalBatches) {
                            batchDisplayNames.add(batch.getBatchNumber() + " - " +
                                    batch.getFormattedDate() + " (" + batch.getWeightKg() + "kg)");
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                batchDisplayNames
                        );
                        etDeliveryBatch.setAdapter(adapter);

                        etDeliveryBatch.setOnItemClickListener((parent, view, position, id) -> {
                            QualityBatchEntity selectedBatch = finalBatches.get(position);
                            if (selectedBatch != null) {
                                preFillFromDelivery(selectedBatch);
                            }
                        });
                    });
                }
            }
        });
    }

    private void preFillFromDelivery(QualityBatchEntity deliveryBatch) {
        // Auto-populate read-only fields from delivery
        etBatchNumberDisplay.setText(deliveryBatch.getBatchNumber());
        etProcessingDateDisplay.setText(deliveryBatch.getFormattedDate());
        etWeightKgDisplay.setText(String.valueOf(deliveryBatch.getWeightKg()));

        // Set quality grade (auto-populated from delivery)
        etQualityGradeDisplay.setText(deliveryBatch.getQualityGrade().getDisplayName(), false);

        // ✅ FIXED: Removed getIcon() call
        if (deliveryBatch.getSourceType() != null) {
            etSourceTypeDisplay.setText(deliveryBatch.getSourceType().getDisplayName(), false);
        }

        // Set batch type from batch number
        String batchNumber = deliveryBatch.getBatchNumber();
        if (batchNumber.contains("-ORG-")) {
            etBatchTypeDisplay.setText("Organic", false);
        } else if (batchNumber.contains("-CONV-")) {
            etBatchTypeDisplay.setText("Conventional", false);
        }

        // Set notes from delivery
        etNotes.setText(deliveryBatch.getNotes());

        Toast.makeText(getContext(), "Loaded delivery: " + deliveryBatch.getBatchNumber(), Toast.LENGTH_SHORT).show();
    }

    private void setupBatchTypeDropdown() {
        String[] types = {"Organic", "Conventional"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                types
        );
        etBatchTypeDisplay.setAdapter(adapter);
    }

    private void setupSourceTypeDropdown() {
        QualityBatchEntity.SourceType[] types = QualityBatchEntity.SourceType.values();
        String[] typeNames = new String[types.length];

        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
        );

        etSourceTypeDisplay.setAdapter(adapter);
    }

    private void loadSuppliersForDropdown() {
        executor.execute(() -> {
            if (supplierRepository != null) {
                supplierList = supplierRepository.getSuppliersSync();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        List<String> supplierNames = new ArrayList<>();
                        for (SupplierEntity supplier : supplierList) {
                            supplierNames.add(supplier.getName() + " (" +
                                    supplier.getType().getDisplayName() + ")");
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                supplierNames
                        );
                        etSupplier.setAdapter(adapter);
                    });
                }
            }
        });
    }

    private void loadExistingData() {
        if (existingBatch == null) return;

        // For edit mode, populate all fields
        etBatchNumberDisplay.setText(existingBatch.getBatchNumber());
        etProcessingDateDisplay.setText(existingBatch.getFormattedDate());
        etWeightKgDisplay.setText(String.valueOf(existingBatch.getWeightKg()));

        etMoistureContent.setText(String.valueOf(existingBatch.getMoistureContent()));
        etNutCount.setText(String.valueOf(existingBatch.getNutCountPerKg()));
        etDefectiveRate.setText(String.valueOf(existingBatch.getDefectiveRate()));
        etKernelRatio.setText(String.valueOf(existingBatch.getKernelRatio()));
        etQualityGradeDisplay.setText(existingBatch.getQualityGrade().getDisplayName(), false);
        etNotes.setText(existingBatch.getNotes());

        // ✅ FIXED: Removed getIcon() call
        if (existingBatch.getSourceType() != null) {
            etSourceTypeDisplay.setText(existingBatch.getSourceType().getDisplayName(), false);
        }

        String batchNumber = existingBatch.getBatchNumber();
        if (batchNumber.contains("-ORG-")) {
            etBatchTypeDisplay.setText("Organic", false);
        } else if (batchNumber.contains("-CONV-")) {
            etBatchTypeDisplay.setText("Conventional", false);
        }

        QualityBatchEntity.TestResults results =
                QualityBatchEntity.TestResults.fromJson(existingBatch.getTestResults());
        if (results.hasResults()) {
            etAflatoxin.setText(String.valueOf(results.getAflatoxin()));
            etPesticideResidue.setText(String.valueOf(results.getPesticideResidue()));
            etHeavyMetals.setText(String.valueOf(results.getHeavyMetals()));
        }
    }

    private void setupGradeDropdown() {
        QualityBatchEntity.QualityGrade[] grades = QualityBatchEntity.QualityGrade.values();
        String[] gradeNames = new String[grades.length];

        for (int i = 0; i < grades.length; i++) {
            gradeNames[i] = grades[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                gradeNames
        );

        etQualityGradeDisplay.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etProcessingDateDisplay.setOnClickListener(v -> showDatePicker());
        etProcessingDateDisplay.setOnFocusChangeListener((v, hasFocus) -> {
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
        etProcessingDateDisplay.setText(sdf.format(selectedDate.getTime()));
    }

    private void saveBatch() {
        if (!validateInputs()) return;

        try {
            double moisture = Double.parseDouble(etMoistureContent.getText().toString().trim());
            int nutCount = Integer.parseInt(etNutCount.getText().toString().trim());
            double defective = Double.parseDouble(etDefectiveRate.getText().toString().trim());
            double kernel = Double.parseDouble(etKernelRatio.getText().toString().trim());

            String batchNumber = etBatchNumberDisplay.getText().toString().trim();
            String gradeStr = etQualityGradeDisplay.getText().toString().trim();
            QualityBatchEntity.QualityGrade grade = getGradeFromDisplay(gradeStr);
            double weight = Double.parseDouble(etWeightKgDisplay.getText().toString().trim());

            long verifiedAt = System.currentTimeMillis();

            final String finalBatchNumber = batchNumber;
            final QualityBatchEntity.QualityGrade finalGrade = grade;
            final double finalMoisture = moisture;
            final int finalNutCount = nutCount;
            final double finalDefective = defective;
            final double finalKernel = kernel;
            final double finalWeight = weight;
            final String finalNotes = etNotes.getText().toString().trim();

            QualityBatchEntity.TestResults results = new QualityBatchEntity.TestResults();
            if (!TextUtils.isEmpty(etAflatoxin.getText())) {
                results.setAflatoxin(Double.parseDouble(etAflatoxin.getText().toString().trim()));
            }
            if (!TextUtils.isEmpty(etPesticideResidue.getText())) {
                results.setPesticideResidue(Double.parseDouble(etPesticideResidue.getText().toString().trim()));
            }
            if (!TextUtils.isEmpty(etHeavyMetals.getText())) {
                results.setHeavyMetals(Double.parseDouble(etHeavyMetals.getText().toString().trim()));
            }
            final String finalTestResults = results.toJson();

            executor.execute(() -> {
                QualityBatchEntity existingBatch = qualityBatchRepository.getBatchByNumberSync(finalBatchNumber);

                if (existingBatch != null) {
                    existingBatch.setMoistureContent(finalMoisture);
                    existingBatch.setNutCountPerKg(finalNutCount);
                    existingBatch.setDefectiveRate(finalDefective);
                    existingBatch.setKernelRatio(finalKernel);
                    existingBatch.setQualityGrade(finalGrade);
                    existingBatch.setTestResults(finalTestResults);
                    existingBatch.setNotes(finalNotes);
                    existingBatch.setUpdatedAt(System.currentTimeMillis());

                    qualityBatchRepository.updateBatch(existingBatch, new QualityBatchRepository.RepositoryCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {

                                    TraceabilityAutoCreator autoCreator = new TraceabilityAutoCreator(
                                            getContext(), farmId);
                                    autoCreator.createMissingTraceabilities();

                                    Toast.makeText(getContext(), "Quality metrics updated!", Toast.LENGTH_SHORT).show();
                                    if (listener != null) listener.onBatchAdded();
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
                } else {
                    Toast.makeText(getContext(), "Error: Batch not found. Please select a delivery.", Toast.LENGTH_LONG).show();
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBatch() {
        if (!validateInputs()) return;

        try {
            double moisture = Double.parseDouble(etMoistureContent.getText().toString().trim());
            int nutCount = Integer.parseInt(etNutCount.getText().toString().trim());
            double defective = Double.parseDouble(etDefectiveRate.getText().toString().trim());
            double kernel = Double.parseDouble(etKernelRatio.getText().toString().trim());
            double weight = Double.parseDouble(etWeightKgDisplay.getText().toString().trim());

            String gradeStr = etQualityGradeDisplay.getText().toString().trim();
            QualityBatchEntity.QualityGrade grade = getGradeFromDisplay(gradeStr);

            QualityBatchEntity.TestResults results = new QualityBatchEntity.TestResults();
            if (!TextUtils.isEmpty(etAflatoxin.getText())) {
                results.setAflatoxin(Double.parseDouble(etAflatoxin.getText().toString().trim()));
            }
            if (!TextUtils.isEmpty(etPesticideResidue.getText())) {
                results.setPesticideResidue(Double.parseDouble(etPesticideResidue.getText().toString().trim()));
            }
            if (!TextUtils.isEmpty(etHeavyMetals.getText())) {
                results.setHeavyMetals(Double.parseDouble(etHeavyMetals.getText().toString().trim()));
            }

            // ✅ Get existing batch to preserve sustainability
            final QualityBatchEntity finalExistingBatch = existingBatch;

            QualityBatchEntity updatedBatch = new QualityBatchEntity(
                    farmId,
                    null,
                    null,
                    existingBatch.getSupplierId(),
                    existingBatch.getSourceType(),
                    etBatchNumberDisplay.getText().toString().trim(),
                    selectedDate.getTimeInMillis(),
                    moisture,
                    nutCount,
                    defective,
                    kernel,
                    weight,
                    grade,
                    etNotes.getText().toString().trim()
            );
            updatedBatch.setId(batchId);
            updatedBatch.setTestResults(results.toJson());

            // ✅ Preserve sustainability fields from existing batch
            updatedBatch.setNoChildLabor(existingBatch.isNoChildLabor());
            updatedBatch.setFarmOsMonitored(existingBatch.isFarmOsMonitored());
            updatedBatch.setFirebreakProtection(existingBatch.isFirebreakProtection());
            updatedBatch.setSustainableHarvesting(existingBatch.isSustainableHarvesting());
            updatedBatch.setVerifiedAt(existingBatch.getVerifiedAt());

            qualityBatchRepository.updateBatch(updatedBatch, new QualityBatchRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Batch updated successfully!", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onBatchAdded();
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
            Toast.makeText(getContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private QualityBatchEntity.SourceType getSourceTypeFromDisplay(String display) {
        for (QualityBatchEntity.SourceType type : QualityBatchEntity.SourceType.values()) {
            if (display.contains(type.getDisplayName())) {
                return type;
            }
        }
        return QualityBatchEntity.SourceType.COOPERATIVE;
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etBatchNumberDisplay.getText())) {
            etBatchNumberDisplay.setError("Please select a delivery");
            Toast.makeText(getContext(), "Please select a delivery first", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(etMoistureContent.getText())) {
            etMoistureContent.setError("Moisture content required");
            return false;
        }
        if (TextUtils.isEmpty(etNutCount.getText())) {
            etNutCount.setError("Nut count required");
            return false;
        }
        if (TextUtils.isEmpty(etDefectiveRate.getText())) {
            etDefectiveRate.setError("Defective rate required");
            return false;
        }
        if (TextUtils.isEmpty(etKernelRatio.getText())) {
            etKernelRatio.setError("Kernel ratio required");
            return false;
        }
        if (TextUtils.isEmpty(etQualityGradeDisplay.getText())) {
            etQualityGradeDisplay.setError("Quality grade required");
            return false;
        }
        return true;
    }

    private QualityBatchEntity.QualityGrade getGradeFromDisplay(String display) {
        for (QualityBatchEntity.QualityGrade grade : QualityBatchEntity.QualityGrade.values()) {
            if (display.contains(grade.getDisplayName())) {
                return grade;
            }
        }
        return QualityBatchEntity.QualityGrade.STANDARD;
    }
}