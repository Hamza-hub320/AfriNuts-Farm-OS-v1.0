package com.afrinuts.farmos.ui.suppliers;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.repository.SupplierDisputeRepository;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class RaiseDisputeDialog extends DialogFragment {
    private static final String TAG = "RaiseDisputeDialog";
    private static final String ARG_SUPPLIER_ID = "supplier_id";
    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_TARGET_ENTITY = "target_entity";
    private static final String ARG_TARGET_ID = "target_id";

    private long supplierId, farmId, targetId;
    private SupplierDisputeEntity.TargetEntity targetEntity;

    private SupplierDisputeRepository repository;
    private SessionManager sessionManager;

    private TextInputEditText etReason;
    private Button btnCancel, btnSave;

    public interface OnDisputeRaisedListener { void onDisputeRaised(long id); }
    private OnDisputeRaisedListener listener;

    public static RaiseDisputeDialog newInstance(long supplierId, long farmId,
                                                 SupplierDisputeEntity.TargetEntity entity,
                                                 long targetId) {
        RaiseDisputeDialog d = new RaiseDisputeDialog();
        Bundle a = new Bundle();
        a.putLong(ARG_SUPPLIER_ID, supplierId);
        a.putLong(ARG_FARM_ID, farmId);
        a.putString(ARG_TARGET_ENTITY, entity.name());
        a.putLong(ARG_TARGET_ID, targetId);
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
        btnCancel = v.findViewById(R.id.btnCancel);
        btnSave = v.findViewById(R.id.btnSave);

        tvTarget.setText("Target: " + targetEntity.name() + " #" + targetId);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(v).setCancelable(false).create();
        dialog.setOnShowListener(di -> {
            btnCancel.setOnClickListener(x -> dismiss());
            btnSave.setOnClickListener(x -> save());
        });
        return dialog;
    }

    private void save() {
        String reason = etReason.getText() == null ? "" : etReason.getText().toString().trim();
        if (TextUtils.isEmpty(reason)) {
            etReason.setError("Reason required");
            return;
        }

        String submittedBy = sessionManager.getUserName();
        SupplierDisputeEntity d = new SupplierDisputeEntity(
                supplierId, farmId, targetEntity, targetId, reason, submittedBy);
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

    @Override public void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.shutdown();
    }
}