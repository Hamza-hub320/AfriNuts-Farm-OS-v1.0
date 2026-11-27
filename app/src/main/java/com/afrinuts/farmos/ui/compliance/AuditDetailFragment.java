package com.afrinuts.farmos.ui.compliance;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.repository.AuditRepository;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.utils.DateFormatter;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Renders the audit detail content directly in the Compliance tablet pane. */
public class AuditDetailFragment extends Fragment {

    private static final String ARG_AUDIT_ID = "audit_id";

    public static AuditDetailFragment newInstance(String auditId) {
        AuditDetailFragment fragment = new AuditDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_AUDIT_ID, auditId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private String auditId;
    private long farmId = 1;
    private AppDatabase database;
    private AuditRepository auditRepository;
    private CertificationRepository certificationRepository;
    private AuditEntity audit;
    private CertificationEntity certification;

    private TextView tvAuditId, tvAuditType, tvStatus, tvCertification, tvAuditDate;
    private TextView tvAuditorName, tvAuditorBody, tvScope, tvReportUrl;
    private TextView tvCreatedAt, tvUpdatedAt, tvValidated, tvFindingsCount;
    private TextView tvStatDate, tvStatCount, tvHeroSubtitle, tvFindingsDetail, tvSuperseded;
    private LinearLayout findingsContainer;
    private CardView cardFindings;
    private MaterialButton btnCorrect;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_audit_detail_content, container, false);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(28));
        view.setBackground(background);
        view.setClipToOutline(true);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auditId = getArguments() == null ? null : getArguments().getString(ARG_AUDIT_ID);
        database = AppDatabase.getInstance(requireContext());
        bindViews(view);
        loadAuditData();
    }

    private void bindViews(View view) {
        tvStatDate = view.findViewById(R.id.tvStatDate);
        tvStatCount = view.findViewById(R.id.tvStatCount);
        tvHeroSubtitle = view.findViewById(R.id.tvHeroSubtitle);
        tvFindingsDetail = view.findViewById(R.id.tvFindingsDetail);
        tvAuditId = view.findViewById(R.id.tvAuditId);
        tvAuditType = view.findViewById(R.id.tvAuditType);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvCertification = view.findViewById(R.id.tvCertification);
        tvAuditorName = view.findViewById(R.id.tvAuditorName);
        tvAuditorBody = view.findViewById(R.id.tvAuditorBody);
        tvScope = view.findViewById(R.id.tvScope);
        tvReportUrl = view.findViewById(R.id.tvReportUrl);
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = view.findViewById(R.id.tvUpdatedAt);
        tvValidated = view.findViewById(R.id.tvValidated);
        tvFindingsCount = view.findViewById(R.id.tvFindingsCount);
        findingsContainer = view.findViewById(R.id.findingsContainer);
        cardFindings = view.findViewById(R.id.cardFindings);
        tvSuperseded = view.findViewById(R.id.tvSuperseded);
        btnCorrect = view.findViewById(R.id.btnCorrect);

        View btnBack = view.findViewById(R.id.btnBack);
        View btnEdit = view.findViewById(R.id.btnEdit);
        ViewGroup phoneHeader = (ViewGroup) btnBack.getParent();
        phoneHeader.removeView(btnEdit);
        phoneHeader.setVisibility(View.GONE);
        moveHeaderIntoHero((ImageButton) btnEdit);
        styleTabletKpis(tvStatDate);
        styleTabletActions(btnCorrect);
        btnEdit.setOnClickListener(v -> openCorrectionDialog());
        btnCorrect.setOnClickListener(v -> openCorrectionDialog());
    }

    private void moveHeaderIntoHero(ImageButton editButton) {
        ViewGroup heroContent = (ViewGroup) tvAuditId.getParent();
        heroContent.removeView(tvAuditId);
        heroContent.removeView(tvAuditType);
        heroContent.removeView(tvHeroSubtitle);
        heroContent.removeView(tvStatus);

        FrameLayout header = new FrameLayout(requireContext());
        LinearLayout textStack = new LinearLayout(requireContext());
        textStack.setOrientation(LinearLayout.VERTICAL);
        textStack.setPadding(0, 0, dp(62), 0);
        header.addView(textStack, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.START));

        tvAuditId.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        textStack.addView(tvAuditId);
        tvAuditType.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        textStack.addView(tvAuditType);
        LinearLayout metadataRow = new LinearLayout(requireContext());
        metadataRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-2, -2);
        subtitleParams.bottomMargin = 0;
        tvHeroSubtitle.setLayoutParams(subtitleParams);
        metadataRow.addView(tvHeroSubtitle);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.setMarginStart(dp(8));
        tvStatus.setLayoutParams(statusParams);
        metadataRow.addView(tvStatus);
        LinearLayout.LayoutParams metadataParams = new LinearLayout.LayoutParams(-1, -2);
        metadataParams.topMargin = dp(4);
        textStack.addView(metadataRow, metadataParams);

        editButton.setImageResource(R.drawable.ic_farm_edit);
        editButton.setColorFilter(0xFF1C1B1A);
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setShape(GradientDrawable.OVAL);
        buttonBackground.setColor(0xFFF6F1E7);
        buttonBackground.setStroke(dp(1), 0xFFDDC9A4);
        editButton.setBackground(buttonBackground);
        FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END);
        header.addView(editButton, editParams);
        heroContent.addView(header, 0, new LinearLayout.LayoutParams(-1, -2));
    }

    private void styleTabletKpis(TextView firstStat) {
        ViewGroup kpiRow = (ViewGroup) firstStat.getParent().getParent().getParent();
        for (int i = 0; i < kpiRow.getChildCount(); i++) {
            View child = kpiRow.getChildAt(i);
            if (child instanceof CardView) {
                CardView card = (CardView) child;
                if (i > 0) card.setCardBackgroundColor(0xFFF6F1E7);
                ViewGroup content = (ViewGroup) card.getChildAt(0);
                if (content != null && content.getChildCount() > 1) {
                    View label = content.getChildAt(0);
                    View value = content.getChildAt(1);
                    if (label instanceof TextView) ((TextView) label).setTextSize(14);
                    if (value instanceof TextView) ((TextView) value).setTextSize(21);
                }
            }
        }
    }

    private void styleTabletActions(MaterialButton correctButton) {
        LinearLayout content = (LinearLayout) correctButton.getParent();
        int buttonIndex = content.indexOfChild(correctButton);
        content.removeView(correctButton);
        LinearLayout actionRow = new LinearLayout(requireContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        content.addView(actionRow, buttonIndex, new LinearLayout.LayoutParams(-1, -2));

        MaterialButton editAction = new MaterialButton(requireContext());
        editAction.setText("Edit");
        editAction.setTextColor(0xFF1C1B1A);
        editAction.setAllCaps(false);
        editAction.setTextSize(16);
        editAction.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_bold));
        editAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        editAction.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE9E1CF));
        editAction.setStrokeWidth(dp(2));
        editAction.setCornerRadius(dp(999));
        editAction.setInsetTop(0);
        editAction.setInsetBottom(0);
        editAction.setOnClickListener(v -> openCorrectionDialog());

        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(dp(128), dp(58));
        editParams.setMarginEnd(dp(8));
        LinearLayout.LayoutParams correctParams = new LinearLayout.LayoutParams(dp(172), dp(58));
        actionRow.addView(editAction, editParams);
        correctButton.setLayoutParams(correctParams);
        actionRow.addView(correctButton);
    }

    private void loadAuditData() {
        if (auditId == null || auditId.isEmpty()) return;
        android.content.Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                if (auditRepository == null) auditRepository = new AuditRepository(appContext, farmId);
                if (certificationRepository == null) certificationRepository = new CertificationRepository(appContext, farmId);
                audit = auditRepository.getAuditByIdSync(auditId);
                if (audit != null && audit.getCertificationId() != null) {
                    certification = certificationRepository.getCertificationByIdSync(audit.getCertificationId());
                } else {
                    certification = null;
                }
                String validatorName = audit == null || audit.getValidatedAt() == null
                        ? null : resolveValidatorName(audit.getValidatedBy());
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (audit == null) {
                        Toast.makeText(requireContext(), "Audit not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    displayAuditData(validatorName);
                    loadSupersededState();
                });
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Error loading audit", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String resolveValidatorName(String uid) {
        if (uid == null || uid.isEmpty()) return uid;
        try {
            UserEntity validator = database.userDao().getUserByUid(uid);
            String name = validator == null ? null : validator.getName();
            return name == null || name.trim().isEmpty() ? uid : name;
        } catch (Exception ignored) {
            return uid;
        }
    }

    private void loadSupersededState() {
        if (audit == null || auditRepository == null) return;
        AuditEntity currentAudit = audit;
        executor.execute(() -> {
            String supersedingId = database.auditDao().getSupersedingRecordId(currentAudit.getAuditId());
            AuditEntity replacedAudit = currentAudit.getSupersedesId() == null
                    ? null : auditRepository.getAuditByIdSync(currentAudit.getSupersedesId());
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || audit != currentAudit) return;
                if (currentAudit.getSupersedesId() != null && !currentAudit.getSupersedesId().isEmpty()) {
                    String replacedDate = replacedAudit == null ? "" : " from " + replacedAudit.getFormattedAuditDate();
                    tvSuperseded.setText("Supersedes an earlier audit. This audit replaces "
                            + currentAudit.getSupersedesId() + replacedDate
                            + ". The earlier audit stays on the record — it is marked superseded, never deleted.");
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else if (supersedingId != null) {
                    tvSuperseded.setText("This audit was superseded by " + supersedingId
                            + ". It stays on the record and is never deleted.");
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else {
                    tvSuperseded.setVisibility(View.GONE);
                }
            });
        });
    }

    private void displayAuditData(String validatedByDisplayName) {
        tvAuditId.setText(audit.getAuditId());
        tvAuditType.setText("Certification audit");
        String statusLabel = audit.getValidatedAt() != null && audit.getStatus() == AuditEntity.AuditStatus.PASS
                ? "Validated" : audit.getStatusDisplay();
        tvStatus.setText(statusLabel);
        updateStatusStyle();

        if (certification != null) {
            String name = certification.getCertificationName();
            String body = certification.getCertificationBody();
            tvHeroSubtitle.setText(name + (body == null || body.isEmpty() ? "" : " · " + body));
            tvCertification.setText(name + " (" + certification.getCertificationId() + ")");
        } else {
            String certificationId = audit.getCertificationId();
            tvHeroSubtitle.setText(certificationId == null ? "Certification audit" : certificationId);
            tvCertification.setText(certificationId == null ? "Not specified" : certificationId);
        }

        tvStatDate.setText(audit.getFormattedAuditDate());
        tvStatCount.setText(String.valueOf(audit.getFindingsCount()));
        tvFindingsDetail.setText(audit.getFindingsCount() + " findings");
        tvAuditorName.setText(audit.getAuditorName() == null ? "Not specified" : audit.getAuditorName());
        tvAuditorBody.setText(audit.getAuditorBody() == null ? "Not specified" : audit.getAuditorBody());
        tvScope.setText(audit.getScope() == null ? "Not specified" : audit.getScope());
        tvReportUrl.setText(audit.getReportUrl() == null || audit.getReportUrl().isEmpty() ? "—" : audit.getReportUrl());
        tvReportUrl.setVisibility(View.VISIBLE);
        tvCreatedAt.setText(dateFormat.format(audit.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(audit.getUpdatedAt()));
        tvValidated.setText(audit.getValidatedAt() == null ? "—"
                : "Validated by " + validatedByDisplayName + " on "
                + DateFormatter.formatDateOrDash(audit.getValidatedAt()));
        displayFindings();
    }

    private void updateStatusStyle() {
        int background;
        int foreground;
        switch (audit.getStatus()) {
            case PASS: background = 0xFFE3F0E7; foreground = 0xFF0A5530; break;
            case FAIL: background = 0xFFFCE8E6; foreground = 0xFFA12820; break;
            case PASS_WITH_CONDITIONS:
            default: background = 0xFFF3EAD8; foreground = 0xFF59544C; break;
        }
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(background);
        pill.setCornerRadius(dp(999));
        tvStatus.setBackground(pill);
        tvStatus.setTextColor(foreground);
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
            View row = getLayoutInflater().inflate(R.layout.item_finding, findingsContainer, false);
            TextView title = row.findViewById(R.id.tvFinding);
            TextView action = row.findViewById(R.id.tvCorrectiveAction);
            TextView status = row.findViewById(R.id.tvFindingStatus);
            title.setText(finding.finding == null ? "No description" : finding.finding);
            action.setText("Corrective action: " + (finding.correctiveAction == null
                    || finding.correctiveAction.isEmpty() ? "—" : finding.correctiveAction));
            action.setVisibility(View.VISIBLE);
            String normalized = finding.status == null ? "OPEN" : finding.status.toUpperCase(Locale.ROOT);
            int statusBackground;
            int statusForeground;
            String label;
            switch (normalized) {
                case "CLOSED": label = "CLOSED"; statusBackground = 0xFF0A5530; statusForeground = Color.WHITE; break;
                case "IN_PROGRESS": label = "IN PROGRESS"; statusBackground = 0xFFF6ECD2; statusForeground = 0xFF8A6414; break;
                default: label = "OPEN"; statusBackground = 0xFFF3EAD8; statusForeground = 0xFF59544C; break;
            }
            status.setText(label);
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(statusBackground);
            pill.setCornerRadius(dp(999));
            status.setBackground(pill);
            status.setTextColor(statusForeground);
            status.setOnClickListener(v -> showStatusUpdateDialog(finding, position));
            findingsContainer.addView(row);
            if (i < findings.size() - 1) {
                View divider = new View(requireContext());
                divider.setBackgroundColor(0xFFE9E1CF);
                findingsContainer.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
    }

    private void showStatusUpdateDialog(AuditEntity.Finding finding, int position) {
        String current = finding.status == null ? "OPEN" : finding.status.toUpperCase(Locale.ROOT);
        UpdateActionStatusDialog dialog = UpdateActionStatusDialog.newInstance(current, "Update finding status");
        dialog.setListener(newStatus -> updateFindingStatus(finding, position, newStatus));
        dialog.show(getParentFragmentManager(), "UpdateFindingStatusDialog");
    }

    private void updateFindingStatus(AuditEntity.Finding finding, int position, String newStatus) {
        finding.status = newStatus;
        List<AuditEntity.Finding> findings = audit.getFindings();
        if (findings != null && position < findings.size()) {
            findings.get(position).status = newStatus;
            audit.setFindings(findings);
        }
        audit.setUpdatedAt(System.currentTimeMillis());
        auditRepository.updateAudit(audit, new AuditRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean result) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                displayFindings();
            }
            @Override public void onError(String error) {
                if (isAdded()) Toast.makeText(requireContext(), "Error updating status: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openCorrectionDialog() {
        if (audit == null) return;
        AddAuditDialog correction = AddAuditDialog.newInstanceCorrection(farmId, audit.getAuditId());
        correction.setOnAuditAddedListener(this::loadAuditData);
        correction.show(getParentFragmentManager(), "AddAuditCorrectionDialog");
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (auditRepository != null) auditRepository.shutdown();
        if (certificationRepository != null) certificationRepository.shutdown();
        executor.shutdown();
    }
}
