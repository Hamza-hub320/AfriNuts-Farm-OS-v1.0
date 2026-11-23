package com.afrinuts.farmos.ui.compliance;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.afrinuts.farmos.utils.SessionManager;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChangeStatusDialog extends DialogFragment {

    private static final String ARG_CERT_ID = "certification_id";
    private static final String ARG_FARM_ID = "farm_id";

    private String certificationId;
    private long farmId;

    private AppDatabase database;
    private CertificationRepository repository;
    private CertificationEntity certification;
    private final List<AuditEntity> audits = new ArrayList<>();
    private final List<InspectionEntity> inspections = new ArrayList<>();
    private final List<VerificationEntity> verifications = new ArrayList<>();
    private final List<DocumentEntity> documents = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private LinearLayout targetsContainer;
    private TextView tvCurrentStatus;
    private Button btnCancel;
    private SessionManager sessionManager;

    public interface OnStatusChangedListener { void onStatusChanged(); }
    private OnStatusChangedListener listener;

    public static ChangeStatusDialog newInstance(String certificationId, long farmId) {
        ChangeStatusDialog d = new ChangeStatusDialog();
        Bundle b = new Bundle();
        b.putString(ARG_CERT_ID, certificationId);
        b.putLong(ARG_FARM_ID, farmId);
        d.setArguments(b);
        return d;
    }

    public void setOnStatusChangedListener(OnStatusChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            certificationId = getArguments().getString(ARG_CERT_ID);
            farmId = getArguments().getLong(ARG_FARM_ID);
        }
        database = AppDatabase.getInstance(requireContext());
        repository = new CertificationRepository(requireContext(), farmId);
        database = AppDatabase.getInstance(requireContext());
        repository = new CertificationRepository(requireContext(), farmId);
        sessionManager = new SessionManager(requireContext());
        loadData();
        loadData();
    }

    private void loadData() {
        executor.execute(() -> {
            certification = repository.getCertificationByIdSync(certificationId);
            audits.clear();  audits.addAll(database.auditDao().getAuditsByCertificationId(certificationId));
            inspections.clear(); inspections.addAll(database.inspectionDao().getInspectionsByCertificationId(certificationId));
            verifications.clear(); verifications.addAll(database.verificationDao().getVerificationsByCertificationId(certificationId));
            documents.clear(); documents.addAll(database.documentDao().getDocumentsByCertificationId(certificationId));

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (certification == null) { dismiss(); return; }
                    populateTargets();
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_status, null);
        targetsContainer = view.findViewById(R.id.targetsContainer);
        tvCurrentStatus = view.findViewById(R.id.tvCurrentStatus);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> dismiss());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(true);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() == null ? null : getDialog().getWindow();
        if (window == null) return;
        float density = getResources().getDisplayMetrics().density;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        int width = getResources().getDisplayMetrics().widthPixels - (int) (32 * density);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void populateTargets() {
        if (targetsContainer == null) return;
        targetsContainer.removeAllViews();
        CertificationEntity.CertificationStatus current = certification.getEffectiveStatus();
        List<CertificationEntity.CertificationStatus> allowed = availableTargets();
        tvCurrentStatus.setText("Current status: " + certification.getEffectiveStatusDisplay());
        for (CertificationEntity.CertificationStatus target : CertificationEntity.CertificationStatus.values()) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_status_target, targetsContainer, false);
            TextView tv = row.findViewById(R.id.tvTarget);
            tv.setText(target.getDisplayName());
            boolean isCurrent = target == current;
            boolean isAllowed = allowed.contains(target);
            int border = isCurrent ? ContextCompat.getColor(requireContext(), R.color.primary)
                    : Color.parseColor("#E9E1CF");
            GradientDrawable background = new GradientDrawable();
            background.setColor(isCurrent ? Color.parseColor("#E3F0E7") : Color.WHITE);
            background.setCornerRadius(dp(18));
            background.setStroke(dp(isCurrent ? 2 : 1), border);
            row.setBackground(background);
            row.setPadding(dp(16), dp(10), dp(16), dp(10));
            tv.setTextColor(ContextCompat.getColor(requireContext(),
                    isCurrent || isAllowed ? R.color.text_primary : R.color.text_secondary));
            tv.setTextSize(15);
            tv.setCompoundDrawablePadding(dp(8));
            if (isCurrent) {
                tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_circle, 0);
            }
            row.setEnabled(isAllowed);
            row.setAlpha(isCurrent || isAllowed ? 1f : 0.58f);
            if (isAllowed) row.setOnClickListener(v -> performTransition(target));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) row.getLayoutParams();
            params.bottomMargin = dp(8);
            row.setLayoutParams(params);
            targetsContainer.addView(row);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private List<CertificationEntity.CertificationStatus> availableTargets() {
        List<CertificationEntity.CertificationStatus> targets = new ArrayList<>();
        CertificationEntity.CertificationStatus current = certification.getEffectiveStatus();
        if (current == CertificationEntity.CertificationStatus.SUSPENDED
                || current == CertificationEntity.CertificationStatus.EXPIRED) {
            targets.add(CertificationEntity.CertificationStatus.CERTIFIED);
            return targets;
        }
        if (current != CertificationEntity.CertificationStatus.IN_PROGRESS)
            targets.add(CertificationEntity.CertificationStatus.IN_PROGRESS);
        if (current != CertificationEntity.CertificationStatus.AUDIT_READY)
            targets.add(CertificationEntity.CertificationStatus.AUDIT_READY);
        if (current != CertificationEntity.CertificationStatus.CERTIFIED)
            targets.add(CertificationEntity.CertificationStatus.CERTIFIED);
        if (current != CertificationEntity.CertificationStatus.SUSPENDED)
            targets.add(CertificationEntity.CertificationStatus.SUSPENDED);
        return targets;
    }

    private String checkFor(CertificationEntity.CertificationStatus target) {
        switch (target) {
            case IN_PROGRESS: return checkInProgress();
            case AUDIT_READY: return checkAuditReady();
            case CERTIFIED:   return checkCertified();
            case SUSPENDED:   return checkSuspended();
            default:          return "That status cannot be set manually.";
        }
    }

    private String checkInProgress() {
        if (audits.isEmpty() && inspections.isEmpty()
                && verifications.isEmpty() && documents.isEmpty()) {
            return "Cannot move to In Progress: no audit, inspection, verification or document exists for this certification yet.";
        }
        return null;
    }

    private String checkAuditReady() {
        boolean passedInternalValidated = false;
        for (AuditEntity a : audits) {
            if (a.getAuditType() == AuditEntity.AuditType.INTERNAL
                    && a.getStatus() == AuditEntity.AuditStatus.PASS
                    && a.getValidatedAt() != null) {
                passedInternalValidated = true;
                break;
            }
        }
        if (!passedInternalValidated) {
            return "Cannot move to Audit Ready: no internal audit with status PASS has been validated.";
        }
        for (AuditEntity a : audits) {
            for (AuditEntity.Finding f : a.getFindings()) {
                if (f.status == null || !"CLOSED".equalsIgnoreCase(f.status)) {
                    return "Cannot move to Audit Ready: audit " + a.getAuditId()
                            + " has an open finding.";
                }
            }
        }
        return null;
    }

    private String checkCertified() {
        if (findLatestCertificate() == null) {
            return "Cannot move to Certified: no certificate document with an issue date exists.";
        }
        return null;
    }

    private String checkSuspended() {
        if (findSuspensionNotice() == null) {
            return "Cannot move to Suspended: no suspension notice document (AUDIT_REPORT named 'Suspension…') exists.";
        }
        return null;
    }

    private DocumentEntity findLatestCertificate() {
        DocumentEntity latest = null;
        for (DocumentEntity d : documents) {
            if (d.getDocumentType() == DocumentEntity.DocumentType.CERTIFICATE
                    && d.getIssueDate() > 0
                    && (latest == null || d.getIssueDate() > latest.getIssueDate())) {
                latest = d;
            }
        }
        return latest;
    }

    private DocumentEntity findSuspensionNotice() {
        for (DocumentEntity d : documents) {
            if (d.getDocumentType() == DocumentEntity.DocumentType.AUDIT_REPORT
                    && d.getDocumentName() != null
                    && d.getDocumentName().toLowerCase(Locale.ROOT).startsWith("suspension")) {
                return d;
            }
        }
        return null;
    }

    private void performTransition(CertificationEntity.CertificationStatus target) {
        if (!sessionManager.canChangeCertificationStatus()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cannot change status")
                    .setMessage("Status changes are restricted to the Compliance Officer and the CEO.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String reason = checkFor(target);
        if (reason != null) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cannot change status")
                    .setMessage(reason)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        CertificationEntity.CertificationStatus old = certification.getEffectiveStatus();
        String actor = currentActor();
        String documentId = null;

        if (target == CertificationEntity.CertificationStatus.CERTIFIED) {
            DocumentEntity cert = findLatestCertificate();
            certification.setApprovalDate(cert.getIssueDate());
            certification.setExpiryDate(cert.getExpiryDate());
            documentId = cert.getDocumentId();
        } else if (target == CertificationEntity.CertificationStatus.SUSPENDED) {
            documentId = findSuspensionNotice().getDocumentId();
        }

        certification.appendStatusEvent(old.name(), target.name(), actor, documentId);
        certification.setStatus(target);

        repository.updateCertification(certification, new CertificationRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean r) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Status changed to " + target.getDisplayName(), Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onStatusChanged();
                    dismiss();
                });
            }
            @Override public void onError(String error) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "❌ " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String currentActor() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return "app";
        if (u.getEmail() != null && !u.getEmail().isEmpty()) return u.getEmail();
        if (u.getUid() != null && !u.getUid().isEmpty()) return u.getUid();
        return "app";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        repository.shutdown();
        executor.shutdown();
    }
}
