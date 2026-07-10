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
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.data.repository.MasterTraceabilityRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateMasterTraceabilityDialog extends DialogFragment {

    private AppDatabase database;
    private BatchTraceabilityRepository batchRepository;
    private MasterTraceabilityRepository masterRepository;

    private List<BatchTraceabilityEntity> availableBatches = new ArrayList<>();
    private List<Long> selectedBatchIds = new ArrayList<>();

    private TextInputEditText etShipmentName;
    private TextInputEditText etBuyer;
    private TextInputEditText etDestination;
    private TextInputEditText etExportDate;
    private Button btnSelectBatches;
    private Button btnCreateMaster;
    private Button btnCancel;

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnMasterTraceabilityCreatedListener {
        void onMasterTraceabilityCreated(MasterBatchTraceabilityEntity master);
    }

    private OnMasterTraceabilityCreatedListener listener;

    public static CreateMasterTraceabilityDialog newInstance() {
        return new CreateMasterTraceabilityDialog();
    }

    public void setOnMasterTraceabilityCreatedListener(OnMasterTraceabilityCreatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
        batchRepository = new BatchTraceabilityRepository(requireContext(), 1);
        masterRepository = new MasterTraceabilityRepository(requireContext(), 1);

        // Load available batches
        new Thread(() -> {
            availableBatches = batchRepository.getAllTraceabilitiesSync();
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_master_traceability, null);

        initViews(view);
        setupDatePicker();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle("Create Master Traceability")
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            // Hide default buttons, use custom ones
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancelDialog = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (btnSave != null) btnSave.setVisibility(View.GONE);
            if (btnCancelDialog != null) btnCancelDialog.setVisibility(View.GONE);

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
            view.findViewById(R.id.btnCreateMaster).setOnClickListener(v -> createMasterTraceability());
        });

        return dialog;
    }

    private void initViews(View view) {
        etShipmentName = view.findViewById(R.id.etShipmentName);
        etBuyer = view.findViewById(R.id.etBuyer);
        etDestination = view.findViewById(R.id.etDestination);
        etExportDate = view.findViewById(R.id.etExportDate);
        btnSelectBatches = view.findViewById(R.id.btnSelectBatches);
        btnCreateMaster = view.findViewById(R.id.btnCreateMaster);
        btnCancel = view.findViewById(R.id.btnCancel);

        btnSelectBatches.setOnClickListener(v -> showBatchSelectionDialog());

        // Set default date
        updateDateLabel();
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

    private void showBatchSelectionDialog() {
        if (availableBatches.isEmpty()) {
            Toast.makeText(getContext(), "No batches available for master traceability", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sort by processing date (newest first)
        availableBatches.sort((b1, b2) -> Long.compare(b2.getProcessingDate(), b1.getProcessingDate()));

        String[] batchNames = new String[availableBatches.size()];
        boolean[] checkedItems = new boolean[availableBatches.size()];

        for (int i = 0; i < availableBatches.size(); i++) {
            BatchTraceabilityEntity batch = availableBatches.get(i);
            batchNames[i] = batch.getBatchNumber() + " - " + batch.getFormattedProcessingDate() +
                    " (" + batch.getFormattedWeight() + ")";
            checkedItems[i] = selectedBatchIds.contains(batch.getId());
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Batches for Master")
                .setMultiChoiceItems(batchNames, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedBatchIds.add(availableBatches.get(which).getId());
                    } else {
                        selectedBatchIds.remove(availableBatches.get(which).getId());
                    }
                    updateSelectedCount();
                })
                .setPositiveButton("Select All", (dialog, which) -> {
                    selectedBatchIds.clear();
                    for (BatchTraceabilityEntity batch : availableBatches) {
                        selectedBatchIds.add(batch.getId());
                    }
                    updateSelectedCount();
                    Toast.makeText(getContext(), selectedBatchIds.size() + " batches selected", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Clear All", (dialog, which) -> {
                    selectedBatchIds.clear();
                    updateSelectedCount();
                })
                .setNeutralButton("Done", null)
                .show();
    }

    private void updateSelectedCount() {
        int count = selectedBatchIds.size();
        btnSelectBatches.setText("Select Batches (" + count + " selected)");
    }

    private void createMasterTraceability() {
        // Validate inputs
        String shipmentName = etShipmentName.getText().toString().trim();
        if (TextUtils.isEmpty(shipmentName)) {
            etShipmentName.setError("Shipment name is required");
            return;
        }

        String buyer = etBuyer.getText().toString().trim();
        if (TextUtils.isEmpty(buyer)) {
            etBuyer.setError("Buyer name is required");
            return;
        }

        String destination = etDestination.getText().toString().trim();
        if (TextUtils.isEmpty(destination)) {
            etDestination.setError("Destination is required");
            return;
        }

        if (selectedBatchIds.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one batch", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        btnCreateMaster.setEnabled(false);
        btnCreateMaster.setText("Creating...");

        new Thread(() -> {
            try {
                // Get selected batches
                List<BatchTraceabilityEntity> selectedBatches = new ArrayList<>();
                double totalWeight = 0;
                List<String> batchNumbers = new ArrayList<>();
                List<Long> batchIds = new ArrayList<>();
                List<String> grades = new ArrayList<>();
                List<String> certifications = new ArrayList<>();

                for (Long id : selectedBatchIds) {
                    BatchTraceabilityEntity batch = batchRepository.getTraceabilityByIdSync(id);
                    if (batch != null) {
                        selectedBatches.add(batch);
                        totalWeight += batch.getWeightKg();
                        batchNumbers.add(batch.getBatchNumber());
                        batchIds.add(batch.getId());
                        if (batch.getGrade() != null && !grades.contains(batch.getGrade())) {
                            grades.add(batch.getGrade());
                        }
                        if (batch.getQualityGrade() != null && !grades.contains(batch.getQualityGrade())) {
                            grades.add(batch.getQualityGrade());
                        }
                        // Add certifications if available
                        if (batch.getCertificationIds() != null && !batch.getCertificationIds().isEmpty()) {
                            // Parse certifications if stored
                        }
                    }
                }

                // Create master batch number
                String masterBatchNumber = "MB-" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new java.util.Date()) +
                        "-" + String.format("%03d", System.currentTimeMillis() % 1000);

                // Create master entity
                MasterBatchTraceabilityEntity master = new MasterBatchTraceabilityEntity(masterBatchNumber, shipmentName);
                master.setTotalWeightKg(totalWeight);
                master.setTotalBatches(selectedBatches.size());
                master.setGrades(String.join(", ", grades));
                master.setCertifications(String.join(", ", certifications));
                master.setBatchNumbers(batchNumbers);
                master.setBatchTraceabilityIds(batchIds);
                master.setBuyer(buyer);
                master.setDestination(destination);
                master.setExportDate(selectedDate.getTimeInMillis());

                // Save to database
                long masterId = masterRepository.insertMasterTraceabilitySync(master);
                master.setId(masterId);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnCreateMaster.setEnabled(true);
                        btnCreateMaster.setText("Create Master");
                        Toast.makeText(getContext(), "Master traceability created: " + masterBatchNumber, Toast.LENGTH_LONG).show();
                        if (listener != null) {
                            listener.onMasterTraceabilityCreated(master);
                        }
                        dismiss();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnCreateMaster.setEnabled(true);
                        btnCreateMaster.setText("Create Master");
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }
}