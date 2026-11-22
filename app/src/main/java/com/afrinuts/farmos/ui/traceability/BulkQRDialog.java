package com.afrinuts.farmos.ui.traceability;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.utils.BulkQRGenerator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BulkQRDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {

    private AppDatabase database;
    private List<Long> selectedBatchIds = new ArrayList<>();
    private List<BatchTraceabilityEntity> allBatches = new ArrayList<>();
    private BulkQRGenerator bulkQRGenerator;

    private TextInputEditText etShipmentName;
    private Button btnGenerateAll;
    private Button btnSelectBatches;
    private ProgressDialog progressDialog;

    public interface OnBulkQRGeneratedListener {
        void onBulkQRGenerated(File pdfFile);
    }

    private OnBulkQRGeneratedListener listener;

    public static BulkQRDialog newInstance() {
        return new BulkQRDialog();
    }

    public void setOnBulkQRGeneratedListener(OnBulkQRGeneratedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
        bulkQRGenerator = new BulkQRGenerator(requireContext());

        // Load all traceabilities
        new Thread(() -> {
            allBatches = database.batchTraceabilityDao().getAll();
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_bulk_qr, null);

        initViews(view);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)

                .setCancelable(false);

        AlertDialog dialog = builder.create();
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnGenerate).setOnClickListener(v -> generateBulkQR());

        return dialog;
    }

    private void initViews(View view) {
        etShipmentName = view.findViewById(R.id.etShipmentName);
        btnGenerateAll = view.findViewById(R.id.btnGenerateAll);
        btnSelectBatches = view.findViewById(R.id.btnSelectBatches);

        btnGenerateAll.setOnClickListener(v -> {
            // Select all batches
            selectedBatchIds.clear();
            for (BatchTraceabilityEntity batch : allBatches) {
                selectedBatchIds.add(batch.getId());
            }
            Toast.makeText(getContext(), "All " + selectedBatchIds.size() + " batches selected", Toast.LENGTH_SHORT).show();
        });

        btnSelectBatches.setOnClickListener(v -> showBatchSelectionDialog());
    }

    private void showBatchSelectionDialog() {
        if (allBatches.isEmpty()) {
            Toast.makeText(getContext(), "No batches available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] batchNames = new String[allBatches.size()];
        boolean[] checkedItems = new boolean[allBatches.size()];

        for (int i = 0; i < allBatches.size(); i++) {
            BatchTraceabilityEntity batch = allBatches.get(i);
            batchNames[i] = batch.getBatchNumber() + " - " + batch.getFormattedProcessingDate();
            checkedItems[i] = selectedBatchIds.contains(batch.getId());
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Batches")
                .setMultiChoiceItems(batchNames, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedBatchIds.add(allBatches.get(which).getId());
                    } else {
                        selectedBatchIds.remove(allBatches.get(which).getId());
                    }
                })
                .setPositiveButton("Select All", (dialog, which) -> {
                    selectedBatchIds.clear();
                    for (BatchTraceabilityEntity batch : allBatches) {
                        selectedBatchIds.add(batch.getId());
                    }
                    Toast.makeText(getContext(), selectedBatchIds.size() + " batches selected", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Done", null)
                .show();
    }

    private void generateBulkQR() {
        String shipmentName = etShipmentName.getText().toString().trim();
        if (shipmentName.isEmpty()) {
            shipmentName = "Shipment_" + System.currentTimeMillis();
        }

        if (selectedBatchIds.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one batch", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Generating QR Codes");
        progressDialog.setMessage("Generating " + selectedBatchIds.size() + " QR codes...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(selectedBatchIds.size());
        progressDialog.setCancelable(false);
        progressDialog.show();

        bulkQRGenerator.generateBulkQRForShipment(selectedBatchIds, shipmentName, new BulkQRGenerator.BulkQRCallback() {
            @Override
            public void onSuccess(File pdfFile) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "PDF generated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onBulkQRGenerated(pdfFile);
                        }
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onProgress(int current, int total) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (progressDialog != null) {
                            progressDialog.setProgress(current);
                            progressDialog.setMessage("Generating " + current + " of " + total + " QR codes...");
                        }
                    });
                }
            }
        });
    }
}
