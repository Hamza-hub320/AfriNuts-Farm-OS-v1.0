package com.afrinuts.farmos.ui.suppliers;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.repository.SupplierCommitmentRepository;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AppendRevisionDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {
    private static final String TAG = "AppendRevisionDialog";
    private static final String ARG_COMMITMENT_ID = "commitment_id";
    private static final String ARG_FARM_ID = "farm_id";

    private long commitmentId, farmId;
    private SupplierCommitmentRepository repository;
    private SessionManager sessionManager;

    private TextInputEditText etRevisedDate, etReason;
    private Button btnCancel, btnSave;
    private Calendar selectedDate = Calendar.getInstance();

    public interface OnRevisionAppendedListener { void onRevisionAppended(); }
    private OnRevisionAppendedListener listener;

    public static AppendRevisionDialog newInstance(long commitmentId, long farmId) {
        AppendRevisionDialog d = new AppendRevisionDialog();
        Bundle a = new Bundle();
        a.putLong(ARG_COMMITMENT_ID, commitmentId);
        a.putLong(ARG_FARM_ID, farmId);
        d.setArguments(a);
        return d;
    }

    public void setOnRevisionAppendedListener(OnRevisionAppendedListener l) {
        this.listener = l;
    }

    @Override public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        if (getArguments() != null) {
            commitmentId = getArguments().getLong(ARG_COMMITMENT_ID);
            farmId = getArguments().getLong(ARG_FARM_ID);
        }
        repository = new SupplierCommitmentRepository(requireContext(), farmId);
        sessionManager = new SessionManager(requireContext());
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle s) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_append_revision, null);

        etRevisedDate = v.findViewById(R.id.etRevisedDate);
        etReason = v.findViewById(R.id.etReason);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnSave = v.findViewById(R.id.btnSave);

        updateDateLabel();
        etRevisedDate.setOnClickListener(x -> new DatePickerDialog(requireContext(),
                (view, y, m, d) -> { selectedDate.set(y, m, d); updateDateLabel(); },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(v).setCancelable(false).create();
        dialog.setOnShowListener(di -> {
            btnCancel.setOnClickListener(x -> dismiss());
            btnSave.setOnClickListener(x -> save());
        });
        return dialog;
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etRevisedDate.setText(sdf.format(selectedDate.getTime()));
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
        btnSave.setEnabled(false);
        btnSave.setText("Saving…");
        String reason = etReason.getText() == null ? "" : etReason.getText().toString().trim();
        String revisedBy = sessionManager.getUserName();

        repository.appendRevision(commitmentId, selectedDate.getTimeInMillis(),
                reason, revisedBy, new SupplierCommitmentRepository.Callback<Void>() {
                    @Override public void onSuccess(Void r) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Revision appended", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onRevisionAppended();
                            dismiss();
                        });
                    }
                    @Override public void onError(String error) {
                        Log.e(TAG, "appendRevision failed: " + error);
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Append Revision");
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.shutdown();
    }
}