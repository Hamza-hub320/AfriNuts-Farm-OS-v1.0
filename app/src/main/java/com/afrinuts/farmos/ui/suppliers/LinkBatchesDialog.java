package com.afrinuts.farmos.ui.suppliers;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.afrinuts.farmos.utils.SessionManager;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierCommitmentRepository;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LinkBatchesDialog extends DialogFragment {
    private static final String TAG = "LinkBatchesDialog";
    private static final String ARG_COMMITMENT_ID = "commitment_id";
    private static final String ARG_SUPPLIER_ID = "supplier_id";
    private static final String ARG_FARM_ID = "farm_id";

    private long commitmentId, supplierId, farmId;
    private SupplierCommitmentRepository commitmentRepo;
    private QualityBatchRepository batchRepo;

    private LinearLayout containerBatches;
    private Button btnCancel, btnSave;
    private SessionManager sessionManager;

    private final Set<Long> selectedBatchIds = new HashSet<>();

    public interface OnBatchesLinkedListener { void onBatchesLinked(); }
    private OnBatchesLinkedListener listener;

    public static LinkBatchesDialog newInstance(long commitmentId, long supplierId, long farmId) {
        LinkBatchesDialog d = new LinkBatchesDialog();
        Bundle a = new Bundle();
        a.putLong(ARG_COMMITMENT_ID, commitmentId);
        a.putLong(ARG_SUPPLIER_ID, supplierId);
        a.putLong(ARG_FARM_ID, farmId);
        d.setArguments(a);
        return d;
    }

    public void setOnBatchesLinkedListener(OnBatchesLinkedListener l) { this.listener = l; }

    @Override public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        sessionManager = new SessionManager(requireContext());
        if (getArguments() != null) {
            commitmentId = getArguments().getLong(ARG_COMMITMENT_ID);
            supplierId = getArguments().getLong(ARG_SUPPLIER_ID);
            farmId = getArguments().getLong(ARG_FARM_ID);
        }
        commitmentRepo = new SupplierCommitmentRepository(requireContext(), farmId);
        batchRepo = new QualityBatchRepository(requireContext(), farmId);
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle s) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_link_batches, null);
        containerBatches = v.findViewById(R.id.containerBatches);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnSave = v.findViewById(R.id.btnSave);

        loadBatches();

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(v).setCancelable(false).create();
        dialog.setOnShowListener(di -> {
            btnCancel.setOnClickListener(x -> dismiss());
            btnSave.setOnClickListener(x -> save());
        });
        return dialog;
    }

    private void loadBatches() {
        new Thread(() -> {
            List<QualityBatchEntity> batches = batchRepo.getBatchesBySupplierIdSync(supplierId);
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (batches.isEmpty()) {
                    TextView tv = new TextView(getContext());
                    tv.setText("No batches for this supplier yet.");
                    containerBatches.addView(tv);
                    btnSave.setEnabled(false);
                    return;
                }
                for (QualityBatchEntity b : batches) {
                    MaterialCheckBox cb = new MaterialCheckBox(getContext());
                    cb.setText(b.getBatchNumber() + " — " + b.getWeightKg() + " kg");
                    cb.setTag(b.getId());
                    cb.setOnCheckedChangeListener((bv, checked) -> {
                        Long id = (Long) bv.getTag();
                        if (checked) selectedBatchIds.add(id);
                        else selectedBatchIds.remove(id);
                    });
                    containerBatches.addView(cb);
                }
            });
        }).start();
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
        if (selectedBatchIds.isEmpty()) {
            Toast.makeText(getContext(), "Pick at least one batch", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSave.setEnabled(false);
        btnSave.setText("Saving…");

        commitmentRepo.linkBatches(commitmentId, new ArrayList<>(selectedBatchIds),
                new SupplierCommitmentRepository.Callback<Void>() {
                    @Override public void onSuccess(Void r) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Batches linked", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onBatchesLinked();
                            dismiss();
                        });
                    }
                    @Override public void onError(String e) {
                        Log.e(TAG, "linkBatches failed: " + e);
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Link Batches");
                            Toast.makeText(getContext(), "Error: " + e, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (commitmentRepo != null) commitmentRepo.shutdown();
        if (batchRepo != null) batchRepo.shutdown();
    }
}