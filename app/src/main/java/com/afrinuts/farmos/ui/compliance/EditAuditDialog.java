package com.afrinuts.farmos.ui.compliance;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.repository.AuditRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditAuditDialog extends DialogFragment {

    private static final String ARG_AUDIT_ID = "audit_id";

    private String auditId;
    private AuditEntity audit;
    private AuditRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private MaterialAutoCompleteTextView etAuditType;
    private TextInputEditText etAuditDate;
    private TextInputEditText etAuditorName;
    private TextInputEditText etAuditorBody;
    private TextInputEditText etScope;
    private MaterialAutoCompleteTextView etStatus;
    private TextInputEditText etReportUrl;
    private LinearLayout findingsContainer;
    private Button btnAddFinding;
    private Button btnCancel;
    private Button btnSave;

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnAuditUpdatedListener {
        void onAuditUpdated();
    }

    private OnAuditUpdatedListener listener;

    public static EditAuditDialog newInstance(String auditId) {
        EditAuditDialog dialog = new EditAuditDialog();
        Bundle args = new Bundle();
        args.putString(ARG_AUDIT_ID, auditId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnAuditUpdatedListener(OnAuditUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            auditId = getArguments().getString(ARG_AUDIT_ID);
        }

        long farmId = 1;
        repository = new AuditRepository(requireContext(), farmId);

        executor.execute(() -> {
            audit = repository.getAuditByIdSync(auditId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (audit != null) {
                        populateFields();
                    } else {
                        Toast.makeText(getContext(), "Audit not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_audit, null);

        initViews(view);
        setupDropdowns();
        setupDatePicker();
        setupFindings();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
            view.findViewById(R.id.btnSave).setOnClickListener(v -> saveAudit());
        });

        return dialog;
    }

    private void initViews(View view) {
        etAuditType = view.findViewById(R.id.etAuditType);
        etAuditDate = view.findViewById(R.id.etAuditDate);
        etAuditorName = view.findViewById(R.id.etAuditorName);
        etAuditorBody = view.findViewById(R.id.etAuditorBody);
        etScope = view.findViewById(R.id.etScope);
        etStatus = view.findViewById(R.id.etStatus);
        etReportUrl = view.findViewById(R.id.etReportUrl);
        findingsContainer = view.findViewById(R.id.findingsContainer);
        btnAddFinding = view.findViewById(R.id.btnAddFinding);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void setupDropdowns() {
        // Audit Type dropdown
        AuditEntity.AuditType[] types = AuditEntity.AuditType.values();
        String[] typeNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getDisplayName();
        }
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
        );
        etAuditType.setAdapter(typeAdapter);

        // Status dropdown
        AuditEntity.AuditStatus[] statuses = AuditEntity.AuditStatus.values();
        String[] statusNames = new String[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            statusNames[i] = statuses[i].getDisplayName();
        }
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                statusNames
        );
        etStatus.setAdapter(statusAdapter);
    }

    private void setupDatePicker() {
        etAuditDate.setOnClickListener(v -> showDatePicker());
        etAuditDate.setOnFocusChangeListener((v, hasFocus) -> {
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
        etAuditDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupFindings() {
        btnAddFinding.setOnClickListener(v -> addFindingItem(null, null));

        // Load existing findings
        if (audit != null) {
            List<AuditEntity.Finding> findings = audit.getFindings();
            if (findings != null) {
                for (AuditEntity.Finding finding : findings) {
                    addFindingItem(finding.finding, finding.correctiveAction);
                }
            }
        }
    }

    private void addFindingItem(String findingText, String correctiveActionText) {
        View findingView = getLayoutInflater().inflate(R.layout.item_finding_input, findingsContainer, false);

        TextInputEditText etFinding = findingView.findViewById(R.id.etFinding);
        TextInputEditText etCorrectiveAction = findingView.findViewById(R.id.etCorrectiveAction);
        Button btnRemove = findingView.findViewById(R.id.btnRemoveFinding);

        if (findingText != null) {
            etFinding.setText(findingText);
        }
        if (correctiveActionText != null) {
            etCorrectiveAction.setText(correctiveActionText);
        }

        btnRemove.setOnClickListener(v -> {
            findingsContainer.removeView(findingView);
        });

        findingsContainer.addView(findingView);
    }

    private List<AuditEntity.Finding> collectFindings() {
        List<AuditEntity.Finding> result = new ArrayList<>();

        for (int i = 0; i < findingsContainer.getChildCount(); i++) {
            View findingView = findingsContainer.getChildAt(i);
            TextInputEditText etFinding = findingView.findViewById(R.id.etFinding);
            TextInputEditText etCorrectiveAction = findingView.findViewById(R.id.etCorrectiveAction);

            String finding = etFinding.getText().toString().trim();
            if (!TextUtils.isEmpty(finding)) {
                AuditEntity.Finding f = new AuditEntity.Finding();
                f.finding = finding;
                f.correctiveAction = etCorrectiveAction.getText().toString().trim();
                f.status = "OPEN";
                result.add(f);
            }
        }

        return result;
    }

    private void populateFields() {
        if (audit == null) return;

        // Set audit type
        etAuditType.setText(audit.getAuditType().getDisplayName(), false);

        // Set date
        if (audit.getAuditDate() > 0) {
            selectedDate.setTimeInMillis(audit.getAuditDate());
            updateDateLabel();
        }

        etAuditorName.setText(audit.getAuditorName());
        etAuditorBody.setText(audit.getAuditorBody());
        etScope.setText(audit.getScope());
        etStatus.setText(audit.getStatus().getDisplayName(), false);
        etReportUrl.setText(audit.getReportUrl());
    }

    private AuditEntity.AuditType getAuditTypeFromDisplay(String display) {
        for (AuditEntity.AuditType type : AuditEntity.AuditType.values()) {
            if (display.equals(type.getDisplayName())) {
                return type;
            }
        }
        return AuditEntity.AuditType.EXTERNAL;
    }

    private AuditEntity.AuditStatus getStatusFromDisplay(String display) {
        for (AuditEntity.AuditStatus status : AuditEntity.AuditStatus.values()) {
            if (display.equals(status.getDisplayName())) {
                return status;
            }
        }
        return AuditEntity.AuditStatus.PASS;
    }

    private void saveAudit() {
        String auditTypeStr = etAuditType.getText().toString().trim();
        AuditEntity.AuditType auditType = getAuditTypeFromDisplay(auditTypeStr);

        long auditDate = selectedDate.getTimeInMillis();
        if (auditDate <= 0) {
            etAuditDate.setError("Please select an audit date");
            return;
        }

        String auditorName = etAuditorName.getText().toString().trim();
        if (TextUtils.isEmpty(auditorName)) {
            etAuditorName.setError("Auditor name required");
            return;
        }

        String auditorBody = etAuditorBody.getText().toString().trim();
        if (TextUtils.isEmpty(auditorBody)) {
            etAuditorBody.setError("Auditor body required");
            return;
        }

        String scope = etScope.getText().toString().trim();
        String statusStr = etStatus.getText().toString().trim();
        AuditEntity.AuditStatus status = getStatusFromDisplay(statusStr);
        String reportUrl = etReportUrl.getText().toString().trim();

        // ✅ Collect findings
        List<AuditEntity.Finding> findings = collectFindings();

        // Update audit
        audit.setAuditType(auditType);
        audit.setAuditDate(auditDate);
        audit.setAuditorName(auditorName);
        audit.setAuditorBody(auditorBody);
        audit.setScope(scope);
        audit.setStatus(status);
        audit.setReportUrl(reportUrl.isEmpty() ? null : reportUrl);
        audit.setFindings(findings);
        audit.setUpdatedAt(System.currentTimeMillis());

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        repository.updateAudit(audit, new AuditRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Audit updated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onAuditUpdated();
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
                        Toast.makeText(getContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
        executor.shutdown();
    }
}