package com.afrinuts.farmos.ui.suppliers;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.local.entity.SupplierScoreSnapshotEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierCommitmentRepository;
import com.afrinuts.farmos.data.repository.SupplierDisputeRepository;
import com.afrinuts.farmos.data.repository.SupplierInspectionRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.data.repository.SupplierScoreSnapshotRepository;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tablet detail-pane counterpart of SupplierDetailActivity. */
public class SupplierDetailFragment extends Fragment {
    private static final String ARG_SUPPLIER_ID = "supplier_id";
    public interface Listener { void onSupplierDeleted(long supplierId); }
    public static SupplierDetailFragment newInstance(long id) {
        SupplierDetailFragment fragment = new SupplierDetailFragment();
        Bundle args = new Bundle(); args.putLong(ARG_SUPPLIER_ID, id); fragment.setArguments(args); return fragment;
    }

    private long supplierId, farmId = 1;
    private SupplierEntity supplier;
    private View root;
    private SupplierRepository supplierRepository;
    private SupplierCommitmentRepository commitmentRepository;
    private SupplierInspectionRepository inspectionRepository;
    private SupplierDisputeRepository disputeRepository;
    private SupplierScoreSnapshotRepository scoreRepository;
    private QualityBatchRepository qualityBatchRepository;
    private SupplierBatchAdapter batchAdapter;
    private final java.util.List<QualityBatchEntity> batches = new java.util.ArrayList<>();
    private boolean showAllBatches;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private LinearLayout commitmentRows, inspectionRows, disputeRows, scoreRows;
    private TextView commitmentsEmpty, inspectionsEmpty, disputesEmpty;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                                  @Nullable Bundle state) {
        root = inflater.inflate(R.layout.fragment_supplier_detail_tablet, container, false);
        supplierId = getArguments() == null ? -1 : getArguments().getLong(ARG_SUPPLIER_ID, -1);

        commitmentRows = root.findViewById(R.id.containerCommitments);
        inspectionRows = root.findViewById(R.id.containerInspections);
        disputeRows = root.findViewById(R.id.containerDisputes);
        commitmentsEmpty = root.findViewById(R.id.tvCommitmentsEmpty);
        inspectionsEmpty = root.findViewById(R.id.tvInspectionsEmpty);
        disputesEmpty = root.findViewById(R.id.tvDisputesEmpty);
        scoreRows = root.findViewById(R.id.scoreRows);

        RecyclerView batchesRecyclerView = root.findViewById(R.id.paneBatchesRecyclerView);
        batchesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        batchAdapter = new SupplierBatchAdapter(new java.util.ArrayList<>(), true);
        batchAdapter.setOnBatchUpdatedListener(this::loadBatches);
        batchAdapter.setOnBatchDisputeClickListener(batch -> {
            RaiseDisputeDialog dialog = RaiseDisputeDialog.newInstance(
                    supplierId, farmId, SupplierDisputeEntity.TargetEntity.BATCH,
                    batch.getId(), batch.getBatchNumber());
            dialog.setOnDisputeRaisedListener(id -> loadTrustSections());
            dialog.show(getParentFragmentManager(), "RaiseBatchDisputeDialog");
        });
        batchesRecyclerView.setAdapter(batchAdapter);
        com.google.android.material.button.MaterialButton tvShowAllBatches = root.findViewById(R.id.tvPaneShowAllBatches);
        tvShowAllBatches.setOnClickListener(v -> {
            showAllBatches = !showAllBatches;
            updateBatchList();
        });
        root.findViewById(R.id.btnPaneEditBatch).setOnClickListener(v -> editBatch());

        root.findViewById(R.id.btnShowMap).setOnClickListener(v -> openMap());
        root.findViewById(R.id.btnEditSupplier).setOnClickListener(v -> editSupplier());
        root.findViewById(R.id.btnRecordDelivery).setOnClickListener(v -> recordDelivery());
        root.findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());
        root.findViewById(R.id.btnAddCommitment).setOnClickListener(v -> addCommitment());
        root.findViewById(R.id.btnAddInspection).setOnClickListener(v -> addInspection());
        root.findViewById(R.id.chipStatus).setOnClickListener(v -> {
            if (new SessionManager(requireContext()).canEditData()) changeStatus();
        });

        supplierRepository = new SupplierRepository(requireContext(), farmId);
        commitmentRepository = new SupplierCommitmentRepository(requireContext(), farmId);
        inspectionRepository = new SupplierInspectionRepository(requireContext(), farmId);
        disputeRepository = new SupplierDisputeRepository(requireContext(), farmId);
        scoreRepository = new SupplierScoreSnapshotRepository(requireContext(), farmId);
        qualityBatchRepository = new QualityBatchRepository(requireContext(), farmId);
        if (supplierId >= 0) {
            supplierRepository.getSupplierById(supplierId).observe(getViewLifecycleOwner(), this::displaySupplier);
            loadTrustSections();
            loadBatchCount();
            loadBatches();
        }
        root.setVisibility(View.VISIBLE);
        return root;
    }

    private void hideOriginalHero() {
        View name = root.findViewById(R.id.tvSupplierName);
        View current = name;
        while (current != null && !(current instanceof CardView)) {
            if (!(current.getParent() instanceof View)) break;
            current = (View) current.getParent();
        }
        if (current != null) current.setVisibility(View.GONE);
    }

    private void hideBatchHistory() {
        View batches = root.findViewById(R.id.batchesRecyclerView);
        View current = batches;
        while (current != null && !(current instanceof CardView)) {
            if (!(current.getParent() instanceof View)) break;
            current = (View) current.getParent();
        }
        if (current != null) current.setVisibility(View.GONE);
    }

    private void addTabletHeroAndStats() {
        ScrollView scroll = root instanceof ViewGroup ? findScroll((ViewGroup) root) : null;
        if (scroll == null || !(scroll.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup content = (ViewGroup) scroll.getChildAt(0);
        LinearLayout hero = new LinearLayout(requireContext());
        hero.setOrientation(LinearLayout.HORIZONTAL); hero.setGravity(android.view.Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(20), dp(20), dp(20), dp(18));
        hero.setBackground(rounded(0xFFFFFFFF, 26));
        ImageView mark = new ImageView(requireContext());
        mark.setImageResource(R.drawable.ic_people);
        mark.setColorFilter(0xFFFFFFFF);
        mark.setBackground(rounded(0xFF0E6B3C, 48));
        mark.setPadding(dp(16), dp(16), dp(16), dp(16));
        hero.addView(mark, params(64, 64));
        LinearLayout title = new LinearLayout(requireContext()); title.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1);
        titleLp.setMargins(dp(16), 0, dp(12), 0);
        hero.addView(title, titleLp);
        TextView name = text("—", 25, 0xFF1C1B1A, true); name.setTag("supplier-pane-name");
        TextView subtitle = text("", 15, 0xFF59544C, false); subtitle.setTag("supplier-pane-subtitle");
        TextView rating = text("", 15, 0xFFD99B22, true); rating.setTag("supplier-pane-rating");
        title.addView(name); title.addView(subtitle); LinearLayout.LayoutParams ratingLp = params(-2, -2); ratingLp.topMargin = dp(4); title.addView(rating, ratingLp);
        TextView status = text("", 14, 0xFF0E6B3C, true); status.setTag("supplier-pane-status");
        status.setPadding(dp(14), dp(8), dp(14), dp(8));
        status.setBackground(rounded(0xFFE3F0E7, 28));
        hero.addView(status, params(-2, -2));
        content.addView(hero, 0, params(-1, -2));

        LinearLayout stats = new LinearLayout(requireContext()); stats.setOrientation(LinearLayout.HORIZONTAL); stats.setTag("supplier-pane-stats");
        LinearLayout.LayoutParams statsLp = params(-1, -2); statsLp.topMargin = dp(12); statsLp.bottomMargin = dp(12);
        String[] labels = {"Trust score", "Batches", "Total volume"};
        String[] values = {"—", "—", "— kg"};
        for (int i = 0; i < labels.length; i++) {
            CardView card = new CardView(requireContext()); card.setRadius(dp(20)); card.setCardElevation(0);
            card.setCardBackgroundColor(i == 0 ? 0xFF0E6B3C : 0xFFF6F1E7);
            LinearLayout box = new LinearLayout(requireContext()); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16), dp(14), dp(12), dp(14));
            TextView label = text(labels[i], 13, i == 0 ? 0xFFD2E6DA : 0xFF59544C, false);
            TextView value = text(values[i], 21, i == 0 ? 0xFFFFFFFF : 0xFF1C1B1A, true); value.setTag("supplier-pane-stat-" + i);
            box.addView(label); box.addView(value); card.addView(box);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1); lp.setMarginEnd(i < 2 ? dp(10) : 0); stats.addView(card, lp);
        }
        content.addView(stats, 1, statsLp);
    }

    private ScrollView findScroll(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ScrollView) return (ScrollView) child;
            if (child instanceof ViewGroup) { ScrollView found = findScroll((ViewGroup) child); if (found != null) return found; }
        }
        return null;
    }

    private void styleDetailSurface() {
        ScrollView scroll = findScroll((ViewGroup) root);
        if (scroll == null) return;
        scroll.setBackground(rounded(0xFFFFFFFF, 28)); scroll.setClipToOutline(true);
        scroll.setPadding(dp(16), dp(16), dp(16), dp(24));
    }

    private void displaySupplier(SupplierEntity value) {
        if (value == null || root == null) return;
        supplier = value;
        farmId = value.getFarmId();
        ((TextView) root.findViewById(R.id.tvSupplierName)).setText(value.getName());
        ((TextView) root.findViewById(R.id.tvSupplierType)).setText(value.getTypeDisplay());
        ((TextView) root.findViewById(R.id.tvRating)).setText(value.getFormattedRating());
        ((Chip) root.findViewById(R.id.chipStatus)).setText(value.getStatusDisplay());
        ((TextView) root.findViewById(R.id.tvContactPerson)).setText(or(value.getContactPerson(), "No contact"));
        ((TextView) root.findViewById(R.id.tvPhone)).setText(or(value.getPhone(), "No phone"));
        ((TextView) root.findViewById(R.id.tvEmail)).setText(or(value.getEmail(), "No email"));
        ((TextView) root.findViewById(R.id.tvLocation)).setText(or(value.getLocation(), "No location"));
        ((TextView) root.findViewById(R.id.tvAnnualVolume)).setText(String.format(Locale.getDefault(), "%,.0f kg", value.getAnnualVolumeKg()));
        ((TextView) root.findViewById(R.id.tvAvgQualityScore)).setText(String.format(Locale.getDefault(), "%.0f", value.getAverageQualityScore()));
        ((TextView) root.findViewById(R.id.tvFarmSize)).setText(String.format(Locale.getDefault(), "%.1f ha", value.getFarmSize()));
        setText(R.id.tvYearsFarming, value.getFormattedYearsFarming());
        setText(R.id.tvQuote, TextUtils.isEmpty(value.getQuote()) ? "" : "“" + value.getQuote() + "”");
        setText(R.id.tvBio, or(value.getBio(), ""));
        View profile = root.findViewById(R.id.layoutFarmerProfile); if (profile != null) profile.setVisibility(value.getFarmSize() > 0 || !TextUtils.isEmpty(value.getBio()) || !TextUtils.isEmpty(value.getQuote()) ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.layoutFirebreak).setVisibility(value.isFirebreakProtection() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.layoutSustainable).setVisibility(value.isSustainableHarvesting() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.layoutChildLabor).setVisibility(value.isNoChildLabor() ? View.VISIBLE : View.GONE);
        View cert = root.findViewById(R.id.layoutCertification);
        cert.setVisibility(View.VISIBLE);
        if (value.isCertified()) {
            setText(R.id.tvCertification, or(value.getCertificationType(), "Certified"));
            setText(R.id.tvCertificationExpiry, value.getCertificationExpiryDate() > 0 ? "Expires " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(value.getCertificationExpiryDate()) : "No expiry");
        } else {
            setText(R.id.tvCertification, "Not certified");
            setText(R.id.tvCertificationExpiry, "");
        }
        ((MaterialButton) root.findViewById(R.id.btnShowMap)).setVisibility(!TextUtils.isEmpty(value.getLocation()) || value.hasLocation() ? View.VISIBLE : View.GONE);
        SessionManager session = new SessionManager(requireContext());
        root.findViewById(R.id.btnDelete).setVisibility(session.canEditData() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.btnEditSupplier).setVisibility(session.canEditData() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.btnPaneEditBatch).setVisibility(session.canEditData() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.chipStatus).setClickable(session.canEditData());
        TextView volume = root.findViewById(R.id.tvPaneVolume); if (volume != null) volume.setText(String.format(Locale.getDefault(), "%,.0f kg", value.getAnnualVolumeKg()));
    }

    private void loadBatchCount() {
        executor.execute(() -> {
            int count = AppDatabase.getInstance(requireContext()).qualityBatchDao().getBatchCountBySupplierId(supplierId);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> { TextView batches = root == null ? null : root.findViewById(R.id.tvPaneBatchCount); if (batches != null) batches.setText(String.valueOf(count)); });
        });
    }

    private void loadBatches() {
        executor.execute(() -> {
            java.util.List<QualityBatchEntity> loaded = qualityBatchRepository.getBatchesBySupplierIdSync(supplierId);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (root == null) return;
                batches.clear();
                if (loaded != null) batches.addAll(loaded);
                double totalWeight = 0;
                double totalKor = 0;
                int korSamples = 0;
                int premiumCount = 0;
                int standardCount = 0;
                for (QualityBatchEntity batch : batches) {
                    totalWeight += batch.getWeightKg();
                    if (batch.getKorGPerKg() > 0) {
                        totalKor += batch.getKorGPerKg();
                        korSamples++;
                    }
                    if (batch.getQualityGrade() == QualityBatchEntity.QualityGrade.PREMIUM) premiumCount++;
                    else if (batch.getQualityGrade() == QualityBatchEntity.QualityGrade.STANDARD) standardCount++;
                }
                TextView count = root.findViewById(R.id.tvPaneBatchStatsCount);
                count.setText(batches.size() + " total");
                ((TextView) root.findViewById(R.id.tvPaneBatchVolume)).setText(
                        String.format(Locale.getDefault(), "%,.0f kg", totalWeight));
                ((TextView) root.findViewById(R.id.tvPaneBatchAverageKor)).setText(korSamples == 0
                        ? "—" : String.format(Locale.getDefault(), "%.0f g/kg", totalKor / korSamples));
                ((TextView) root.findViewById(R.id.tvPaneBatchPremiumCount)).setText(premiumCount + " of " + batches.size());
                ((TextView) root.findViewById(R.id.tvPaneGradeLegendPremium)).setText(
                        "Premium · " + premiumCount + " batches");
                ((TextView) root.findViewById(R.id.tvPaneGradeLegendStandard)).setText(
                        "Standard · " + standardCount + " batches");
                TextView paneVolume = root.findViewById(R.id.tvPaneVolume);
                TextView annualVolume = root.findViewById(R.id.tvAnnualVolume);
                String formattedVolume = String.format(Locale.getDefault(), "%,.0f kg", totalWeight);
                if (paneVolume != null) paneVolume.setText(formattedVolume);
                if (annualVolume != null) annualVolume.setText(formattedVolume);

                float remainderWeight = Math.max(0, batches.size() - premiumCount);
                LinearLayout.LayoutParams premiumParams = (LinearLayout.LayoutParams)
                        root.findViewById(R.id.viewPanePremiumBar).getLayoutParams();
                premiumParams.width = 0;
                premiumParams.weight = premiumCount;
                root.findViewById(R.id.viewPanePremiumBar).setLayoutParams(premiumParams);
                LinearLayout.LayoutParams otherParams = (LinearLayout.LayoutParams)
                        root.findViewById(R.id.viewPaneOtherGradeBar).getLayoutParams();
                otherParams.width = 0;
                otherParams.weight = remainderWeight;
                root.findViewById(R.id.viewPaneOtherGradeBar).setLayoutParams(otherParams);
                updateBatchList();
            });
        });
    }

    private void updateBatchList() {
        if (batchAdapter == null) return;
        int visibleCount = showAllBatches ? batches.size() : Math.min(5, batches.size());
        batchAdapter.updateBatches(new java.util.ArrayList<>(batches.subList(0, visibleCount)));
        boolean hasBatches = !batches.isEmpty();
        if (root != null) {
            root.findViewById(R.id.tvPaneBatchEmpty).setVisibility(hasBatches ? View.GONE : View.VISIBLE);
            root.findViewById(R.id.layoutPaneBatchSummary).setVisibility(hasBatches ? View.VISIBLE : View.GONE);
            root.findViewById(R.id.layoutPaneBatchGradeSplit).setVisibility(hasBatches ? View.VISIBLE : View.GONE);
            root.findViewById(R.id.paneBatchesRecyclerView).setVisibility(hasBatches ? View.VISIBLE : View.GONE);
        }
        TextView showing = root == null ? null : root.findViewById(R.id.tvPaneBatchShowing);
        if (showing != null) {
            showing.setVisibility(batches.size() > 5 ? View.VISIBLE : View.GONE);
            showing.setText(showAllBatches
                    ? "Showing all " + batches.size() + " batches · newest first"
                    : "Showing latest 5 of " + batches.size() + " · newest first");
        }
        com.google.android.material.button.MaterialButton toggle = root == null ? null : root.findViewById(R.id.tvPaneShowAllBatches);
        if (toggle == null) return;
        if (batches.size() <= 5) {
            toggle.setVisibility(View.GONE);
        } else {
            toggle.setVisibility(View.VISIBLE);
            toggle.setText(showAllBatches ? "Show less — latest 5 only" : "Show all " + batches.size() + " batches");
            toggle.setIconResource(showAllBatches ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        }
    }

    private void editBatch() {
        if (batches.isEmpty()) {
            Toast.makeText(requireContext(), "No batches to edit. Record a delivery first.", Toast.LENGTH_SHORT).show();
            return;
        }
        SelectBatchToEditDialog dialog = SelectBatchToEditDialog.newInstance(supplierId);
        dialog.setOnBatchSelectedListener(batch -> {
            EditRecordDeliveryDialog editDialog = EditRecordDeliveryDialog.newInstance(batch.getId());
            editDialog.setOnDeliveryUpdatedListener(this::loadBatches);
            editDialog.show(getParentFragmentManager(), "EditRecordDeliveryDialog");
        });
        dialog.show(getParentFragmentManager(), "SelectBatchToEditDialog");
    }

    private void loadTrustSections() {
        commitmentRepository.getBySupplierId(supplierId).thenAccept(rows -> onUi(() -> renderSimpleRows(commitmentRows, commitmentsEmpty, rows, "No commitments yet.")));
        inspectionRepository.getBySupplierId(supplierId).thenAccept(rows -> onUi(() -> renderSimpleRows(inspectionRows, inspectionsEmpty, rows, "No inspections yet.")));
        disputeRepository.getBySupplierId(supplierId).thenAccept(rows -> onUi(() -> renderSimpleRows(disputeRows, disputesEmpty, rows, "No disputes.")));
        scoreRepository.getLatestLive(supplierId).observe(getViewLifecycleOwner(), this::renderScore);
    }

    private void renderSimpleRows(LinearLayout container, TextView empty, java.util.List<?> rows, String message) {
        if (container == null || empty == null) return;
        container.removeAllViews();
        empty.setVisibility(rows == null || rows.isEmpty() ? View.VISIBLE : View.GONE);
        if (rows == null) return;
        for (Object row : rows) {
            String description;
            if (row instanceof SupplierCommitmentEntity) {
                SupplierCommitmentEntity c = (SupplierCommitmentEntity) row;
                description = or(c.getContractId(), "Commitment") + " · " + c.getSeason() + " · "
                        + String.format(Locale.getDefault(), "%,.0f kg", c.getCommittedKg()) + " · "
                        + (c.getStatus() == null ? "—" : c.getStatus().name());
            } else if (row instanceof SupplierInspectionEntity) {
                SupplierInspectionEntity inspection = (SupplierInspectionEntity) row;
                description = (inspection.getInspectionType() == null ? "Inspection" : inspection.getInspectionType().name())
                        + " · " + (inspection.getOutcome() == null ? "—" : inspection.getOutcome().name())
                        + " · " + dateFormat.format(new Date(inspection.getInspectionDate()))
                        + (TextUtils.isEmpty(inspection.getFindings()) ? "" : "\n" + inspection.getFindings());
            } else if (row instanceof SupplierDisputeEntity) {
                SupplierDisputeEntity dispute = (SupplierDisputeEntity) row;
                description = (dispute.getTargetEntity() == null ? "Record" : dispute.getTargetEntity().name())
                        + " · " + (dispute.getReviewStatus() == null ? "OPEN" : dispute.getReviewStatus().name())
                        + (TextUtils.isEmpty(dispute.getReason()) ? "" : "\n" + dispute.getReason());
            } else description = String.valueOf(row);
            TextView line = text(description, 14, 0xFF59544C, false);
            line.setPadding(0, dp(8), 0, dp(8)); container.addView(line);
        }
    }

    private void renderScore(SupplierScoreSnapshotEntity score) {
        if (score == null) {
            setText(R.id.tvOverallScore, "—");
            setText(R.id.tvScoreDenominator, "/ 100");
            setText(R.id.tvPaneTrustScore, "—");
            setText(R.id.tvConfidence, "No score yet");
            setText(R.id.tvScoreMeta, "");
            if (scoreRows != null) scoreRows.removeAllViews();
            return;
        }
        setText(R.id.tvOverallScore, score.getOverall() == null ? "—" : String.format(Locale.getDefault(), "%.0f", score.getOverall()));
        setText(R.id.tvScoreDenominator, "/ 100");
        setText(R.id.tvPaneTrustScore, score.getOverall() == null ? "—" : String.format(Locale.getDefault(), "%.0f", score.getOverall()));
        setText(R.id.tvConfidence, score.getConfidence() == null ? "—" : "Confidence: " + score.getConfidence().name());
        setText(R.id.tvScoreMeta, "v" + or(score.getScoringVersion(), "?") + " · " + dateFormat.format(new Date(score.getCalculatedAt())) + " · " + score.getVerifiedTransactions() + " verified transactions");
        if (scoreRows != null) {
            scoreRows.removeAllViews();
            addScore("Quality", "25%", score.getQuality()); addScore("Delivery", "20%", score.getDelivery());
            addScore("Traceability", "20%", score.getTraceability()); addScore("Compliance", "15%", score.getCompliance());
            addScore("Sustainability", "10%", score.getSustainability()); addScore("Cooperation", "10%", score.getCooperation());
        }
    }

    private void addScore(String title, String weight, Double value) {
        if (value == null || scoreRows == null) return;
        LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(0, dp(8), 0, dp(8));
        LinearLayout labelRow = new LinearLayout(requireContext());
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView label = text(title, 17, 0xFF3F3C37, true);
        TextView share = text(" " + weight, 16, 0xFF716B61, true);
        TextView scoreValue = text(String.format(Locale.getDefault(), "%.0f", value), 17, 0xFF1C1B1A, true);
        labelRow.addView(label);
        labelRow.addView(share);
        labelRow.addView(scoreValue, new LinearLayout.LayoutParams(0, -2, 1));
        scoreValue.setGravity(android.view.Gravity.END);
        row.addView(labelRow);
        android.widget.ProgressBar bar = new android.widget.ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100); bar.setProgress((int) Math.round(value));
        GradientDrawable track = new GradientDrawable();
        track.setColor(0xFFF3EBDD);
        track.setCornerRadius(dp(6));
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(0xFF0E6B3C);
        fill.setCornerRadius(dp(6));
        ClipDrawable clippedFill = new ClipDrawable(fill, android.view.Gravity.LEFT, ClipDrawable.HORIZONTAL);
        LayerDrawable progressDrawable = new LayerDrawable(new android.graphics.drawable.Drawable[]{track, clippedFill});
        progressDrawable.setId(0, android.R.id.background);
        progressDrawable.setId(1, android.R.id.progress);
        bar.setProgressDrawable(progressDrawable);
        LinearLayout.LayoutParams barParams = params(-1, 12);
        barParams.topMargin = dp(7);
        row.addView(bar, barParams); scoreRows.addView(row);
    }

    private void addCommitment() {
        RecordCommitmentDialog dialog = RecordCommitmentDialog.newInstance(supplierId, farmId);
        dialog.setOnCommitmentCreatedListener(id -> loadTrustSections()); dialog.show(getParentFragmentManager(), "RecordCommitmentDialog");
    }
    private void addInspection() {
        RecordInspectionDialog dialog = RecordInspectionDialog.newInstance(supplierId, farmId);
        dialog.setOnInspectionCreatedListener(id -> loadTrustSections()); dialog.show(getParentFragmentManager(), "RecordInspectionDialog");
    }
    private void recordDelivery() {
        RecordDeliveryDialog dialog = RecordDeliveryDialog.newInstance(supplierId);
        dialog.setOnDeliveryRecordedListener(() -> { loadBatchCount(); loadBatches(); }); dialog.show(getParentFragmentManager(), "RecordDeliveryDialog");
    }
    private void editSupplier() {
        EditSupplierDialog dialog = EditSupplierDialog.newInstance(supplierId);
        dialog.setOnSupplierUpdatedListener(() -> supplierRepository.getSupplierById(supplierId).observe(getViewLifecycleOwner(), this::displaySupplier));
        dialog.show(getParentFragmentManager(), "EditSupplierDialog");
    }
    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Supplier")
                .setMessage("Are you sure you want to delete " + (supplier == null ? "this supplier" : supplier.getName()) + "? This action cannot be undone.")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete", (d, w) -> {
                    if (supplier == null) return;
                    supplierRepository.deleteSupplier(supplier, new SupplierRepository.RepositoryCallback<Boolean>() {
                        @Override public void onSuccess(Boolean result) { onUi(() -> { if (getActivity() instanceof Listener) ((Listener) getActivity()).onSupplierDeleted(supplierId); }); }
                        @Override public void onError(String error) { onUi(() -> Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()); }
                    });
                }).show();
    }
    private void changeStatus() {
        String[] labels = {"Active", "Pending", "Suspended", "Inactive"};
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Change Status").setItems(labels, (d, which) -> {
            SupplierEntity.SupplierStatus[] values = SupplierEntity.SupplierStatus.values();
            supplier.setStatus(values[which]); supplier.setActive(values[which] == SupplierEntity.SupplierStatus.ACTIVE);
            supplierRepository.updateSupplier(supplier, new SupplierRepository.RepositoryCallback<Boolean>() {
                @Override public void onSuccess(Boolean result) { onUi(() -> displaySupplier(supplier)); }
                @Override public void onError(String error) { onUi(() -> Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()); }
            });
        }).show();
    }
    private void openMap() {
        if (supplier == null) return;
        String query = supplier.hasLocation() ? supplier.getLatitude() + "," + supplier.getLongitude() : Uri.encode(or(supplier.getLocation(), ""));
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + query)));
    }

    private void onUi(Runnable runnable) { if (!isAdded()) return; requireActivity().runOnUiThread(() -> { if (isAdded()) runnable.run(); }); }
    private void setText(int id, String value) { TextView view = root == null ? null : root.findViewById(id); if (view != null) view.setText(value); }
    private String or(String value, String fallback) { return TextUtils.isEmpty(value) ? fallback : value; }
    private TextView text(String value, float size, int color, boolean bold) { TextView t = new TextView(requireContext()); t.setText(value); t.setTextSize(size); t.setTextColor(color); if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); return t; }
    private LinearLayout.LayoutParams params(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private android.graphics.drawable.GradientDrawable rounded(int color, int radius) { android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }

    @Override public void onDestroyView() { super.onDestroyView(); root = null; }
    @Override public void onDestroy() {
        super.onDestroy(); executor.shutdown();
        if (supplierRepository != null) supplierRepository.shutdown();
        if (commitmentRepository != null) commitmentRepository.shutdown();
        if (inspectionRepository != null) inspectionRepository.shutdown();
        if (disputeRepository != null) disputeRepository.shutdown();
        if (scoreRepository != null) scoreRepository.shutdown();
        if (qualityBatchRepository != null) qualityBatchRepository.shutdown();
    }
}
