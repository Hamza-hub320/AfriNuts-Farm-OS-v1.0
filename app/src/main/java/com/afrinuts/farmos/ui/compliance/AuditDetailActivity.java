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
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.repository.AuditRepository;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
    private TextView tvFindingsCount;
    private LinearLayout findingsContainer;
    private CardView cardStatus;
    private CardView cardFindings;

    private MaterialButton btnEdit;
    private MaterialButton btnDelete;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_detail);

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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvAuditId = findViewById(R.id.tvAuditId);
        tvAuditType = findViewById(R.id.tvAuditType);
        tvStatus = findViewById(R.id.tvStatus);
        tvCertification = findViewById(R.id.tvCertification);
        tvAuditDate = findViewById(R.id.tvAuditDate);
        tvAuditorName = findViewById(R.id.tvAuditorName);
        tvAuditorBody = findViewById(R.id.tvAuditorBody);
        tvScope = findViewById(R.id.tvScope);
        tvReportUrl = findViewById(R.id.tvReportUrl);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        tvFindingsCount = findViewById(R.id.tvFindingsCount);
        findingsContainer = findViewById(R.id.findingsContainer);
        cardStatus = findViewById(R.id.cardStatus);
        cardFindings = findViewById(R.id.cardFindings);

        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Audit Details");
        }
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

                if (audit != null && audit.getCertificationId() != null) {
                    certification = certificationRepository.getCertificationByIdSync(audit.getCertificationId());
                }

                runOnUiThread(() -> {
                    if (audit != null) {
                        displayAuditData();
                    } else {
                        Toast.makeText(this, "Audit not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading audit", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void displayAuditData() {
        // Basic Info
        tvAuditId.setText(audit.getAuditId());
        tvAuditType.setText(audit.getAuditTypeDisplay());

        // Status with color
        tvStatus.setText(audit.getStatusDisplay());
        updateStatusColor();

        // Certification
        if (certification != null) {
            tvCertification.setText(certification.getCertificationName() + " (" + certification.getCertificationId() + ")");
        } else {
            tvCertification.setText(audit.getCertificationId());
        }

        // Dates
        tvAuditDate.setText(audit.getFormattedAuditDate());

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
            tvReportUrl.setVisibility(View.GONE);
        }

        // Timestamps
        tvCreatedAt.setText(dateFormat.format(audit.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(audit.getUpdatedAt()));

        // Findings
        displayFindings();

        // Role-based UI
        if (!sessionManager.canEditData()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void updateStatusColor() {
        int colorRes;
        switch (audit.getStatus()) {
            case PASS:
                colorRes = R.color.status_certified;
                break;
            case PASS_WITH_CONDITIONS:
                colorRes = R.color.status_audit_ready;
                break;
            case FAIL:
                colorRes = R.color.status_expired;
                break;
            default:
                colorRes = R.color.status_not_started;
                break;
        }
        cardStatus.setCardBackgroundColor(getColor(colorRes));
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

            if (finding.correctiveAction != null && !finding.correctiveAction.isEmpty()) {
                tvCorrectiveAction.setText("Action: " + finding.correctiveAction);
                tvCorrectiveAction.setVisibility(View.VISIBLE);
            } else {
                tvCorrectiveAction.setVisibility(View.GONE);
            }

            // Get status with proper display
            String status = finding.status != null ? finding.status.toUpperCase() : "OPEN";
            String statusDisplay;
            int bgRes;

            switch (status) {
                case "CLOSED":
                    statusDisplay = "CLOSED";
                    bgRes = R.drawable.bg_status_finding_closed;
                    break;
                case "IN_PROGRESS":
                    statusDisplay = "IN PROGRESS";
                    bgRes = R.drawable.bg_status_finding_in_progress;
                    break;
                default:
                    statusDisplay = "OPEN";
                    bgRes = R.drawable.bg_status_finding_open;
                    break;
            }

            tvFindingStatus.setText(statusDisplay);
            tvFindingStatus.setBackgroundResource(bgRes);

            // ✅ Make the status clickable to update it
            tvFindingStatus.setOnClickListener(v -> showStatusUpdateDialog(finding, position));

            findingsContainer.addView(findingView);
        }
    }

    private void showStatusUpdateDialog(AuditEntity.Finding finding, int position) {
        String[] statusOptions = {
                "OPEN - Not addressed yet",
                "IN PROGRESS - Being addressed",
                "CLOSED - Resolved"
        };

        String[] statusValues = {"OPEN", "IN PROGRESS", "CLOSED"};

        // Find current selection
        int currentSelection = 0;
        String currentStatus = finding.status != null ? finding.status.toUpperCase() : "OPEN";
        for (int i = 0; i < statusValues.length; i++) {
            if (statusValues[i].equals(currentStatus)) {
                currentSelection = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Finding Status")
                .setSingleChoiceItems(statusOptions, currentSelection, (dialog, which) -> {
                    String newStatus = statusValues[which];
                    updateFindingStatus(finding, position, newStatus);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    private void showEditDialog() {
        if (audit == null) return;

        EditAuditDialog dialog = EditAuditDialog.newInstance(audit.getAuditId());
        dialog.setOnAuditUpdatedListener(() -> {
            loadAuditData();
            Toast.makeText(this, "Audit updated!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "EditAuditDialog");
    }

    private void confirmDelete() {
        if (!sessionManager.isOwner()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Only owners can delete audits", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Audit")
                .setMessage("Are you sure you want to delete this audit record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAudit())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAudit() {
        Toast.makeText(this, "Deleting audit...", Toast.LENGTH_SHORT).show();

        auditRepository.deleteAudit(audit, new AuditRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(AuditDetailActivity.this,
                            "Audit deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(AuditDetailActivity.this,
                                "Error: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
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