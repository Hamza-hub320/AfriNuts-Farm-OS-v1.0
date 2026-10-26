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
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.repository.AuditRepository;
import com.afrinuts.farmos.data.repository.CertificationRepository;
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

public class AddAuditDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_CERTIFICATION_ID = "certification_id";

    private long farmId;
    private String preselectedCertificationId;
    private AuditRepository auditRepository;
    private CertificationRepository certificationRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<CertificationEntity> certifications = new ArrayList<>();

    // UI Elements
    private MaterialAutoCompleteTextView etCertification;
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
    private List<AuditEntity.Finding> findings = new ArrayList<>();
    private int findingCounter = 0;

    public interface OnAuditAddedListener {
        void onAuditAdded();
    }

    private OnAuditAddedListener listener;

    public static AddAuditDialog newInstance(long farmId) {
        return newInstance(farmId, null);
    }

    public static AddAuditDialog newInstance(long farmId, String certificationId) {
        AddAuditDialog dialog = new AddAuditDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        if (certificationId != null) {
            args.putString(ARG_CERTIFICATION_ID, certificationId);
        }
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnAuditAddedListener(OnAuditAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
            preselectedCertificationId = getArguments().getString(ARG_CERTIFICATION_ID);
        }

        auditRepository = new AuditRepository(requireContext(), farmId);
        certificationRepository = new CertificationRepository(requireContext(), farmId);
        loadCertifications();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_audit, null);

        initViews(view);
        setupDropdowns();
        setupDatePicker();
        setupFindings();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle("Add Audit")
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
        etCertification = view.findViewById(R.id.etCertification);
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

    private void loadCertifications() {
        executor.execute(() -> {
            certifications = certificationRepository.getCertificationsSync();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    List<String> certNames = new ArrayList<>();
                    for (CertificationEntity cert : certifications) {
                        certNames.add(cert.getCertificationName() + " (" + cert.getCertificationId() + ")");
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            certNames
                    );
                    etCertification.setAdapter(adapter);

                    if (preselectedCertificationId != null) {
                        for (int i = 0; i < certifications.size(); i++) {
                            CertificationEntity cert = certifications.get(i);
                            if (cert.getCertificationId().equals(preselectedCertificationId)) {
                                etCertification.setText(cert.getCertificationName() + " (" + cert.getCertificationId() + ")", false);
                                break;
                            }
                        }
                    }
                });
            }
        });
    }

    private void setupDropdowns() {
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
        etAuditType.setText(typeNames[0], false);

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
        etStatus.setText(statusNames[0], false);
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
                f.status = "OPEN";  // ✅ Default to OPEN
                f.dueDate = 0;
                result.add(f);
            }
        }

        return result;
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

    private String getCertificationIdFromDisplay(String display) {
        if (display == null) return null;
        int start = display.indexOf("(");
        int end = display.indexOf(")");
        if (start != -1 && end != -1 && end > start) {
            return display.substring(start + 1, end);
        }
        return null;
    }

    private void saveAudit() {
        String certDisplay = etCertification.getText().toString().trim();
        if (TextUtils.isEmpty(certDisplay)) {
            etCertification.setError("Please select a certification");
            return;
        }

        String certificationId = getCertificationIdFromDisplay(certDisplay);
        if (certificationId == null) {
            etCertification.setError("Invalid certification selection");
            return;
        }

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

        AuditEntity audit = new AuditEntity(
                certificationId,
                farmId,
                auditType,
                auditDate,
                auditorName,
                auditorBody,
                scope,
                findings,
                status,
                reportUrl.isEmpty() ? null : reportUrl
        );

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        auditRepository.insertAudit(audit, new AuditRepository.InsertCallback() {
            @Override
            public void onSuccess(String auditId) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Audit added! ID: " + auditId, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onAuditAdded();
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Add Audit");
                        Toast.makeText(getContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (auditRepository != null) {
            auditRepository.shutdown();
        }
        if (certificationRepository != null) {
            certificationRepository.shutdown();
        }
        executor.shutdown();
    }
}