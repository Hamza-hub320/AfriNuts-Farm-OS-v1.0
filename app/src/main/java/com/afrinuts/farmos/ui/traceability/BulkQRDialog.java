package com.afrinuts.farmos.ui.traceability;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;

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
    private TextView tvBatchSelectionCount;
    private LinearLayout layoutBatchSelectionRows;
    private ScrollView scrollBulkContent;
    private final List<CheckBox> batchCheckboxes = new ArrayList<>();
    private ProgressDialog progressDialog;
    private boolean scrollToBatchListAfterLoad;

    public interface OnBulkQRGeneratedListener {
        void onBulkQRGenerated(File pdfFile);
    }

    private OnBulkQRGeneratedListener listener;

    public static BulkQRDialog newInstance() {
        return newInstance(false);
    }

    public static BulkQRDialog newInstance(boolean scrollToBatchList) {
        BulkQRDialog dialog = new BulkQRDialog();
        Bundle args = new Bundle();
        args.putBoolean("scroll_to_batch_list", scrollToBatchList);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnBulkQRGeneratedListener(OnBulkQRGeneratedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
        bulkQRGenerator = new BulkQRGenerator(requireContext());
        scrollToBatchListAfterLoad = getArguments() != null
                && getArguments().getBoolean("scroll_to_batch_list", false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_bulk_qr, null);
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        initViews(view);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)

                .setCancelable(false);

        AlertDialog dialog = builder.create();
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnGenerate).setOnClickListener(v -> generateBulkQR());
        loadBatchesForSelection();

        return dialog;
    }

    private void initViews(View view) {
        etShipmentName = view.findViewById(R.id.etShipmentName);
        btnGenerateAll = view.findViewById(R.id.btnGenerateAll);
        tvBatchSelectionCount = view.findViewById(R.id.tvBatchSelectionCount);
        layoutBatchSelectionRows = view.findViewById(R.id.layoutBatchSelectionRows);
        scrollBulkContent = view.findViewById(R.id.scrollBulkContent);
        btnGenerateAll.setEnabled(false);
        etShipmentName.clearFocus();

        btnGenerateAll.setOnClickListener(v -> {
            selectedBatchIds.clear();
            for (BatchTraceabilityEntity batch : allBatches) {
                selectedBatchIds.add(batch.getId());
            }
            updateBatchSelectionUi();
        });
    }

    private void loadBatchesForSelection() {
        new Thread(() -> {
            List<BatchTraceabilityEntity> loaded = database.batchTraceabilityDao().getAll();
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                allBatches = loaded != null ? loaded : new ArrayList<>();
                btnGenerateAll.setEnabled(!allBatches.isEmpty());
                populateBatchRows();
                updateBatchSelectionUi();
                if (scrollToBatchListAfterLoad) {
                    scrollToBatchListAfterLoad = false;
                    scrollBulkContent.post(() -> scrollBulkContent.smoothScrollTo(0, layoutBatchSelectionRows.getTop()));
                }
            });
        }).start();
    }

    private void populateBatchRows() {
        layoutBatchSelectionRows.removeAllViews();
        batchCheckboxes.clear();
        if (allBatches.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No batches available");
            empty.setTextColor(0xFF59544C);
            empty.setTextSize(13);
            empty.setPadding(dp(4), dp(12), dp(4), dp(12));
            layoutBatchSelectionRows.addView(empty);
            return;
        }

        ColorStateList checkboxTint = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{0xFF0E6B3C, 0xFF8A857B});
        for (BatchTraceabilityEntity batch : allBatches) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(8), dp(14), dp(8));
            row.setBackgroundResource(R.drawable.bg_batch_selection_row);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(8);
            layoutBatchSelectionRows.addView(row, rowParams);

            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setButtonTintList(checkboxTint);
            checkBox.setClickable(false);
            checkBox.setFocusable(false);
            checkBox.setChecked(selectedBatchIds.contains(batch.getId()));
            row.addView(checkBox, new LinearLayout.LayoutParams(dp(40), dp(48)));
            batchCheckboxes.add(checkBox);

            LinearLayout labels = new LinearLayout(requireContext());
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(dp(4), 0, 0, 0);
            row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView batchNumber = new TextView(requireContext());
            batchNumber.setText(batch.getBatchNumber() != null ? batch.getBatchNumber() : "Batch");
            batchNumber.setTextColor(0xFF1C1B1A);
            batchNumber.setTextSize(14);
            batchNumber.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_extrabold));
            batchNumber.setMaxLines(2);
            batchNumber.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labels.addView(batchNumber);

            TextView detail = new TextView(requireContext());
            detail.setText(batch.getFormattedProcessingDate() + " · " + batch.getFormattedWeight());
            detail.setTextColor(0xFF59544C);
            detail.setTextSize(12);
            detail.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_medium));
            labels.addView(detail);

            row.setOnClickListener(v -> {
                checkBox.setChecked(!checkBox.isChecked());
                if (checkBox.isChecked()) {
                    if (!selectedBatchIds.contains(batch.getId())) selectedBatchIds.add(batch.getId());
                } else {
                    selectedBatchIds.remove(batch.getId());
                }
                updateBatchSelectionUi();
            });
        }
    }

    private void updateBatchSelectionUi() {
        for (int i = 0; i < batchCheckboxes.size() && i < allBatches.size(); i++) {
            batchCheckboxes.get(i).setChecked(selectedBatchIds.contains(allBatches.get(i).getId()));
        }
        if (tvBatchSelectionCount != null) {
            tvBatchSelectionCount.setText("Select batches (" + selectedBatchIds.size() + "/" + allBatches.size() + ")");
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void generateBulkQR() {
        String shipmentName = etShipmentName.getText().toString().trim();

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
