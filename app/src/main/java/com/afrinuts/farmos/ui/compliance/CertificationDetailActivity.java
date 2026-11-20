package com.afrinuts.farmos.ui.compliance;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.utils.DateFormatter;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.repository.AuditRepository;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.InspectionRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CertificationDetailActivity extends BaseActivity {

    public static final String EXTRA_CERTIFICATION_ID = "certification_id";

    private String certificationId;
    private CertificationEntity certification;
    private CertificationRepository certificationRepository;
    private AuditRepository auditRepository;
    private InspectionRepository inspectionRepository;
    private AppDatabase database;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements - Main
    private TextView tvCertificationId;
    private TextView tvCertificationName;
    private TextView tvCertificationBody;
    private TextView tvStatus;
    private TextView tvScope;
    private TextView tvTargetDate;
    private TextView tvApprovalDate;
    private TextView tvExpiryDate;
    private TextView tvStandards;
    private TextView tvCreatedAt;
    private TextView tvUpdatedAt;
    private CardView cardStatus;

    // UI Elements - Related Records
    private TextView tvAuditCount;
    private TextView tvInspectionCount;
    private TextView tvDocumentCount;

    // UI Elements - Buttons
    private MaterialButton btnEdit;
    private MaterialButton btnDelete;
    private MaterialButton btnChangeStatus;
    private MaterialButton btnAddAudit;
    private TextView tvStatusHistory;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certification_detail);
        com.afrinuts.farmos.ui.TabletPane.attachList(this, savedInstanceState, new com.afrinuts.farmos.ui.compliance.CertificationListFragment());

        certificationId = getIntent().getStringExtra(EXTRA_CERTIFICATION_ID);
        if (certificationId == null || certificationId.isEmpty()) {
            Toast.makeText(this, "Certification not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        initViews();
        setupToolbar();
        loadCertificationData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Main Info
        tvCertificationId = findViewById(R.id.tvCertificationId);
        tvCertificationName = findViewById(R.id.tvCertificationName);
        tvCertificationBody = findViewById(R.id.tvCertificationBody);
        tvStatus = findViewById(R.id.tvStatus);
        tvScope = findViewById(R.id.tvScope);
        tvTargetDate = findViewById(R.id.tvTargetDate);
        tvApprovalDate = findViewById(R.id.tvApprovalDate);
        tvExpiryDate = findViewById(R.id.tvExpiryDate);
        tvStandards = findViewById(R.id.tvStandards);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        tvStatusHistory = findViewById(R.id.tvStatusHistory);
        cardStatus = findViewById(R.id.cardStatus);

        // Related Records
        tvAuditCount = findViewById(R.id.tvAuditCount);
        tvInspectionCount = findViewById(R.id.tvInspectionCount);
        tvDocumentCount = findViewById(R.id.tvDocumentCount);

        // Buttons
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnAddAudit = findViewById(R.id.btnAddAudit);
        btnChangeStatus = findViewById(R.id.btnChangeStatus);
        btnChangeStatus.setOnClickListener(v -> showChangeStatusDialog());

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnAddAudit.setOnClickListener(v -> showAddAuditDialog());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Certification Details");
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

    private void loadCertificationData() {
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                certificationRepository = new CertificationRepository(this, farmId);
                auditRepository = new AuditRepository(this, farmId);
                inspectionRepository = new InspectionRepository(this, farmId);

                certification = certificationRepository.getCertificationByIdSync(certificationId);

                runOnUiThread(() -> {
                    if (certification != null) {
                        displayCertificationData();
                        loadRelatedRecordsCounts();
                    } else {
                        Toast.makeText(this, "Certification not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading certification", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }



    private void loadRelatedRecordsCounts() {
        if (certification == null) return;

        executor.execute(() -> {
            try {
                // ✅ Use sync methods directly from DAO (not LiveData)
                List<AuditEntity> audits = database.auditDao()
                        .getAuditsByCertificationId(certificationId);
                int auditCount = audits != null ? audits.size() : 0;

                List<InspectionEntity> inspections = database.inspectionDao()
                        .getInspectionsByCertificationId(certificationId);
                int inspectionCount = inspections != null ? inspections.size() : 0;

                // Document count (you can query documents table later)
                List<DocumentEntity> documents = database.documentDao()
                        .getDocumentsByCertificationId(certificationId);
                int documentCount = documents != null ? documents.size() : 0;

                // Verification count (optional)
                List<VerificationEntity> verifications = database.verificationDao()
                        .getVerificationsByCertificationId(certificationId);
                int verificationCount = verifications != null ? verifications.size() : 0;

                runOnUiThread(() -> {
                    tvAuditCount.setText(String.valueOf(auditCount));
                    tvInspectionCount.setText(String.valueOf(inspectionCount));
                    tvDocumentCount.setText(String.valueOf(documentCount));

                    // Log for debugging
                    Log.d(TAG, "Related counts - Audits: " + auditCount +
                            ", Inspections: " + inspectionCount +
                            ", Verifications: " + verificationCount);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading related counts", e);
                runOnUiThread(() -> {
                    tvAuditCount.setText("0");
                    tvInspectionCount.setText("0");
                    tvDocumentCount.setText("0");
                });
            }
        });
    }

    private void showAddAuditDialog() {
        if (certification == null) return;

        AddAuditDialog dialog = AddAuditDialog.newInstance(farmId, certification.getCertificationId());
        dialog.setOnAuditAddedListener(() -> {
            loadRelatedRecordsCounts();
            Toast.makeText(this, "Audit added for " + certification.getCertificationName(), Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "AddAuditDialog");
    }

    private void displayCertificationData() {
        // Basic Info
        tvCertificationId.setText(certification.getCertificationId());
        tvCertificationName.setText(certification.getCertificationName());
        tvCertificationBody.setText(certification.getCertificationBody());
        tvScope.setText(certification.getScope() != null ? certification.getScope() : "Not specified");

        // Status with color
        tvStatus.setText(certification.getEffectiveStatusDisplay());
        updateStatusColor();

        // Dates
        tvTargetDate.setText(certification.getFormattedTargetDate());
        tvApprovalDate.setText(certification.getFormattedApprovalDate());
        tvExpiryDate.setText(certification.getFormattedExpiryDate());

        // Standards
        tvStandards.setText(certification.getStandardsDisplay());

        // Audit timestamps
        tvCreatedAt.setText(dateFormat.format(certification.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(certification.getUpdatedAt()));

        // Status history
        List<CertificationEntity.StatusEvent> history = certification.getStatusHistory();
        if (history.isEmpty()) {
            tvStatusHistory.setText("No status changes yet");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = history.size() - 1; i >= 0; i--) {
                CertificationEntity.StatusEvent e = history.get(i);
                if (sb.length() > 0) sb.append("\n");
                sb.append(e.fromStatus != null ? e.fromStatus : "—")
                        .append(" → ").append(e.toStatus)
                        .append(" · ").append(DateFormatter.formatDateOrDash(e.date))
                        .append(" · ").append(e.actor != null ? e.actor : "—");
            }
            tvStatusHistory.setText(sb.toString());
        }

        // Role-based UI
        if (!sessionManager.canEditData()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            btnChangeStatus.setVisibility(View.GONE);
        }
    }

    private void updateStatusColor() {
        int colorRes;
        switch (certification.getEffectiveStatus()) {
            case CERTIFIED:
                colorRes = R.color.status_certified;
                break;
            case IN_PROGRESS:
                colorRes = R.color.status_in_progress;
                break;
            case AUDIT_READY:
                colorRes = R.color.status_audit_ready;
                break;
            case EXPIRED:
                colorRes = R.color.status_expired;
                break;
            case SUSPENDED:
                colorRes = R.color.status_suspended;
                break;
            default:
                colorRes = R.color.status_not_started;
                break;
        }
        cardStatus.setCardBackgroundColor(getColor(colorRes));
    }

    private void showEditDialog() {
        if (certification == null) return;

        EditCertificationDialog dialog = EditCertificationDialog.newInstance(certification.getCertificationId());
        dialog.setOnCertificationUpdatedListener(() -> {
            loadCertificationData();  // ✅ Reload data
            Toast.makeText(this, "Certification updated!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "EditCertificationDialog");
    }

    private void confirmDelete() {
        if (!sessionManager.isOwner()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Only owners can delete certifications", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Certification")
                .setMessage("Are you sure you want to delete " + certification.getCertificationName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCertification())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeStatusDialog() {
        if (certification == null) return;
        ChangeStatusDialog dialog = ChangeStatusDialog.newInstance(
                certification.getCertificationId(), farmId);
        dialog.setOnStatusChangedListener(() -> {
            loadCertificationData();
            loadRelatedRecordsCounts();
            Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "ChangeStatusDialog");
    }


    private void deleteCertification() {
        Toast.makeText(this, "Deleting certification...", Toast.LENGTH_SHORT).show();

        certificationRepository.deleteCertification(certification, new CertificationRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(CertificationDetailActivity.this,
                            "Certification and all related records deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(CertificationDetailActivity.this,
                                "Error: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (certificationRepository != null) {
            certificationRepository.shutdown();
        }
        if (auditRepository != null) {
            auditRepository.shutdown();
        }
        if (inspectionRepository != null) {
            inspectionRepository.shutdown();
        }
        executor.shutdown();
    }
}