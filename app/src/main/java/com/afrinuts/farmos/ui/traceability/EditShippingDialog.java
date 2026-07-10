package com.afrinuts.farmos.ui.traceability;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.utils.TraceabilityAutoCreator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditShippingDialog extends DialogFragment {

    private static final String ARG_TRACEABILITY_ID = "traceability_id";

    private long traceabilityId;
    private BatchTraceabilityEntity traceability;
    private QualityBatchEntity qualityBatch;
    private SupplierEntity supplier;
    private BatchTraceabilityRepository repository;
    private QualityBatchRepository qualityBatchRepository;
    private SupplierRepository supplierRepository;

    private TextInputEditText etExportDate;
    private TextInputEditText etBuyer;        // ✅ New
    private TextInputEditText etDestination;
    private TextInputEditText etHarvestYear;   // ✅ New
    private TextInputEditText etCertifications; // ✅ New

    private Calendar selectedDate = Calendar.getInstance();

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

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle("Edit Shipping Details")
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
            view.findViewById(R.id.btnSave).setOnClickListener(v -> saveShippingDetails());
        });

        return dialog;
    }

    private void initViews(View view) {
        etExportDate = view.findViewById(R.id.etExportDate);
        etBuyer = view.findViewById(R.id.etBuyer);              // ✅ New
        etDestination = view.findViewById(R.id.etDestination);
        etHarvestYear = view.findViewById(R.id.etHarvestYear);   // ✅ New
        etCertifications = view.findViewById(R.id.etCertifications); // ✅ New
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

    private void populateFields() {
        if (traceability == null) return;

        // ✅ Auto-populate from existing data
        if (traceability.getExportDate() != null && traceability.getExportDate() > 0) {
            selectedDate.setTimeInMillis(traceability.getExportDate());
            updateDateLabel();
        } else {
            updateDateLabel();
        }

        // ✅ Buyer - from traceability or from revenue via quality batch
        if (traceability.getBuyer() != null && !traceability.getBuyer().isEmpty()) {
            etBuyer.setText(traceability.getBuyer());
        } else if (qualityBatch != null) {
            // Try to get buyer from revenue
            // For now, leave empty
        }

        if (traceability.getDestination() != null) {
            etDestination.setText(traceability.getDestination());
        }

        // ✅ Harvest Year - from traceability or quality batch
        if (traceability.getHarvestCycleId() != null) {
            // Try to get harvest year from revenue
            etHarvestYear.setText(String.valueOf(2026)); // Placeholder
        }

        // ✅ Certifications - from supplier or traceability
        if (traceability.getCertificationIds() != null && !traceability.getCertificationIds().isEmpty()) {
            etCertifications.setText(traceability.getCertificationIds());
        } else if (supplier != null && supplier.isCertified()) {
            etCertifications.setText(supplier.getCertificationType());
        }
    }

    private void saveShippingDetails() {
        if (traceability == null) {
            Toast.makeText(getContext(), "Traceability record not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get values
        long exportDate = selectedDate.getTimeInMillis();
        String buyer = etBuyer.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        int harvestYear = 0;
        String harvestYearStr = etHarvestYear.getText().toString().trim();
        if (!TextUtils.isEmpty(harvestYearStr)) {
            try {
                harvestYear = Integer.parseInt(harvestYearStr);
            } catch (NumberFormatException e) {
                // Keep as 0
            }
        }
        String certifications = etCertifications.getText().toString().trim();

        // Update traceability
        traceability.setExportDate(exportDate);
        traceability.setBuyer(buyer.isEmpty() ? null : buyer);
        traceability.setDestination(destination.isEmpty() ? null : destination);
        traceability.setHarvestYear(harvestYear);
        traceability.setCertificationIds(certifications.isEmpty() ? null : certifications);
        traceability.setUpdatedAt(System.currentTimeMillis());

        repository.updateTraceability(traceability, new BatchTraceabilityRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Shipping details updated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onShippingUpdated();
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
    }
}