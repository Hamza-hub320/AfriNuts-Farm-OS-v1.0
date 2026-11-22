package com.afrinuts.farmos.ui.traceability;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditShippingDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {

    private static final String ARG_TRACEABILITY_ID = "traceability_id";

    private long traceabilityId;
    private BatchTraceabilityEntity traceability;
    private QualityBatchEntity qualityBatch;
    private SupplierEntity supplier;
    private BatchTraceabilityRepository repository;
    private QualityBatchRepository qualityBatchRepository;
    private SupplierRepository supplierRepository;

    // Shipping fields
    private TextInputEditText etExportDate;
    private TextInputEditText etBuyer;
    private TextInputEditText etDestination;
    private TextInputEditText etHarvestYear;
    private TextInputEditText etCertifications;

    // ✅ Processing fields
    private TextInputEditText etProcessingLocation;
    private TextInputEditText etProcessingFacility;
    private TextInputEditText etProcessingMethod;
    private TextInputEditText etKernelWeight;
    private TextInputEditText etShellingMethod;
    private TextInputEditText etGradingMethod;
    private TextInputEditText etYieldPercentage;

    private Calendar selectedDate = Calendar.getInstance();

    // ✅ Default processing values
    private static final String DEFAULT_PROCESSING_LOCATION = "Odienné, Côte d'Ivoire";
    private static final String DEFAULT_PROCESSING_FACILITY = "AfriNuts NDC (Nuts Distribution Center)";
    private static final String DEFAULT_PROCESSING_METHOD = "Steam roasting — zero chemical additives";
    private static final String DEFAULT_SHELLING_METHOD = "Semi-automated — mechanical and manual";
    private static final String DEFAULT_GRADING_METHOD = "Manual inspection + electronic sorting";

    public interface OnShippingUpdatedListener {
        void onShippingUpdated();
    }

    private OnShippingUpdatedListener listener;

    public static EditShippingDialog newInstance(long traceabilityId) {
        EditShippingDialog dialog = new EditShippingDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_TRACEABILITY_ID, traceabilityId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnShippingUpdatedListener(OnShippingUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            traceabilityId = getArguments().getLong(ARG_TRACEABILITY_ID);
        }

        repository = new BatchTraceabilityRepository(requireContext(), 1);

        // Load data
        new Thread(() -> {
            traceability = repository.getTraceabilityByIdSync(traceabilityId);

            if (traceability != null) {
                if (traceability.getQualityBatchId() != null) {
                    qualityBatchRepository = new QualityBatchRepository(requireContext(), 1);
                    qualityBatch = qualityBatchRepository.getBatchByIdSync(traceability.getQualityBatchId());
                }

                if (traceability.getSupplierId() != null) {
                    supplierRepository = new SupplierRepository(requireContext(), 1);
                    supplier = supplierRepository.getSupplierByIdSync(traceability.getSupplierId());
                }

                if (traceability.getExportDate() != null && traceability.getExportDate() > 0) {
                    selectedDate.setTimeInMillis(traceability.getExportDate());
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(this::populateFields);
            }
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_shipping, null);

        initViews(view);
        setupDatePicker();

        // ✅ Auto-populate processing fields
        autoPopulateProcessingFields();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)

                .setCancelable(false);

        AlertDialog dialog = builder.create();
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> saveShippingDetails());

        return dialog;
    }

    private void initViews(View view) {
        // Shipping fields
        etExportDate = view.findViewById(R.id.etExportDate);
        etBuyer = view.findViewById(R.id.etBuyer);
        etDestination = view.findViewById(R.id.etDestination);
        etHarvestYear = view.findViewById(R.id.etHarvestYear);
        etCertifications = view.findViewById(R.id.etCertifications);

        // ✅ Processing fields
        etProcessingLocation = view.findViewById(R.id.etProcessingLocation);
        etProcessingFacility = view.findViewById(R.id.etProcessingFacility);
        etProcessingMethod = view.findViewById(R.id.etProcessingMethod);
        etKernelWeight = view.findViewById(R.id.etKernelWeight);
        etShellingMethod = view.findViewById(R.id.etShellingMethod);
        etGradingMethod = view.findViewById(R.id.etGradingMethod);
        etYieldPercentage = view.findViewById(R.id.etYieldPercentage);

        // ✅ Listen for kernel weight changes to calculate yield
        etKernelWeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                calculateYield();
            }
        });
    }

    private void setupDatePicker() {
        etExportDate.setOnClickListener(v -> showDatePicker());
        etExportDate.setOnFocusChangeListener((v, hasFocus) -> {
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
        etExportDate.setText(sdf.format(selectedDate.getTime()));
    }

    // ✅ Auto-populate processing fields with default values
    private void autoPopulateProcessingFields() {
        if (traceability != null) {
            // If values exist in traceability, use them, otherwise use defaults
            etProcessingLocation.setText(
                    traceability.getProcessingLocation() != null ?
                            traceability.getProcessingLocation() : DEFAULT_PROCESSING_LOCATION
            );

            etProcessingFacility.setText(
                    traceability.getProcessingFacility() != null ?
                            traceability.getProcessingFacility() : DEFAULT_PROCESSING_FACILITY
            );

            etProcessingMethod.setText(
                    traceability.getProcessingMethod() != null ?
                            traceability.getProcessingMethod() : DEFAULT_PROCESSING_METHOD
            );

            etShellingMethod.setText(
                    traceability.getShellingMethod() != null ?
                            traceability.getShellingMethod() : DEFAULT_SHELLING_METHOD
            );

            etGradingMethod.setText(
                    traceability.getGradingMethod() != null ?
                            traceability.getGradingMethod() : DEFAULT_GRADING_METHOD
            );

            // Kernel weight
            if (traceability.getKernelWeightKg() > 0) {
                etKernelWeight.setText(String.valueOf(traceability.getKernelWeightKg()));
                etYieldPercentage.setText(traceability.getFormattedYield());
            }
        } else {
            // New traceability - use all defaults
            etProcessingLocation.setText(DEFAULT_PROCESSING_LOCATION);
            etProcessingFacility.setText(DEFAULT_PROCESSING_FACILITY);
            etProcessingMethod.setText(DEFAULT_PROCESSING_METHOD);
            etShellingMethod.setText(DEFAULT_SHELLING_METHOD);
            etGradingMethod.setText(DEFAULT_GRADING_METHOD);
        }
    }

    private void calculateYield() {
        String kernelWeightStr = etKernelWeight.getText().toString().trim();
        if (TextUtils.isEmpty(kernelWeightStr) || traceability == null) {
            etYieldPercentage.setText("0%");
            return;
        }

        try {
            double kernelWeight = Double.parseDouble(kernelWeightStr);
            double rawWeight = traceability.getWeightKg();

            if (rawWeight > 0 && kernelWeight > 0) {
                double yield = (kernelWeight / rawWeight) * 100;
                etYieldPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", yield));
            } else {
                etYieldPercentage.setText("0%");
            }
        } catch (NumberFormatException e) {
            etYieldPercentage.setText("Invalid");
        }
    }

    private void populateFields() {
        if (traceability == null) return;

        // Shipping fields
        if (traceability.getExportDate() != null && traceability.getExportDate() > 0) {
            selectedDate.setTimeInMillis(traceability.getExportDate());
            updateDateLabel();
        } else {
            updateDateLabel();
        }

        etBuyer.setText(traceability.getBuyer());
        etDestination.setText(traceability.getDestination());

        if (traceability.getHarvestYear() > 0) {
            etHarvestYear.setText(String.valueOf(traceability.getHarvestYear()));
        }

        etCertifications.setText(traceability.getCertificationIds());

        // Processing fields - already auto-populated
        if (traceability.getKernelWeightKg() > 0) {
            etKernelWeight.setText(String.valueOf(traceability.getKernelWeightKg()));
            calculateYield();
        }
    }

    private void saveShippingDetails() {
        if (traceability == null) {
            Toast.makeText(getContext(), "Traceability record not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get shipping values
        long exportDate = selectedDate.getTimeInMillis();
        String buyer = etBuyer.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        String harvestYearStr = etHarvestYear.getText().toString().trim();
        String certifications = etCertifications.getText().toString().trim();

        // ✅ Get processing values
        String processingLocation = etProcessingLocation.getText().toString().trim();
        String processingFacility = etProcessingFacility.getText().toString().trim();
        String processingMethod = etProcessingMethod.getText().toString().trim();
        String shellingMethod = etShellingMethod.getText().toString().trim();
        String gradingMethod = etGradingMethod.getText().toString().trim();

        double kernelWeight = 0;
        String kernelWeightStr = etKernelWeight.getText().toString().trim();
        if (!TextUtils.isEmpty(kernelWeightStr)) {
            try {
                kernelWeight = Double.parseDouble(kernelWeightStr);
            } catch (NumberFormatException e) {
                // Keep as 0
            }
        }

        int harvestYear = 0;
        if (!TextUtils.isEmpty(harvestYearStr)) {
            try {
                harvestYear = Integer.parseInt(harvestYearStr);
            } catch (NumberFormatException e) {
                // Keep as 0
            }
        }

        // Update traceability
        traceability.setExportDate(exportDate);
        traceability.setBuyer(buyer.isEmpty() ? null : buyer);
        traceability.setDestination(destination.isEmpty() ? null : destination);
        traceability.setHarvestYear(harvestYear);
        traceability.setCertificationIds(certifications.isEmpty() ? null : certifications);
        traceability.setUpdatedAt(System.currentTimeMillis());

        // ✅ Update processing fields
        traceability.setProcessingLocation(processingLocation.isEmpty() ? DEFAULT_PROCESSING_LOCATION : processingLocation);
        traceability.setProcessingFacility(processingFacility.isEmpty() ? DEFAULT_PROCESSING_FACILITY : processingFacility);
        traceability.setProcessingMethod(processingMethod.isEmpty() ? DEFAULT_PROCESSING_METHOD : processingMethod);
        traceability.setKernelWeightKg(kernelWeight);
        traceability.setShellingMethod(shellingMethod.isEmpty() ? DEFAULT_SHELLING_METHOD : shellingMethod);
        traceability.setGradingMethod(gradingMethod.isEmpty() ? DEFAULT_GRADING_METHOD : gradingMethod);
        // Yield is auto-calculated

        repository.updateTraceability(traceability, new BatchTraceabilityRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Shipping details updated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onShippingUpdated();
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
}
