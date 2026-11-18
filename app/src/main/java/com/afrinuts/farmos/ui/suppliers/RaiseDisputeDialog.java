package com.afrinuts.farmos.ui.suppliers;

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
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierDisputeRepository;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class RaiseDisputeDialog extends DialogFragment {
    private static final String TAG = "RaiseDisputeDialog";
    private static final String ARG_SUPPLIER_ID = "supplier_id";
    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_TARGET_ENTITY = "target_entity";
    private static final String ARG_TARGET_ID = "target_id";
    private static final String ARG_TARGET_LABEL = "target_label";

    private long supplierId, farmId, targetId;
    private SupplierDisputeEntity.TargetEntity targetEntity;

    private SupplierDisputeRepository repository;
    private SessionManager sessionManager;

    private TextInputEditText etReason;
    private Button btnCancel, btnSave;
    private String targetLabel;

    private MaterialAutoCompleteTextView etBatchPicker;
    private long selectedBatchId;
    private String selectedBatchLabel;
    private QualityBatchRepository batchRepo;
    private List<QualityBatchEntity> allBatches = new ArrayList<>();
    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    public interface OnDisputeRaisedListener { void onDisputeRaised(long id); }
    private OnDisputeRaisedListener listener;

    public static RaiseDisputeDialog newInstance(long supplierId, long farmId,
                                                 SupplierDisputeEntity.TargetEntity entity,
                                                 long targetId) {
        return newInstance(supplierId, farmId, entity, targetId, null);
    }

    /**
     * 3.4: targetLabel is the human-readable identity of the target
     * (contract number, batch number). Null falls back to the raw id —
     * but the brief mandates the caller always pass one.
     */
    public static RaiseDisputeDialog newInstance(long supplierId, long farmId,
                                                 SupplierDisputeEntity.TargetEntity entity,
                                                 long targetId,
                                                 String targetLabel) {
        RaiseDisputeDialog d = new RaiseDisputeDialog();
        Bundle a = new Bundle();
        a.putLong(ARG_SUPPLIER_ID, supplierId);
        a.putLong(ARG_FARM_ID, farmId);
        a.putString(ARG_TARGET_ENTITY, entity.name());
        a.putLong(ARG_TARGET_ID, targetId);
        a.putString(ARG_TARGET_LABEL, targetLabel);
        d.setArguments(a);
        return d;
    }

    public void setOnDisputeRaisedListener(OnDisputeRaisedListener l) { this.listener = l; }

    @Override public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        if (getArguments() != null) {
            supplierId = getArguments().getLong(ARG_SUPPLIER_ID);
            farmId = getArguments().getLong(ARG_FARM_ID);
            targetEntity = SupplierDisputeEntity.TargetEntity.valueOf(
                    getArguments().getString(ARG_TARGET_ENTITY, "COMMITMENT"));
            targetId = getArguments().getLong(ARG_TARGET_ID);
        }
        repository = new SupplierDisputeRepository(requireContext(), farmId);
        sessionManager = new SessionManager(requireContext());

    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle s) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_raise_dispute, null);
        etReason = v.findViewById(R.id.etReason);
        TextView tvTarget = v.findViewById(R.id.tvTarget);
        etBatchPicker = v.findViewById(R.id.etBatchPicker);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnSave = v.findViewById(R.id.btnSave);
        targetLabel = getArguments().getString(ARG_TARGET_LABEL);

        batchRepo = new QualityBatchRepository(requireContext(), farmId);
        selectedBatchId = targetId;
        selectedBatchLabel = targetLabel;

        etBatchPicker = v.findViewById(R.id.etBatchPicker);

        if (targetLabel != null && !targetLabel.isEmpty()) {
            tvTarget.setText("Target: " + targetEntity.name() + " " + targetLabel);
        } else {
            tvTarget.setText("Target: " + targetEntity.name() + " #" + targetId);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(v).setCancelable(false).create();
        dialog.setOnShowListener(di -> {
            btnCancel.setOnClickListener(x -> dismiss());
            btnSave.setOnClickListener(x -> save());
        });

        loadBatchPicker();

        return dialog;
    }

    private void save() {
        if (!sessionManager.canEditData()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cannot perform this action")
                    .setMessage("Only Owners and Managers can do this.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String reason = etReason.getText() == null ? "" : etReason.getText().toString().trim();
        if (TextUtils.isEmpty(reason)) {
            etReason.setError("Reason required");
            return;
        }

        String submittedBy = sessionManager.getUserName();
        SupplierDisputeEntity d = new SupplierDisputeEntity(
                supplierId, farmId, targetEntity, selectedBatchId, reason, submittedBy);
        d.setEvidenceUrlList(new ArrayList<>()); // photos wired in 6b

        btnSave.setEnabled(false);
        btnSave.setText("Submitting…");

        repository.raiseDispute(d, new SupplierDisputeRepository.Callback<Long>() {
            @Override public void onSuccess(Long id) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Dispute raised", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onDisputeRaised(id);
                    dismiss();
                });
            }
            @Override public void onError(String e) {
                Log.e(TAG, "raiseDispute failed: " + e);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Submit Dispute");
                    Toast.makeText(getContext(), "Error: " + e, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private static final java.text.SimpleDateFormat SHORT_DATE =
            new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());

    private String formatShortDate(long millis) {
        if (millis <= 0) return "—";
        return SHORT_DATE.format(new java.util.Date(millis));
    }

    private void updateTargetLine() {
        TextView tvTarget = getDialog() == null
                ? null : ((AlertDialog) getDialog()).findViewById(R.id.tvTarget);
        if (tvTarget == null) return;

        String label;
        if (selectedBatchLabel != null && !selectedBatchLabel.isEmpty()
                && targetEntity == SupplierDisputeEntity.TargetEntity.BATCH) {
            label = "BATCH " + selectedBatchLabel;
        } else if (targetLabel != null && !targetLabel.isEmpty()) {
            label = targetEntity.name() + " " + targetLabel;
        } else {
            Log.w(TAG, "RaiseDisputeDialog: no target label provided for "
                    + targetEntity + " #" + selectedBatchId);
            label = targetEntity.name() + " #" + selectedBatchId;
        }
        tvTarget.setText("Target: " + label);
    }

    private void loadBatchPicker() {
        executor.execute(() -> {
            List<QualityBatchEntity> batches =
                    batchRepo.getBatchesBySupplierIdSync(supplierId);
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                allBatches.clear();
                if (batches != null) allBatches.addAll(batches);

                List<String> labels = new ArrayList<>();
                for (QualityBatchEntity b : allBatches) {
                    labels.add(b.getBatchNumber() + " — " +
                            formatShortDate(b.getProcessingDate()) + " — " +
                            (b.getQualityGrade() != null ? b.getQualityGrade().name() : "—"));
                }

                etBatchPicker.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, labels));

                // Pre-select the current batch if we opened from a batch row
                if (targetEntity == SupplierDisputeEntity.TargetEntity.BATCH) {
                    for (int i = 0; i < allBatches.size(); i++) {
                        if (allBatches.get(i).getId() == targetId) {
                            etBatchPicker.setText(labels.get(i), false);
                            selectedBatchId = allBatches.get(i).getId();
                            selectedBatchLabel = allBatches.get(i).getBatchNumber();
                            updateTargetLine();
                            break;
                        }
                    }
                }

                etBatchPicker.setOnItemClickListener((p, v, pos, id) -> {
                    QualityBatchEntity chosen = allBatches.get(pos);
                    selectedBatchId = chosen.getId();
                    selectedBatchLabel = chosen.getBatchNumber();
                    // Picking a batch always retargets to BATCH — even from a
                    // commitment or inspection origin. The brief is explicit.
                    targetEntity = SupplierDisputeEntity.TargetEntity.BATCH;
                    updateTargetLine();
                });
            });
        });
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.shutdown();
        if (batchRepo != null) batchRepo.shutdown();
        if (executor != null) executor.shutdown();
    }
}