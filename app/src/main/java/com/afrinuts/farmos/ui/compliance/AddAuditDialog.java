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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddAuditDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_CERTIFICATION_ID = "certification_id";

    private long farmId;
    private String preselectedCertificationId;
    private static final String ARG_CORRECTION_OF = "correction_of";
    private String correctionOfId;
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

    public static AddAuditDialog newInstanceCorrection(long farmId, String originalAuditId) {
        AddAuditDialog dialog = new AddAuditDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        args.putString(ARG_CORRECTION_OF, originalAuditId);
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
            correctionOfId = getArguments().getString(ARG_CORRECTION_OF);
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

        if (correctionOfId == null) {
            addFindingItem(null, null);
        }

        String title = correctionOfId != null ? "Correcting " + correctionOfId : "Add Audit";
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle(title)
                .setCancelable(false);

        if (correctionOfId != null) {
            prefillFromOriginal(correctionOfId);
        }

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

    private void prefillFromOriginal(String originalId) {
        executor.execute(() -> {
            AuditEntity original = auditRepository.getAuditByIdSync(originalId);
            if (original == null) return;
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                // Certification is preselected via the certification-picker; set preselect then reload
                preselectedCertificationId = original.getCertificationId();
                loadCertifications();

                etAuditType.setText(original.getAuditTypeDisplay(), false);

                // Audit date: correction's date defaults to TODAY (the act of correcting).
                // The original's date is preserved on the original record.
                selectedDate.setTimeInMillis(original.getAuditDate() > 0 ? original.getAuditDate() : System.currentTimeMillis());
                updateDateLabel();

                etAuditorName.setText(original.getAuditorName() != null ? original.getAuditorName() : "");
                etAuditorBody.setText(original.getAuditorBody() != null ? original.getAuditorBody() : "");
                etScope.setText(original.getScope() != null ? original.getScope() : "");
                etStatus.setText(original.getStatusDisplay(), false);
                etReportUrl.setText(original.getReportUrl() != null ? original.getReportUrl() : "");

                // Findings: prefill text, corrective action, owner, due date and closure
                // status. Severity/standardClause are not on Finding (model: finding,
                // correctiveAction, status, assignedTo, dueDate).
                List<AuditEntity.Finding> findings = original.getFindings();
                if (findings != null) {
                    for (AuditEntity.Finding f : findings) {
                        addFindingItem(f.finding, f.correctiveAction, f.status, f.assignedTo, f.dueDate);
                    }
                }
            });
        });
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
        addFindingItem(findingText, correctiveActionText, "OPEN", null, 0L);
    }

    private void addFindingItem(String findingText, String correctiveActionText,
                                String status, String assignedTo, long dueDate) {
        View findingView = getLayoutInflater().inflate(R.layout.item_finding_input, findingsContainer, false);

        TextInputEditText etFinding = findingView.findViewById(R.id.etFinding);
        TextInputEditText etCorrectiveAction = findingView.findViewById(R.id.etCorrectiveAction);
        TextInputEditText etOwner = findingView.findViewById(R.id.etFindingOwner);
        TextInputEditText etDueDate = findingView.findViewById(R.id.etFindingDueDate);
        Button btnRemove = findingView.findViewById(R.id.btnRemoveFinding);

        if (findingText != null) etFinding.setText(findingText);
        if (correctiveActionText != null) etCorrectiveAction.setText(correctiveActionText);
        if (assignedTo != null) etOwner.setText(assignedTo);

        findingView.setTag(R.id.tag_finding_status, status != null ? status : "OPEN");
        findingView.setTag(R.id.tag_finding_due_date, dueDate);
        if (dueDate > 0) {
            etDueDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(dueDate)));
        }

        etDueDate.setOnClickListener(v -> {
            long current = (Long) findingView.getTag(R.id.tag_finding_due_date);
            Calendar c = Calendar.getInstance();
            if (current > 0) c.setTimeInMillis(current);
            new DatePickerDialog(requireContext(),
                    (dp, year, month, day) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, day, 0, 0, 0);
                        picked.set(Calendar.MILLISECOND, 0);
                        long millis = picked.getTimeInMillis();
                        findingView.setTag(R.id.tag_finding_due_date, millis);
                        etDueDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(millis)));
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        btnRemove.setOnClickListener(v -> findingsContainer.removeView(findingView));
        findingsContainer.addView(findingView);
    }

    private List<AuditEntity.Finding> collectFindings() {
        List<AuditEntity.Finding> result = new ArrayList<>();

        for (int i = 0; i < findingsContainer.getChildCount(); i++) {
            View findingView = findingsContainer.getChildAt(i);
            TextInputEditText etFinding = findingView.findViewById(R.id.etFinding);
            TextInputEditText etCorrectiveAction = findingView.findViewById(R.id.etCorrectiveAction);
            TextInputEditText etOwner = findingView.findViewById(R.id.etFindingOwner);

            String finding = etFinding.getText().toString().trim();
            if (!TextUtils.isEmpty(finding)) {
                AuditEntity.Finding f = new AuditEntity.Finding();
                f.finding = finding;
                f.correctiveAction = etCorrectiveAction.getText().toString().trim();
                f.assignedTo = etOwner.getText().toString().trim();
                Object statusTag = findingView.getTag(R.id.tag_finding_status);
                f.status = statusTag instanceof String ? (String) statusTag : "OPEN";
                Object dueTag = findingView.getTag(R.id.tag_finding_due_date);
                f.dueDate = dueTag instanceof Long ? (Long) dueTag : 0L;
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

        if (correctionOfId != null) {
            audit.setSupersedesId(correctionOfId);
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        auditRepository.insertAudit(audit, new AuditRepository.InsertCallback() {
            @Override
            public void onSuccess(String auditId) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String label = correctionOfId != null ? "Correction added! ID: " : "Audit added! ID: ";
                        Toast.makeText(getContext(), label + auditId, Toast.LENGTH_SHORT).show();
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