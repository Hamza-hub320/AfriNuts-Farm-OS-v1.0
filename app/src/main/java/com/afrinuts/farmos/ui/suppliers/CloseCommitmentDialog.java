package com.afrinuts.farmos.ui.suppliers;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
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
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.repository.SupplierCommitmentRepository;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

public class CloseCommitmentDialog extends DialogFragment {
    private static final String TAG = "CloseCommitmentDialog";
    private static final String ARG_COMMITMENT_ID = "commitment_id";
    private static final String ARG_FARM_ID = "farm_id";

    private long commitmentId, farmId;
    private SupplierCommitmentRepository repository;

    private MaterialAutoCompleteTextView etOutcome, etDelayReason;
    private TextInputLayout layoutDelayReason;
    private MaterialCheckBox cbBuyerRequested;
    private Button btnCancel, btnSave;

    private SupplierCommitmentEntity.CommitmentStatus selectedOutcome =
            SupplierCommitmentEntity.CommitmentStatus.FULFILLED;
    private SupplierCommitmentEntity.DelayReason selectedDelayReason = null;

    public interface OnClosedListener { void onClosed(); }
    private OnClosedListener listener;

    public static CloseCommitmentDialog newInstance(long commitmentId, long farmId) {
        CloseCommitmentDialog d = new CloseCommitmentDialog();
        Bundle a = new Bundle();
        a.putLong(ARG_COMMITMENT_ID, commitmentId);
        a.putLong(ARG_FARM_ID, farmId);
        d.setArguments(a);
        return d;
    }

    public void setOnClosedListener(OnClosedListener l) { this.listener = l; }

    @Override public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        if (getArguments() != null) {
            commitmentId = getArguments().getLong(ARG_COMMITMENT_ID);
            farmId = getArguments().getLong(ARG_FARM_ID);
        }
        repository = new SupplierCommitmentRepository(requireContext(), farmId);
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle s) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_close_commitment, null);
        etOutcome = v.findViewById(R.id.etOutcome);
        etDelayReason = v.findViewById(R.id.etDelayReason);
        layoutDelayReason = v.findViewById(R.id.layoutDelayReason);
        cbBuyerRequested = v.findViewById(R.id.cbBuyerRequested);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnSave = v.findViewById(R.id.btnSave);

        setupOutcomeDropdown();
        setupDelayDropdown();

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(v).setCancelable(false).create();
        dialog.setOnShowListener(di -> {
            btnCancel.setOnClickListener(x -> dismiss());
            btnSave.setOnClickListener(x -> save());
        });
        return dialog;
    }

    private void setupOutcomeDropdown() {
        String[] labels = {"Fulfilled", "Partial", "Missed", "Cancelled"};
        etOutcome.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, labels));
        etOutcome.setText(labels[0], false);
        etOutcome.setOnItemClickListener((p, x, pos, id) -> {
            switch (pos) {
                case 0: selectedOutcome = SupplierCommitmentEntity.CommitmentStatus.FULFILLED; break;
                case 1: selectedOutcome = SupplierCommitmentEntity.CommitmentStatus.PARTIAL; break;
                case 2: selectedOutcome = SupplierCommitmentEntity.CommitmentStatus.MISSED; break;
                case 3: selectedOutcome = SupplierCommitmentEntity.CommitmentStatus.CANCELLED; break;
            }
            boolean needs = selectedOutcome == SupplierCommitmentEntity.CommitmentStatus.PARTIAL
                    || selectedOutcome == SupplierCommitmentEntity.CommitmentStatus.MISSED;
            layoutDelayReason.setVisibility(needs ? View.VISIBLE : View.GONE);
        });
    }

    private void setupDelayDropdown() {
        String[] labels = {"Supplier", "Weather", "Buyer", "Logistics", "Other"};
        etDelayReason.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, labels));
        etDelayReason.setText(labels[0], false);
        etDelayReason.setOnItemClickListener((p, x, pos, id) -> {
            switch (pos) {
                case 0: selectedDelayReason = SupplierCommitmentEntity.DelayReason.SUPPLIER; break;
                case 1: selectedDelayReason = SupplierCommitmentEntity.DelayReason.WEATHER; break;
                case 2: selectedDelayReason = SupplierCommitmentEntity.DelayReason.BUYER; break;
                case 3: selectedDelayReason = SupplierCommitmentEntity.DelayReason.LOGISTICS; break;
                case 4: selectedDelayReason = SupplierCommitmentEntity.DelayReason.OTHER; break;
            }
        });
    }

    private void save() {
        btnSave.setEnabled(false);
        btnSave.setText("Saving…");
        repository.closeCommitment(commitmentId, selectedOutcome,
                selectedDelayReason, cbBuyerRequested.isChecked(),
                new SupplierCommitmentRepository.Callback<Void>() {
                    @Override public void onSuccess(Void r) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Commitment closed", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onClosed();
                            dismiss();
                        });
                    }
                    @Override public void onError(String e) {
                        Log.e(TAG, "closeCommitment failed: " + e);
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Close Commitment");
                            Toast.makeText(getContext(), "Error: " + e, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.shutdown();
    }
}