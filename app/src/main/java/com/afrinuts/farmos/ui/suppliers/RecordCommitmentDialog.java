package com.afrinuts.farmos.ui.suppliers;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RecordCommitmentDialog extends DialogFragment {
    private static final String TAG = "RecordCommitmentDialog";
    private static final String ARG_SUPPLIER_ID = "supplier_id";
    private static final String ARG_FARM_ID = "farm_id";

    private long supplierId;
    private long farmId;
    private SupplierCommitmentRepository repository;
    private SessionManager sessionManager;

    private TextInputEditText etContractId, etSeason, etCommittedKg, etCommittedDate;
    private MaterialAutoCompleteTextView etProductType;
    private Button btnCancel, btnSave;

    private Calendar selectedDate = Calendar.getInstance();
    private SupplierCommitmentEntity.ProductType selectedProductType =
            SupplierCommitmentEntity.ProductType.RCN;

    public interface OnCommitmentCreatedListener {
        void onCommitmentCreated(long commitmentId);
    }
    private OnCommitmentCreatedListener listener;

    public static RecordCommitmentDialog newInstance(long supplierId, long farmId) {
        RecordCommitmentDialog d = new RecordCommitmentDialog();
        Bundle a = new Bundle();
        a.putLong(ARG_SUPPLIER_ID, supplierId);
        a.putLong(ARG_FARM_ID, farmId);
        d.setArguments(a);
        return d;
    }

    public void setOnCommitmentCreatedListener(OnCommitmentCreatedListener l) {
        this.listener = l;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            supplierId = getArguments().getLong(ARG_SUPPLIER_ID);
            farmId = getArguments().getLong(ARG_FARM_ID);
        }
        repository = new SupplierCommitmentRepository(requireContext(), farmId);
        sessionManager = new SessionManager(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_record_commitment, null);

        etContractId = v.findViewById(R.id.etContractId);
        etSeason = v.findViewById(R.id.etSeason);
        etCommittedKg = v.findViewById(R.id.etCommittedKg);
        etCommittedDate = v.findViewById(R.id.etCommittedDate);
        etProductType = v.findViewById(R.id.etProductType);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnSave = v.findViewById(R.id.btnSave);

        setupProductTypeDropdown();
        updateDateLabel();
        etCommittedDate.setOnClickListener(x -> showDatePicker());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(v)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(di -> {
            btnCancel.setOnClickListener(x -> dismiss());
            btnSave.setOnClickListener(x -> save(dialog));
        });
        return dialog;
    }

    private void setupProductTypeDropdown() {
        String[] labels = {"RCN", "KERNEL (Phase Two)"};
        etProductType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, labels));
        etProductType.setText(labels[0], false);
        etProductType.setOnItemClickListener((p, x, pos, id) ->
                selectedProductType = pos == 0
                        ? SupplierCommitmentEntity.ProductType.RCN
                        : SupplierCommitmentEntity.ProductType.KERNEL);
    }

    private void showDatePicker() {
        new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    selectedDate.set(y, m, d);
                    updateDateLabel();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etCommittedDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void save(AlertDialog dialog) {
        if (!sessionManager.canOnboardSuppliers()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cannot perform this action")
                    .setMessage("Only Owners, Managers and Field Officers can record a commitment.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String contractId = text(etContractId);
        String season = text(etSeason);
        String kgStr = text(etCommittedKg);

        if (TextUtils.isEmpty(season)) { etSeason.setError("Season is required"); return; }
        double kg;
        try { kg = Double.parseDouble(kgStr); }
        catch (Exception e) { etCommittedKg.setError("Invalid quantity"); return; }
        if (kg <= 0) { etCommittedKg.setError("Must be positive"); return; }

        btnSave.setEnabled(false);
        btnSave.setText("Saving…");

        SupplierCommitmentEntity c = new SupplierCommitmentEntity(
                supplierId, farmId, contractId, season,
                selectedProductType, kg, selectedDate.getTimeInMillis());

        String createdBy = sessionManager.getUserName();
        repository.createCommitment(c, createdBy, new SupplierCommitmentRepository.Callback<Long>() {
            @Override public void onSuccess(Long id) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Commitment recorded", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onCommitmentCreated(id);
                    dismiss();
                });
            }
            @Override public void onError(String error) {
                Log.e(TAG, "createCommitment failed: " + error);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Commitment");
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.shutdown();
    }
}