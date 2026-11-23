package com.afrinuts.farmos.ui.compliance;

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


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
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private LinearLayout standardsRows;
    private TextView tvCreatedAt;
    private TextView tvUpdatedAt;
    private TextView tvTargetDateStat;
    private TextView tvScopeStat;
    private TextView tvStandardsCount;

    // UI Elements - Related Records
    private TextView tvAuditCount;
    private TextView tvInspectionCount;
    private TextView tvDocumentCount;

    // UI Elements - Buttons
    private View btnEdit;
    private View btnDelete;
    private MaterialButton btnChangeStatus;
    private MaterialButton btnAddAudit;
    private LinearLayout statusHistoryRows;

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
        loadCertificationData();
    }

    private void initViews() {
        // Main Info
        tvCertificationId = findViewById(R.id.tvCertificationId);
        tvCertificationName = findViewById(R.id.tvCertificationName);
        tvCertificationBody = findViewById(R.id.tvCertificationBody);
        tvStatus = findViewById(R.id.tvStatus);
        tvScope = findViewById(R.id.tvScope);
        tvTargetDate = findViewById(R.id.tvTargetDate);
        tvApprovalDate = findViewById(R.id.tvApprovalDate);
        tvExpiryDate = findViewById(R.id.tvExpiryDate);
        standardsRows = findViewById(R.id.standardsRows);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        tvTargetDateStat = findViewById(R.id.tvTargetDateStat);
        tvScopeStat = findViewById(R.id.tvScopeStat);
        tvStandardsCount = findViewById(R.id.tvStandardsCount);
        statusHistoryRows = findViewById(R.id.statusHistoryRows);

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
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
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
        tvTargetDateStat.setText(certification.getFormattedTargetDate());
        tvApprovalDate.setText(certification.getFormattedApprovalDate());
        tvExpiryDate.setText(certification.getFormattedExpiryDate());
        tvScopeStat.setText(certification.getScope() != null && !certification.getScope().trim().isEmpty()
                ? certification.getScope() : "—");

        // Standards
        List<String> standards = certification.getApplicableStandards();
        tvStandardsCount.setText(standards.size() + (standards.size() == 1 ? " standard" : " standards"));
        renderStandards(standards);

        // Audit timestamps
        tvCreatedAt.setText(dateFormat.format(certification.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(certification.getUpdatedAt()));

        // Status history
        List<CertificationEntity.StatusEvent> history = certification.getStatusHistory();
        renderStatusHistory(history);

        // Role-based UI
        if (!sessionManager.canEditData()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            btnChangeStatus.setVisibility(View.GONE);
        }
    }

    private void updateStatusColor() {
        int pillDrawable;
        switch (certification.getEffectiveStatus()) {
            case CERTIFIED: pillDrawable = R.drawable.bg_status_certified; break;
            case IN_PROGRESS: pillDrawable = R.drawable.bg_status_in_progress; break;
            case AUDIT_READY: pillDrawable = R.drawable.bg_status_audit_ready; break;
            case EXPIRED: pillDrawable = R.drawable.bg_status_expired; break;
            case SUSPENDED: pillDrawable = R.drawable.bg_status_suspended; break;
            default: pillDrawable = R.drawable.bg_status_not_started; break;
        }
        tvStatus.setBackgroundResource(pillDrawable);
    }

    private String statusLabel(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) return "—";
        try {
            return CertificationEntity.CertificationStatus.valueOf(rawStatus).getDisplayName();
        } catch (IllegalArgumentException ignored) {
            return rawStatus.replace('_', ' ');
        }
    }

    private void renderStandards(List<String> standards) {
        standardsRows.removeAllViews();
        if (standards == null || standards.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("—");
            empty.setTextColor(0xFF1C1B1A);
            empty.setTextSize(15);
            empty.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_medium));
            standardsRows.addView(empty);
            return;
        }

        for (int i = 0; i < standards.size(); i++) {
            addStandardRow(standardsRows, standards.get(i));
            if (i < standards.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0xFFE9E1CF);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                dividerParams.leftMargin = dp(48);
                dividerParams.topMargin = dp(10);
                dividerParams.bottomMargin = dp(10);
                standardsRows.addView(divider, dividerParams);
            }
        }
    }

    private void addStandardRow(LinearLayout container, String standard) {
        String name = standard == null ? "—" : standard;
        String reference = null;
        String[] pieces = name.split(" — ", 2);
        if (pieces.length == 2 && pieces[1].length() <= 15) {
            name = pieces[0].trim();
            reference = pieces[1].trim();
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);

        FrameLayout iconTile = new FrameLayout(this);
        GradientDrawable tileBackground = new GradientDrawable();
        tileBackground.setColor(0xFFE3F0E7);
        tileBackground.setCornerRadius(dp(12));
        iconTile.setBackground(tileBackground);
        ImageView medal = new ImageView(this);
        medal.setImageResource(R.drawable.ic_medal);
        medal.setColorFilter(0xFF0A5530);
        medal.setContentDescription("Standard");
        FrameLayout.LayoutParams medalParams = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER);
        iconTile.addView(medal, medalParams);
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        tileParams.rightMargin = dp(12);
        row.addView(iconTile, tileParams);

        TextView standardName = new TextView(this);
        standardName.setText(name);
        standardName.setTextColor(0xFF1C1B1A);
        standardName.setTextSize(15);
        standardName.setLineSpacing(dp(1), 1f);
        standardName.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_semibold));
        standardName.setMaxLines(2);
        if (reference != null) {
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(standardName, nameParams);
            TextView chip = new TextView(this);
            chip.setText(reference);
            chip.setTextColor(0xFF59544C);
            chip.setTextSize(11);
            chip.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(8), dp(4), dp(8), dp(4));
            GradientDrawable chipBackground = new GradientDrawable();
            chipBackground.setColor(0xFFF6F1E7);
            chipBackground.setCornerRadius(dp(8));
            chip.setBackground(chipBackground);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chipParams.leftMargin = dp(8);
            row.addView(chip, chipParams);
        } else {
            row.addView(standardName, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        container.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void renderStatusHistory(List<CertificationEntity.StatusEvent> history) {
        statusHistoryRows.removeAllViews();
        if (history == null || history.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No status changes yet");
            empty.setTextColor(0xFF59544C);
            empty.setTextSize(14);
            empty.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_medium));
            statusHistoryRows.addView(empty);
            return;
        }

        List<CertificationEntity.StatusEvent> chronological = new ArrayList<>(history);
        Collections.sort(chronological, Comparator.comparingLong(event -> event.date));
        for (int i = 0; i < chronological.size(); i++) {
            boolean current = i == chronological.size() - 1;
            addStatusHistoryRow(statusHistoryRows, chronological.get(i), current);
        }
    }

    private void addStatusHistoryRow(LinearLayout container,
                                    CertificationEntity.StatusEvent event,
                                    boolean current) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);

        TextView check = new TextView(this);
        check.setText("✓");
        check.setTextSize(16);
        check.setTypeface(Typeface.DEFAULT_BOLD);
        check.setTextColor(current ? Color.WHITE : 0xFF716B62);
        check.setGravity(Gravity.CENTER);
        GradientDrawable checkBackground = new GradientDrawable();
        checkBackground.setShape(GradientDrawable.OVAL);
        checkBackground.setColor(current ? 0xFF0E6B3C : 0xFFEEEADF);
        check.setBackground(checkBackground);
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        checkParams.rightMargin = dp(12);
        row.addView(check, checkParams);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout statusLine = new LinearLayout(this);
        statusLine.setOrientation(LinearLayout.HORIZONTAL);
        statusLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView status = new TextView(this);
        status.setText(statusLabel(event.toStatus));
        status.setTextColor(0xFF1C1B1A);
        status.setTextSize(15);
        status.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
        statusLine.addView(status);
        if (current) {
            TextView currentPill = new TextView(this);
            currentPill.setText("CURRENT");
            currentPill.setTextColor(0xFF0A5530);
            currentPill.setTextSize(10);
            currentPill.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_extrabold));
            currentPill.setPadding(dp(9), dp(4), dp(9), dp(4));
            GradientDrawable pillBackground = new GradientDrawable();
            pillBackground.setColor(0xFFE3F0E7);
            pillBackground.setCornerRadius(dp(999));
            currentPill.setBackground(pillBackground);
            LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pillParams.leftMargin = dp(10);
            statusLine.addView(currentPill, pillParams);
        }
        textColumn.addView(statusLine);

        TextView dateActor = new TextView(this);
        dateActor.setText(DateFormatter.formatDateOrDash(event.date)
                + " · " + (event.actor == null ? "—" : event.actor));
        dateActor.setTextColor(0xFF59544C);
        dateActor.setTextSize(13);
        dateActor.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_medium));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = dp(2);
        textColumn.addView(dateActor, dateParams);

        row.addView(textColumn, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (current) rowParams.bottomMargin = 0;
        else rowParams.bottomMargin = dp(18);
        container.addView(row, rowParams);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
