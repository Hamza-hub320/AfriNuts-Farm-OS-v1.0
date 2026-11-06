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
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.repository.SupplierInspectionRepository;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RecordInspectionDialog extends DialogFragment {
    private static final String TAG = "RecordInspectionDialog";
    private static final String ARG_SUPPLIER_ID = "supplier_id";
    private static final String ARG_FARM_ID = "farm_id";

    private long supplierId, farmId;
    private SupplierInspectionRepository repository;
    private SessionManager sessionManager;

    private MaterialAutoCompleteTextView etInspectionType, etOutcome, etSeverity;
    private TextInputLayout layoutSeverity;
    private TextInputEditText etInspectionDate, etFindings, etCorrectiveAction, etInspector;
    private Button btnCancel, btnSave;

    private Calendar selectedDate = Calendar.getInstance();
    private SupplierInspectionEntity.InspectionType selectedType =
            SupplierInspectionEntity.InspectionType.FOOD_SAFETY;
    private SupplierInspectionEntity.Outcome selectedOutcome =
            SupplierInspectionEntity.Outcome.PASS;
    private SupplierInspectionEntity.Severity selectedSeverity = null;

    public interface OnInspectionCreatedListener { void onInspectionCreated(long id); }
    private OnInspectionCreatedListener listener;

    public static RecordInspectionDialog newInstance(long supplierId, long farmId) {
        RecordInspectionDialog d = new RecordInspectionDialog();
        Bundle a = new Bundle();
        a.putLong(ARG_SUPPLIER_ID, supplierId);
        a.putLong(ARG_FARM_ID, farmId);
        d.setArguments(a);
        return d;
    }

    public void setOnInspectionCreatedListener(OnInspectionCreatedListener l) { this.listener = l; }

    @Override public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        if (getArguments() != null) {
            supplierId = getArguments().getLong(ARG_SUPPLIER_ID);
            farmId = getArguments().getLong(ARG_FARM_ID);
        }
        repository = new SupplierInspectionRepository(requireContext(), farmId);
        sessionManager = new SessionManager(requireContext());
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle s) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_record_inspection, null);

        etInspectionType = v.findViewById(R.id.etInspectionType);
        etInspectionDate = v.findViewById(R.id.etInspectionDate);
        etOutcome = v.findViewById(R.id.etOutcome);
        etSeverity = v.findViewById(R.id.etSeverity);
        layoutSeverity = v.findViewById(R.id.layoutSeverity);
        etFindings = v.findViewById(R.id.etFindings);
        etCorrectiveAction = v.findViewById(R.id.etCorrectiveAction);
        etInspector = v.findViewById(R.id.etInspector);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnSave = v.findViewById(R.id.btnSave);

        setupTypeDropdown();
        setupOutcomeDropdown();
        setupSeverityDropdown();
        updateDateLabel();
        etInspectionDate.setOnClickListener(x -> showDatePicker());
        etInspector.setText(sessionManager.getUserName());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(v).setCancelable(false).create();
        dialog.setOnShowListener(di -> {
            btnCancel.setOnClickListener(x -> dismiss());
            btnSave.setOnClickListener(x -> save());
        });
        return dialog;
    }

    private void setupTypeDropdown() {
        String[] labels = {"Food safety", "Labor", "Environmental", "Documentation",
                "Organic", "Other"};
        etInspectionType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, labels));
        etInspectionType.setText(labels[0], false);
        etInspectionType.setOnItemClickListener((p, x, pos, id) -> {
            switch (pos) {
                case 0: selectedType = SupplierInspectionEntity.InspectionType.FOOD_SAFETY; break;
                case 1: selectedType = SupplierInspectionEntity.InspectionType.LABOR; break;
                case 2: selectedType = SupplierInspectionEntity.InspectionType.ENVIRONMENTAL; break;
                case 3: selectedType = SupplierInspectionEntity.InspectionType.DOCUMENTATION; break;
                case 4: selectedType = SupplierInspectionEntity.InspectionType.ORGANIC; break;
                case 5: selectedType = SupplierInspectionEntity.InspectionType.OTHER; break;
            }
        });
    }

    private void setupOutcomeDropdown() {
        String[] labels = {"Pass", "Fail", "Conditional"};
        etOutcome.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, labels));
        etOutcome.setText(labels[0], false);
        etOutcome.setOnItemClickListener((p, x, pos, id) -> {
            switch (pos) {
                case 0: selectedOutcome = SupplierInspectionEntity.Outcome.PASS; break;
                case 1: selectedOutcome = SupplierInspectionEntity.Outcome.FAIL; break;
                case 2: selectedOutcome = SupplierInspectionEntity.Outcome.CONDITIONAL; break;
            }
            boolean needsSeverity = selectedOutcome == SupplierInspectionEntity.Outcome.FAIL
                    || selectedOutcome == SupplierInspectionEntity.Outcome.CONDITIONAL;
            layoutSeverity.setVisibility(needsSeverity ? View.VISIBLE : View.GONE);
        });
    }

    private void setupSeverityDropdown() {
        String[] labels = {"Minor", "Major", "Critical"};
        etSeverity.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, labels));
        etSeverity.setText(labels[0], false);
        etSeverity.setOnItemClickListener((p, x, pos, id) -> {
            switch (pos) {
                case 0: selectedSeverity = SupplierInspectionEntity.Severity.MINOR; break;
                case 1: selectedSeverity = SupplierInspectionEntity.Severity.MAJOR; break;
                case 2: selectedSeverity = SupplierInspectionEntity.Severity.CRITICAL; break;
            }
        });
    }

    private void showDatePicker() {
        new DatePickerDialog(requireContext(),
                (view, y, m, d) -> { selectedDate.set(y, m, d); updateDateLabel(); },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etInspectionDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void save() {
        String findings = etFindings.getText() == null ? "" : etFindings.getText().toString().trim();
        if (TextUtils.isEmpty(findings)) {
            etFindings.setError("Findings required");
            return;
        }
        boolean severityRequired = selectedOutcome == SupplierInspectionEntity.Outcome.FAIL
                || selectedOutcome == SupplierInspectionEntity.Outcome.CONDITIONAL;
        if (severityRequired && selectedSeverity == null) {
            Toast.makeText(getContext(), "Severity is required for FAIL/CONDITIONAL", Toast.LENGTH_SHORT).show();
            return;
        }

        String inspector = etInspector.getText() == null ? "" : etInspector.getText().toString().trim();
        String corrective = etCorrectiveAction.getText() == null ? "" : etCorrectiveAction.getText().toString().trim();

        SupplierInspectionEntity e = new SupplierInspectionEntity(
                supplierId, farmId, selectedType, selectedDate.getTimeInMillis(),
                selectedOutcome, findings, inspector);
        if (selectedSeverity != null) e.setSeverity(selectedSeverity.name());
        if (!TextUtils.isEmpty(corrective)) e.setCorrectiveAction(corrective);

        btnSave.setEnabled(false);
        btnSave.setText("Saving…");

        String createdBy = sessionManager.getUserName();
        repository.createInspection(e, createdBy,
                new SupplierInspectionRepository.Callback<Long>() {
                    @Override public void onSuccess(Long id) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Inspection recorded", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onInspectionCreated(id);
                            dismiss();
                        });
                    }
                    @Override public void onError(String err) {
                        Log.e(TAG, "createInspection failed: " + err);
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Save Inspection");
                            Toast.makeText(getContext(), "Error: " + err, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.shutdown();
    }
}