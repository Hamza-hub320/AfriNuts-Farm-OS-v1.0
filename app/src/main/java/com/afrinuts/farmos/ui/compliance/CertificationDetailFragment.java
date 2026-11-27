package com.afrinuts.farmos.ui.compliance;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.utils.DateFormatter;
import com.afrinuts.farmos.utils.SessionManager;
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

/** Shows a programme's existing detail content inside the Compliance tablet pane. */
public class CertificationDetailFragment extends Fragment {

    private static final String ARG_CERTIFICATION_ID = "certification_id";

    public static CertificationDetailFragment newInstance(String certificationId) {
        CertificationDetailFragment fragment = new CertificationDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CERTIFICATION_ID, certificationId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private String certificationId;
    private long farmId = 1;
    private CertificationEntity certification;
    private AppDatabase database;
    private CertificationRepository certificationRepository;
    private SessionManager sessionManager;

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
    private TextView tvAuditCount;
    private TextView tvInspectionCount;
    private TextView tvDocumentCount;
    private LinearLayout statusHistoryRows;
    private View btnEdit;
    private View btnDelete;
    private View btnBack;
    private MaterialButton btnChangeStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_certification_detail_content, container, false);
        GradientDrawable paneBackground = new GradientDrawable();
        paneBackground.setColor(Color.WHITE);
        paneBackground.setCornerRadius(dp(28));
        view.setBackground(paneBackground);
        view.setClipToOutline(true);
        // Tablet detail sits in its own pane; keep the header and first KPI row close together.
        View scroll = view.findViewById(R.id.certificationDetailScroll);
        if (scroll != null) {
            scroll.setPadding(scroll.getPaddingLeft(), dp(4), scroll.getPaddingRight(), scroll.getPaddingBottom());
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        certificationId = getArguments() == null ? null : getArguments().getString(ARG_CERTIFICATION_ID);
        sessionManager = new SessionManager(requireContext());
        database = AppDatabase.getInstance(requireContext());
        bindViews(view);
        loadCertificationData();
    }

    private void bindViews(View view) {
        tvCertificationId = view.findViewById(R.id.tvCertificationId);
        tvCertificationName = view.findViewById(R.id.tvCertificationName);
        tvCertificationBody = view.findViewById(R.id.tvCertificationBody);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvScope = view.findViewById(R.id.tvScope);
        tvTargetDate = view.findViewById(R.id.tvTargetDate);
        tvApprovalDate = view.findViewById(R.id.tvApprovalDate);
        tvExpiryDate = view.findViewById(R.id.tvExpiryDate);
        standardsRows = view.findViewById(R.id.standardsRows);
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = view.findViewById(R.id.tvUpdatedAt);
        tvTargetDateStat = view.findViewById(R.id.tvTargetDateStat);
        tvScopeStat = view.findViewById(R.id.tvScopeStat);
        tvStandardsCount = view.findViewById(R.id.tvStandardsCount);
        statusHistoryRows = view.findViewById(R.id.statusHistoryRows);
        tvAuditCount = view.findViewById(R.id.tvAuditCount);
        tvInspectionCount = view.findViewById(R.id.tvInspectionCount);
        tvDocumentCount = view.findViewById(R.id.tvDocumentCount);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnBack = view.findViewById(R.id.btnBack);
        btnChangeStatus = view.findViewById(R.id.btnChangeStatus);

        btnBack.setVisibility(View.GONE);
        moveHeaderActionsIntoProgrammeCard();
        styleTabletKpis();
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnChangeStatus.setOnClickListener(v -> showChangeStatusDialog());
        MaterialButton addAudit = view.findViewById(R.id.btnAddAudit);
        addAudit.setOnClickListener(v -> showAddAuditDialog());
        styleTabletActions(addAudit);
    }

    private void styleTabletKpis() {
        ViewGroup kpiRow = (ViewGroup) tvTargetDateStat.getParent().getParent().getParent();
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

    private void styleTabletActions(MaterialButton addAudit) {
        LinearLayout actionRow = (LinearLayout) addAudit.getParent();
        actionRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        MaterialButton changeStatus = actionRow.findViewById(R.id.btnChangeStatus);

        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(dp(128), dp(58));
        addParams.rightMargin = dp(8);
        addAudit.setLayoutParams(addParams);
        addAudit.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        addAudit.setStrokeColor(ColorStateList.valueOf(0xFFE9E1CF));
        addAudit.setStrokeWidth(dp(2));

        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(dp(172), dp(58));
        changeStatus.setLayoutParams(statusParams);
    }

    private void moveHeaderActionsIntoProgrammeCard() {
        ViewGroup phoneHeader = (ViewGroup) btnBack.getParent();
        ImageButton editButton = (ImageButton) btnEdit;
        ImageButton deleteButton = (ImageButton) btnDelete;
        editButton.setImageResource(R.drawable.ic_farm_edit);
        editButton.setColorFilter(0xFF1C1B1A);
        GradientDrawable deleteBackground = new GradientDrawable();
        deleteBackground.setShape(GradientDrawable.OVAL);
        deleteBackground.setColor(0xFFFCE7E2);
        deleteBackground.setStroke(dp(1), 0xFFE9B3A3);
        deleteButton.setBackground(deleteBackground);
        deleteButton.setColorFilter(0xFFB83D2A);
        GradientDrawable editBackground = new GradientDrawable();
        editBackground.setShape(GradientDrawable.OVAL);
        editBackground.setColor(0xFFF6F1E7);
        editBackground.setStroke(dp(1), 0xFFDDC9A4);
        editButton.setBackground(editBackground);
        phoneHeader.removeView(btnEdit);
        phoneHeader.removeView(btnDelete);
        phoneHeader.setVisibility(View.GONE);

        ViewGroup infoCardContent = (ViewGroup) tvCertificationId.getParent();
        infoCardContent.setPadding(infoCardContent.getPaddingLeft(), infoCardContent.getPaddingTop(),
                infoCardContent.getPaddingRight(), 0);
        View infoCard = (View) infoCardContent.getParent();
        ViewGroup.MarginLayoutParams infoCardParams =
                (ViewGroup.MarginLayoutParams) infoCard.getLayoutParams();
        infoCardParams.bottomMargin = 0;
        infoCard.setLayoutParams(infoCardParams);
        infoCardContent.removeView(tvCertificationId);
        infoCardContent.removeView(tvCertificationName);
        infoCardContent.removeView(tvCertificationBody);
        infoCardContent.removeView(tvStatus);

        // Keep the text stack compact; float the actions at the card's upper-right
        // instead of letting their 48dp height create a gap below the programme ID.
        FrameLayout header = new FrameLayout(requireContext());
        LinearLayout textStack = new LinearLayout(requireContext());
        textStack.setOrientation(LinearLayout.VERTICAL);
        header.addView(textStack, new FrameLayout.LayoutParams(-1, -2,
                Gravity.TOP | Gravity.START));
        tvCertificationId.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        textStack.addView(tvCertificationId);
        tvCertificationName.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        textStack.addView(tvCertificationName);

        LinearLayout metadataRow = new LinearLayout(requireContext());
        metadataRow.setGravity(Gravity.CENTER_VERTICAL);
        tvCertificationBody.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        metadataRow.addView(tvCertificationBody);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.leftMargin = dp(12);
        tvStatus.setTextSize(13);
        tvStatus.setPadding(dp(14), dp(7), dp(14), dp(7));
        tvStatus.setLayoutParams(statusParams);
        metadataRow.addView(tvStatus);
        LinearLayout.LayoutParams metadataParams = new LinearLayout.LayoutParams(-1, -2);
        metadataParams.topMargin = dp(2);
        textStack.addView(metadataRow, metadataParams);

        FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(dp(48), dp(48),
                Gravity.TOP | Gravity.END);
        editParams.rightMargin = dp(56);
        header.addView(btnEdit, editParams);
        header.addView(btnDelete, new FrameLayout.LayoutParams(dp(48), dp(48),
                Gravity.TOP | Gravity.END));
        infoCardContent.addView(header, 0, new LinearLayout.LayoutParams(-1, -2));
    }

    private void loadCertificationData() {
        if (certificationId == null || certificationId.isEmpty()) return;
        android.content.Context context = requireContext();
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                certificationRepository = new CertificationRepository(context, farmId);
                certification = certificationRepository.getCertificationByIdSync(certificationId);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (certification == null) {
                        Toast.makeText(requireContext(), "Certification not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    displayCertificationData();
                    loadRelatedRecordsCounts();
                });
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Error loading certification", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadRelatedRecordsCounts() {
        if (certification == null || !isAdded()) return;
        executor.execute(() -> {
            int audits = 0, inspections = 0, documents = 0;
            try {
                List<AuditEntity> auditRows = database.auditDao().getAuditsByCertificationId(certificationId);
                List<InspectionEntity> inspectionRows = database.inspectionDao().getInspectionsByCertificationId(certificationId);
                List<DocumentEntity> documentRows = database.documentDao().getDocumentsByCertificationId(certificationId);
                List<VerificationEntity> verificationRows = database.verificationDao().getVerificationsByCertificationId(certificationId);
                audits = auditRows == null ? 0 : auditRows.size();
                inspections = inspectionRows == null ? 0 : inspectionRows.size();
                documents = documentRows == null ? 0 : documentRows.size();
                if (verificationRows != null) { /* queried to match the detail activity's related data load */ }
            } catch (Exception ignored) { }
            final int auditCount = audits, inspectionCount = inspections, documentCount = documents;
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                tvAuditCount.setText(String.valueOf(auditCount));
                tvInspectionCount.setText(String.valueOf(inspectionCount));
                tvDocumentCount.setText(String.valueOf(documentCount));
            });
        });
    }

    private void displayCertificationData() {
        tvCertificationId.setText(certification.getCertificationId());
        String programmeName = certification.getCertificationName();
        String body = certification.getCertificationBody();
        if (body != null && !body.trim().isEmpty()
                && (programmeName == null || !programmeName.toLowerCase(Locale.ROOT)
                .contains(body.toLowerCase(Locale.ROOT)))) {
            programmeName = programmeName + " (" + body + ")";
        }
        tvCertificationName.setText(programmeName);
        tvCertificationBody.setText(certification.getCertificationBody() == null
                ? "—" : certification.getCertificationBody());
        tvScope.setText(certification.getScope() != null ? certification.getScope() : "Not specified");
        tvStatus.setText(certification.getEffectiveStatusDisplay());
        updateStatusStyle();
        tvTargetDate.setText(certification.getFormattedTargetDate());
        tvTargetDateStat.setText(certification.getFormattedTargetDate());
        tvApprovalDate.setText(certification.getFormattedApprovalDate());
        tvExpiryDate.setText(certification.getFormattedExpiryDate());
        tvScopeStat.setText(certification.getScope() != null && !certification.getScope().trim().isEmpty()
                ? certification.getScope() : "—");
        List<String> standards = certification.getApplicableStandards();
        tvStandardsCount.setText(standards.size() + (standards.size() == 1 ? " standard" : " standards"));
        renderStandards(standards);
        tvCreatedAt.setText(dateFormat.format(certification.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(certification.getUpdatedAt()));
        renderStatusHistory(certification.getStatusHistory());
        if (!sessionManager.canEditData()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            btnChangeStatus.setVisibility(View.GONE);
        }
    }

    private void updateStatusStyle() {
        int background;
        int foreground;
        switch (certification.getEffectiveStatus()) {
            case CERTIFIED: background = 0xFFE3F0E7; foreground = 0xFF0A5530; break;
            case IN_PROGRESS: background = 0xFFF3EAD8; foreground = 0xFF735A2B; break;
            case AUDIT_READY: background = 0xFFF7EDCF; foreground = 0xFF8A6414; break;
            case EXPIRED:
            case SUSPENDED: background = 0xFFFCE7E2; foreground = 0xFF9F3528; break;
            default: background = 0xFFEEEADF; foreground = 0xFF716B62; break;
        }
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(background);
        pill.setCornerRadius(dp(999));
        tvStatus.setBackground(pill);
        tvStatus.setTextColor(foreground);
    }

    private String statusLabel(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) return "—";
        try { return CertificationEntity.CertificationStatus.valueOf(rawStatus).getDisplayName(); }
        catch (IllegalArgumentException ignored) { return rawStatus.replace('_', ' '); }
    }

    private void renderStandards(List<String> standards) {
        standardsRows.removeAllViews();
        if (standards == null || standards.isEmpty()) {
            TextView empty = text("—", 15, 0xFF1C1B1A, R.font.manrope_medium);
            standardsRows.addView(empty);
            return;
        }
        for (int i = 0; i < standards.size(); i++) {
            addStandardRow(standardsRows, standards.get(i));
            if (i < standards.size() - 1) {
                View divider = new View(requireContext());
                divider.setBackgroundColor(0xFFE9E1CF);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
                params.leftMargin = dp(48); params.topMargin = dp(10); params.bottomMargin = dp(10);
                standardsRows.addView(divider, params);
            }
        }
    }

    private void addStandardRow(LinearLayout container, String standard) {
        String name = standard == null ? "—" : standard;
        String reference = null;
        String[] pieces = name.split(" — ", 2);
        if (pieces.length == 2 && pieces[1].length() <= 15) { name = pieces[0].trim(); reference = pieces[1].trim(); }
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.TOP);
        FrameLayout tile = new FrameLayout(requireContext());
        GradientDrawable tileBg = new GradientDrawable(); tileBg.setColor(0xFFE3F0E7); tileBg.setCornerRadius(dp(12));
        tile.setBackground(tileBg);
        ImageView medal = new ImageView(requireContext()); medal.setImageResource(R.drawable.ic_medal);
        medal.setColorFilter(0xFF0A5530); medal.setContentDescription("Standard");
        tile.addView(medal, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(36), dp(36)); tileParams.rightMargin = dp(12);
        row.addView(tile, tileParams);
        TextView standardName = text(name, 15, 0xFF1C1B1A, R.font.manrope_semibold); standardName.setMaxLines(2);
        if (reference != null) {
            row.addView(standardName, new LinearLayout.LayoutParams(0, -2, 1));
            TextView chip = text(reference, 11, 0xFF59544C, R.font.manrope_bold);
            chip.setGravity(Gravity.CENTER); chip.setPadding(dp(8), dp(4), dp(8), dp(4));
            GradientDrawable chipBg = new GradientDrawable(); chipBg.setColor(0xFFF6F1E7); chipBg.setCornerRadius(dp(8)); chip.setBackground(chipBg);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, -2); chipParams.leftMargin = dp(8); row.addView(chip, chipParams);
        } else row.addView(standardName, new LinearLayout.LayoutParams(0, -2, 1));
        container.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private void renderStatusHistory(List<CertificationEntity.StatusEvent> history) {
        statusHistoryRows.removeAllViews();
        if (history == null || history.isEmpty()) {
            statusHistoryRows.addView(text("No status changes yet", 14, 0xFF59544C, R.font.manrope_medium));
            return;
        }
        List<CertificationEntity.StatusEvent> chronological = new ArrayList<>(history);
        Collections.sort(chronological, Comparator.comparingLong(event -> event.date));
        for (int i = 0; i < chronological.size(); i++) addStatusHistoryRow(chronological.get(i), i == chronological.size() - 1);
    }

    private void addStatusHistoryRow(CertificationEntity.StatusEvent event, boolean current) {
        LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.TOP);
        TextView check = text("✓", 16, current ? Color.WHITE : 0xFF716B62, R.font.manrope_bold);
        check.setGravity(Gravity.CENTER); GradientDrawable checkBg = new GradientDrawable(); checkBg.setShape(GradientDrawable.OVAL); checkBg.setColor(current ? 0xFF0E6B3C : 0xFFEEEADF); check.setBackground(checkBg);
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(28), dp(28)); checkParams.rightMargin = dp(12); row.addView(check, checkParams);
        LinearLayout textColumn = new LinearLayout(requireContext()); textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout statusLine = new LinearLayout(requireContext()); statusLine.setGravity(Gravity.CENTER_VERTICAL);
        statusLine.addView(text(statusLabel(event.toStatus), 15, 0xFF1C1B1A, R.font.manrope_bold));
        if (current) {
            TextView pill = text("CURRENT", 10, 0xFF0A5530, R.font.manrope_extrabold); pill.setPadding(dp(9), dp(4), dp(9), dp(4));
            GradientDrawable pillBg = new GradientDrawable(); pillBg.setColor(0xFFE3F0E7); pillBg.setCornerRadius(dp(999)); pill.setBackground(pillBg);
            LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(-2, -2); pillParams.leftMargin = dp(10); statusLine.addView(pill, pillParams);
        }
        textColumn.addView(statusLine);
        TextView dateActor = text(DateFormatter.formatDateOrDash(event.date) + " · " + (event.actor == null ? "—" : event.actor), 13, 0xFF59544C, R.font.manrope_medium);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2); dateParams.topMargin = dp(2); textColumn.addView(dateActor, dateParams);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2); if (!current) rowParams.bottomMargin = dp(18); statusHistoryRows.addView(row, rowParams);
    }

    private TextView text(String value, float size, int color, int font) {
        TextView view = new TextView(requireContext()); view.setText(value); view.setTextColor(color); view.setTextSize(size);
        view.setTypeface(ResourcesCompat.getFont(requireContext(), font)); view.setIncludeFontPadding(false); return view;
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void showEditDialog() {
        if (certification == null) return;
        EditCertificationDialog dialog = EditCertificationDialog.newInstance(certificationId);
        dialog.setOnCertificationUpdatedListener(() -> { loadCertificationData(); Toast.makeText(requireContext(), "Certification updated!", Toast.LENGTH_SHORT).show(); });
        dialog.show(getParentFragmentManager(), "EditCertificationDialog");
    }

    private void confirmDelete() {
        if (certification == null) return;
        if (!sessionManager.isOwner()) { Snackbar.make(requireView(), "Only owners can delete certifications", Snackbar.LENGTH_SHORT).show(); return; }
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Certification")
                .setMessage("Are you sure you want to delete " + certification.getCertificationName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCertification()).setNegativeButton("Cancel", null).show();
    }

    private void showChangeStatusDialog() {
        if (certification == null) return;
        ChangeStatusDialog dialog = ChangeStatusDialog.newInstance(certificationId, farmId);
        dialog.setOnStatusChangedListener(() -> { loadCertificationData(); loadRelatedRecordsCounts(); Toast.makeText(requireContext(), "Status updated", Toast.LENGTH_SHORT).show(); });
        dialog.show(getParentFragmentManager(), "ChangeStatusDialog");
    }

    private void showAddAuditDialog() {
        if (certification == null) return;
        AddAuditDialog dialog = AddAuditDialog.newInstance(farmId, certificationId);
        dialog.setOnAuditAddedListener(() -> { loadRelatedRecordsCounts(); Toast.makeText(requireContext(), "Audit added for " + certification.getCertificationName(), Toast.LENGTH_SHORT).show(); });
        dialog.show(getParentFragmentManager(), "AddAuditDialog");
    }

    private void deleteCertification() {
        if (certificationRepository == null || certification == null) return;
        Toast.makeText(requireContext(), "Deleting certification...", Toast.LENGTH_SHORT).show();
        certificationRepository.deleteCertification(certification, new CertificationRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean result) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Certification and all related records deleted", Toast.LENGTH_SHORT).show());
            }
            @Override public void onError(String error) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (certificationRepository != null) certificationRepository.shutdown();
        executor.shutdown();
    }
}
