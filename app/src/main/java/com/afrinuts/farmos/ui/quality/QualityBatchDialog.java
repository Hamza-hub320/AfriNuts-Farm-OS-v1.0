package com.afrinuts.farmos.ui.quality;

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
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
    private QualityBatchRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

        repository = new QualityBatchRepository(requireContext(), farmId);

        if (isEditMode && batchId != -1) {
            executor.execute(() -> {
                existingBatch = repository.getBatchByIdSync(batchId);
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
        etProcessingDate = view.findViewById(R.id.etProcessingDate);
        etMoistureContent = view.findViewById(R.id.etMoistureContent);
        etNutCount = view.findViewById(R.id.etNutCount);
        etDefectiveRate = view.findViewById(R.id.etDefectiveRate);
        etKernelRatio = view.findViewById(R.id.etKernelRatio);
        etWeightKg = view.findViewById(R.id.etWeightKg);
        etQualityGrade = view.findViewById(R.id.etQualityGrade);
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
            // Generate batch number suggestion
            generateBatchNumber();
        }

        updateDateLabel();
    }

    private void generateBatchNumber() {
        String timestamp = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new java.util.Date());
        etBatchNumber.setText("BATCH-" + timestamp + "-" + System.currentTimeMillis() % 1000);
    }

    private void loadExistingData() {
        if (existingBatch == null) return;

        etBatchNumber.setText(existingBatch.getBatchNumber());
        etBatchNumber.setEnabled(false);
        etMoistureContent.setText(String.valueOf(existingBatch.getMoistureContent()));
        etNutCount.setText(String.valueOf(existingBatch.getNutCountPerKg()));
        etDefectiveRate.setText(String.valueOf(existingBatch.getDefectiveRate()));
        etKernelRatio.setText(String.valueOf(existingBatch.getKernelRatio()));
        etWeightKg.setText(String.valueOf(existingBatch.getWeightKg()));
        etQualityGrade.setText(existingBatch.getQualityGrade().getDisplayName(), false);
        etNotes.setText(existingBatch.getNotes());

        // Load test results
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
            gradeNames[i] = grades[i].getIcon() + " " + grades[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                gradeNames
        );

        etQualityGrade.setAdapter(adapter);
        etQualityGrade.setText(gradeNames[0], false);
    }

    private void setupDatePicker() {
        etProcessingDate.setOnClickListener(v -> showDatePicker());
        etProcessingDate.setOnFocusChangeListener((v, hasFocus) -> {
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
        etProcessingDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void saveBatch() {
        // Validate inputs
        if (!validateInputs()) return;

        try {
            double moisture = Double.parseDouble(etMoistureContent.getText().toString().trim());
            int nutCount = Integer.parseInt(etNutCount.getText().toString().trim());
            double defective = Double.parseDouble(etDefectiveRate.getText().toString().trim());
            double kernel = Double.parseDouble(etKernelRatio.getText().toString().trim());
            double weight = Double.parseDouble(etWeightKg.getText().toString().trim());

            // Get quality grade
            String gradeStr = etQualityGrade.getText().toString().trim();
            QualityBatchEntity.QualityGrade grade = getGradeFromDisplay(gradeStr);

            // Build test results
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

            // Create batch
            QualityBatchEntity batch = new QualityBatchEntity(
                    farmId,
                    null, // blockId - can be added later
                    null, // harvestCycleId - can be added later
                    etBatchNumber.getText().toString().trim(),
                    selectedDate.getTimeInMillis(),
                    moisture,
                    nutCount,
                    defective,
                    kernel,
                    weight,
                    grade,
                    etNotes.getText().toString().trim()
            );
            batch.setTestResults(results.toJson());

            // Save to repository
            repository.insertBatch(batch, new QualityBatchRepository.InsertCallback() {
                @Override
                public void onSuccess(long id) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Quality batch added!", Toast.LENGTH_SHORT).show();
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

    private void updateBatch() {
        // Similar to save but updates existing
        // Implementation similar to saveBatch but uses updateBatch method
    }

    private boolean validateInputs() {
        // Validate all fields
        if (TextUtils.isEmpty(etBatchNumber.getText())) {
            etBatchNumber.setError("Batch number required");
            return false;
        }
        if (TextUtils.isEmpty(etMoistureContent.getText())) {
            etMoistureContent.setError("Moisture content required");
            return false;
        }
        // Add more validations...
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