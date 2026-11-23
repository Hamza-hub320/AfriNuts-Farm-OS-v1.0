package com.afrinuts.farmos.ui.compliance;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.repository.AuditRepository;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.DateFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuditDetailActivity extends BaseActivity {

    public static final String EXTRA_AUDIT_ID = "audit_id";

    private String auditId;
    private AuditEntity audit;
    private CertificationEntity certification;
    private AuditRepository auditRepository;
    private CertificationRepository certificationRepository;
    private AppDatabase database;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvAuditId;
    private TextView tvAuditType;
    private TextView tvStatus;
    private TextView tvCertification;
    private TextView tvAuditDate;
    private TextView tvAuditorName;
    private TextView tvAuditorBody;
    private TextView tvScope;
    private TextView tvReportUrl;
    private TextView tvCreatedAt;
    private TextView tvUpdatedAt;
    private TextView tvValidated;
    private TextView tvFindingsCount;
    private LinearLayout findingsContainer;
    private CardView cardFindings;
    private MaterialButton btnCorrect;
    private TextView tvSuperseded;
    private TextView tvStatDate, tvStatCount, tvHeroSubtitle, tvFindingsDetail;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_detail);
        com.afrinuts.farmos.ui.TabletPane.attachList(this, savedInstanceState, new com.afrinuts.farmos.ui.compliance.AuditListFragment());

        auditId = getIntent().getStringExtra(EXTRA_AUDIT_ID);
        if (auditId == null || auditId.isEmpty()) {
            Toast.makeText(this, "Audit not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        initViews();
        setupToolbar();
        loadAuditData();
    }

    private void initViews() {
        tvStatDate = findViewById(R.id.tvStatDate);
        tvStatCount = findViewById(R.id.tvStatCount);
        tvHeroSubtitle = findViewById(R.id.tvHeroSubtitle);
        tvFindingsDetail = findViewById(R.id.tvFindingsDetail);
        tvAuditId = findViewById(R.id.tvAuditId);
        tvAuditType = findViewById(R.id.tvAuditType);
        tvStatus = findViewById(R.id.tvStatus);
        tvCertification = findViewById(R.id.tvCertification);
        tvAuditorName = findViewById(R.id.tvAuditorName);
        tvAuditorBody = findViewById(R.id.tvAuditorBody);
        tvScope = findViewById(R.id.tvScope);
        tvReportUrl = findViewById(R.id.tvReportUrl);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        tvValidated = findViewById(R.id.tvValidated);
        tvFindingsCount = findViewById(R.id.tvFindingsCount);
        findingsContainer = findViewById(R.id.findingsContainer);
        cardFindings = findViewById(R.id.cardFindings);
        tvSuperseded = findViewById(R.id.tvSuperseded);

        btnCorrect = findViewById(R.id.btnCorrect);
        btnCorrect.setOnClickListener(v -> openCorrectionDialog());

        loadSupersededState();
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEdit).setOnClickListener(v -> openCorrectionDialog());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAuditData() {
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                auditRepository = new AuditRepository(this, farmId);
                certificationRepository = new CertificationRepository(this, farmId);

                audit = auditRepository.getAuditByIdSync(auditId);

                String validatedByDisplayName = audit == null || audit.getValidatedAt() == null
                        ? null : resolveValidatorName(audit.getValidatedBy());

                if (audit != null && audit.getCertificationId() != null) {
                    certification = certificationRepository.getCertificationByIdSync(audit.getCertificationId());
                }

                runOnUiThread(() -> {
                    if (audit != null) {
                        displayAuditData(validatedByDisplayName);
                        loadSupersededState();
                    } else {
                        Toast.makeText(this, "Audit not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

                loadSupersededState();

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading audit", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void loadSupersededState() {
        if (audit == null) return;
        executor.execute(() -> {
            String supersedingId = database.auditDao()
                    .getSupersedingRecordId(audit.getAuditId());
            AuditEntity replacedAudit = audit.getSupersedesId() == null
                    ? null : auditRepository.getAuditByIdSync(audit.getSupersedesId());
            runOnUiThread(() -> {
                if (audit.getSupersedesId() != null && !audit.getSupersedesId().isEmpty()) {
                    String replacedDate = replacedAudit == null ? "" : " from " + replacedAudit.getFormattedAuditDate();
                    tvSuperseded.setText("Supersedes an earlier audit. This audit replaces "
                            + audit.getSupersedesId() + replacedDate + ". The earlier audit stays on the record — it is marked superseded, never deleted.");
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else if (supersedingId != null) {
                    tvSuperseded.setText("This audit was superseded by " + supersedingId + ". It stays on the record and is never deleted.");
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else {
                    tvSuperseded.setVisibility(View.GONE);
                }
            });
        });
    }

    private String resolveValidatorName(String uid) {
        if (uid == null || uid.isEmpty()) return uid;
        try {
            UserEntity validator = database.userDao().getUserByUid(uid);
            String name = validator != null ? validator.getName() : null;
            return name != null && !name.trim().isEmpty() ? name : uid;
        } catch (Exception ignored) {
            return uid;
        }
    }

    private void displayAuditData(String validatedByDisplayName) {
        // Basic Info
        tvAuditId.setText(audit.getAuditId());
        tvAuditType.setText("Certification audit");

        // Status with color
        String statusLabel = audit.getValidatedAt() != null
                && audit.getStatus() == AuditEntity.AuditStatus.PASS
                ? "Validated" : audit.getStatusDisplay();
        tvStatus.setText(statusLabel);
        updateStatusColor();

        // Certification
        if (certification != null) {
            String certificationName = certification.getCertificationName();
            String certificationBody = certification.getCertificationBody();
            tvHeroSubtitle.setText(certificationName + (certificationBody == null || certificationBody.isEmpty() ? "" : " · " + certificationBody));
            tvCertification.setText(certificationName + " (" + certification.getCertificationId() + ")");
        } else {
            tvHeroSubtitle.setText(audit.getCertificationId() != null ? audit.getCertificationId() : "Certification audit");
            tvCertification.setText(audit.getCertificationId() != null ? audit.getCertificationId() : "Not specified");
        }

        // Dates
        tvStatDate.setText(audit.getFormattedAuditDate());
        tvStatCount.setText(String.valueOf(audit.getFindingsCount()));
        tvFindingsDetail.setText(audit.getFindingsCount() + " findings");

        // Auditor
        tvAuditorName.setText(audit.getAuditorName() != null ? audit.getAuditorName() : "Not specified");
        tvAuditorBody.setText(audit.getAuditorBody() != null ? audit.getAuditorBody() : "Not specified");

        // Scope
        tvScope.setText(audit.getScope() != null ? audit.getScope() : "Not specified");

        // Report URL
        if (audit.getReportUrl() != null && !audit.getReportUrl().isEmpty()) {
            tvReportUrl.setText(audit.getReportUrl());
            tvReportUrl.setVisibility(View.VISIBLE);
        } else {
            tvReportUrl.setText("—");
            tvReportUrl.setVisibility(View.VISIBLE);
        }

        // Timestamps
        tvCreatedAt.setText(dateFormat.format(audit.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(audit.getUpdatedAt()));

        // Validation
        if (audit.getValidatedAt() == null) {
            tvValidated.setText("—");
        } else {
            tvValidated.setText("Validated by " + validatedByDisplayName
                    + " on " + DateFormatter.formatDateOrDash(audit.getValidatedAt()));
        }

        // Findings
        displayFindings();
    }

    private void updateStatusColor() {
        int backgroundColor;
        int textColor;
        switch (audit.getStatus()) {
            case PASS:
                backgroundColor = android.graphics.Color.parseColor("#E3F0E7");
                textColor = android.graphics.Color.parseColor("#0A5530");
                break;
            case FAIL:
                backgroundColor = android.graphics.Color.parseColor("#FCE8E6");
                textColor = android.graphics.Color.parseColor("#A12820");
                break;
            case PASS_WITH_CONDITIONS:
            default:
                backgroundColor = android.graphics.Color.parseColor("#F3EAD8");
                textColor = android.graphics.Color.parseColor("#59544C");
                break;
        }
        android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
        pill.setColor(backgroundColor);
        pill.setCornerRadius(getResources().getDisplayMetrics().density * 999f);
        tvStatus.setBackground(pill);
        tvStatus.setTextColor(textColor);
    }

    private void displayFindings() {
        List<AuditEntity.Finding> findings = audit.getFindings();

        if (findings == null || findings.isEmpty()) {
            cardFindings.setVisibility(View.GONE);
            return;
        }

        cardFindings.setVisibility(View.VISIBLE);
        tvFindingsCount.setText("(" + findings.size() + ")");

        findingsContainer.removeAllViews();

        for (int i = 0; i < findings.size(); i++) {
            AuditEntity.Finding finding = findings.get(i);
            int position = i;

            View findingView = getLayoutInflater().inflate(R.layout.item_finding, findingsContainer, false);

            TextView tvFinding = findingView.findViewById(R.id.tvFinding);
            TextView tvCorrectiveAction = findingView.findViewById(R.id.tvCorrectiveAction);
            TextView tvFindingStatus = findingView.findViewById(R.id.tvFindingStatus);

            tvFinding.setText(finding.finding != null ? finding.finding : "No description");

            tvCorrectiveAction.setText("Corrective action: "
                    + (finding.correctiveAction != null && !finding.correctiveAction.isEmpty()
                    ? finding.correctiveAction : "—"));
            tvCorrectiveAction.setVisibility(View.VISIBLE);

            // Get status with proper display
            String status = finding.status != null ? finding.status.toUpperCase() : "OPEN";
            String statusDisplay;
            int backgroundColor;
            int textColor;

            switch (status) {
                case "CLOSED":
                    statusDisplay = "CLOSED";
                    backgroundColor = android.graphics.Color.parseColor("#0A5530");
                    textColor = android.graphics.Color.WHITE;
                    break;
                case "IN_PROGRESS":
                    statusDisplay = "IN PROGRESS";
                    backgroundColor = android.graphics.Color.parseColor("#F6ECD2");
                    textColor = android.graphics.Color.parseColor("#8A6414");
                    break;
                default:
                    statusDisplay = "OPEN";
                    backgroundColor = android.graphics.Color.parseColor("#F3EAD8");
                    textColor = android.graphics.Color.parseColor("#59544C");
                    break;
            }

            tvFindingStatus.setText(statusDisplay);
            android.graphics.drawable.GradientDrawable statusPill = new android.graphics.drawable.GradientDrawable();
            statusPill.setColor(backgroundColor);
            statusPill.setCornerRadius(getResources().getDisplayMetrics().density * 999f);
            tvFindingStatus.setBackground(statusPill);
            tvFindingStatus.setTextColor(textColor);

            // ✅ Make the status clickable to update it
            tvFindingStatus.setOnClickListener(v -> showStatusUpdateDialog(finding, position));

            findingsContainer.addView(findingView);
            if (i < findings.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(android.graphics.Color.parseColor("#E9E1CF"));
                findingsContainer.addView(divider, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int) (getResources().getDisplayMetrics().density + 0.5f))));
            }
        }
    }

    private void showStatusUpdateDialog(AuditEntity.Finding finding, int position) {
        String currentStatus = finding.status != null ? finding.status.toUpperCase() : "OPEN";
        UpdateActionStatusDialog dialog = UpdateActionStatusDialog.newInstance(
                currentStatus, "Update finding status");
        dialog.setListener(newStatus -> updateFindingStatus(finding, position, newStatus));
        dialog.show(getSupportFragmentManager(), "UpdateFindingStatusDialog");
    }

    private void updateFindingStatus(AuditEntity.Finding finding, int position, String newStatus) {
        // Update the finding status
        finding.status = newStatus;

        // Update the audit's findings list
        List<AuditEntity.Finding> findings = audit.getFindings();
        if (findings != null && position < findings.size()) {
            findings.get(position).status = newStatus;
            audit.setFindings(findings);
        }

        // Save to repository
        audit.setUpdatedAt(System.currentTimeMillis());

        auditRepository.updateAudit(audit, new AuditRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(AuditDetailActivity.this,
                            "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                    // Refresh the findings display
                    displayFindings();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AuditDetailActivity.this,
                            "Error updating status: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openCorrectionDialog() {
        AddAuditDialog correction = AddAuditDialog.newInstanceCorrection(farmId, audit.getAuditId());
        correction.setOnAuditAddedListener(this::loadAuditData);
        correction.show(getSupportFragmentManager(), "AddAuditCorrectionDialog");
    }

    @Override
    protected void onDestroy() {
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
