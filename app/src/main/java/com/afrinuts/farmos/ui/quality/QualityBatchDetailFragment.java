package com.afrinuts.farmos.ui.quality;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.utils.SessionManager;
import com.afrinuts.farmos.utils.QRCodeGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tablet host for the phone batch detail content. */
public class QualityBatchDetailFragment extends Fragment {
    private static final String ARG_BATCH_ID = "batch_id";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long batchId;
    private long farmId;
    private QualityBatchEntity batch;
    private SupplierEntity supplier;
    private QualityBatchRepository repository;
    private SupplierRepository supplierRepository;
    private AppDatabase database;
    private SessionManager session;
    private View root;
    private TextView tvBatchNumber, tvBatchDate, tvGrade, tvBatchType;
    private TextView tvMoisture, tvDefective, tvNutCount, tvKernel, tvWeight;
    private TextView tvSupplierName, tvSupplierLocation, tvSupplierCount, tvReceivedAt;
    private TextView tvAflatoxin, tvPesticide, tvHeavyMetals, tvNotes;
    private TextView tvKor, tvRcnMoisture, tvRcnNutCount, tvMouldy, tvImmature;
    private TextView tvInsectDamaged, tvShrivelled, tvTotalDefective, tvForeignMatter, tvDryingMethod;
    private TextView tvSustainabilityScore, tvVerifiedDate, tvNoChildLabor, tvFarmOs, tvFirebreak, tvSustainable;
    private CardView cardKernel, cardRcn, cardTests, cardNotes, cardSustainability;
    private MaterialButton btnEdit, btnTrace;
    private int supplierBatchCount;
    private boolean tablet;
    private boolean tabletHeaderInline;

    public static QualityBatchDetailFragment newInstance(long batchId) {
        QualityBatchDetailFragment fragment = new QualityBatchDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_BATCH_ID, batchId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.activity_quality_batch_detail, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        root = view;
        tablet = getResources().getConfiguration().screenWidthDp >= 600;
        batchId = getArguments() == null ? -1 : getArguments().getLong(ARG_BATCH_ID, -1);
        session = new SessionManager(requireContext());
        database = AppDatabase.getInstance(requireContext());
        bindViews(view);
        styleTabletCard(view);
        setupActions();
        loadBatch();
    }

    private void bindViews(View view) {
        View toolbar = view.findViewById(R.id.farmDetailHeader);
        if (toolbar != null) toolbar.setVisibility(View.GONE);
        tvBatchNumber = view.findViewById(R.id.tvBatchNumber);
        tvBatchDate = view.findViewById(R.id.tvBatchDate);
        tvGrade = view.findViewById(R.id.tvGrade);
        tvBatchType = view.findViewById(R.id.tvBatchType);
        tvMoisture = view.findViewById(R.id.tvMoistureValue);
        tvDefective = view.findViewById(R.id.tvDefectiveValue);
        tvNutCount = view.findViewById(R.id.tvNutCountValue);
        tvKernel = view.findViewById(R.id.tvKernelValue);
        tvWeight = view.findViewById(R.id.tvWeightValue);
        tvSupplierName = view.findViewById(R.id.tvSupplierName);
        tvSupplierLocation = view.findViewById(R.id.tvSupplierLocation);
        tvSupplierCount = view.findViewById(R.id.tvSupplierBatchCount);
        tvReceivedAt = view.findViewById(R.id.tvReceivedAt);
        tvAflatoxin = view.findViewById(R.id.tvAflatoxinValue);
        tvPesticide = view.findViewById(R.id.tvPesticideValue);
        tvHeavyMetals = view.findViewById(R.id.tvHeavyMetalsValue);
        tvNotes = view.findViewById(R.id.tvNotesValue);
        tvKor = view.findViewById(R.id.tvKorValue);
        tvRcnMoisture = view.findViewById(R.id.tvRcnMoistureValue);
        tvRcnNutCount = view.findViewById(R.id.tvRcnNutCountValue);
        tvMouldy = view.findViewById(R.id.tvMouldyRate);
        tvImmature = view.findViewById(R.id.tvImmatureRate);
        tvInsectDamaged = view.findViewById(R.id.tvInsectDamagedRate);
        tvShrivelled = view.findViewById(R.id.tvShrivelledRate);
        tvTotalDefective = view.findViewById(R.id.tvTotalDefective);
        tvForeignMatter = view.findViewById(R.id.tvForeignMatter);
        tvDryingMethod = view.findViewById(R.id.tvDryingMethod);
        tvSustainabilityScore = view.findViewById(R.id.tvSustainabilityScore);
        tvVerifiedDate = view.findViewById(R.id.tvVerifiedDate);
        tvNoChildLabor = view.findViewById(R.id.tvNoChildLabor);
        tvFarmOs = view.findViewById(R.id.tvFarmOsMonitored);
        tvFirebreak = view.findViewById(R.id.tvFirebreakProtection);
        tvSustainable = view.findViewById(R.id.tvSustainableHarvesting);
        cardKernel = view.findViewById(R.id.cardKernelMetrics);
        cardRcn = view.findViewById(R.id.cardRcnMetrics);
        cardTests = view.findViewById(R.id.cardTestResults);
        cardNotes = view.findViewById(R.id.cardNotes);
        cardSustainability = view.findViewById(R.id.layoutSustainability);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnTrace = view.findViewById(R.id.btnGenerateTraceability);
    }

    private void styleTabletCard(View view) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(android.graphics.Color.WHITE);
        background.setCornerRadius(getResources().getDisplayMetrics().density * 28f);
        view.setBackground(background);
        view.setClipToOutline(true);
        View scroll = ((ViewGroup) view).getChildAt(1);
        if (scroll != null) {
            scroll.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            scroll.setClipToOutline(false);
        }
    }

    private void setupActions() {
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnTrace.setVisibility(View.GONE);
        btnTrace.setOnClickListener(v -> {
            if (batch != null && btnTrace.getText().toString().equalsIgnoreCase("View trace")) openTraceability();
            else generateTraceability();
        });
        View supplierLink = root.findViewById(R.id.tvSupplierSeeAll);
        if (supplierLink != null) supplierLink.setOnClickListener(v -> viewSupplier());
    }

    private void loadBatch() {
        if (batchId < 0 || root == null) return;
        executor.execute(() -> {
            try {
                QualityBatchEntity loaded = database.qualityBatchDao().getBatchById(batchId);
                if (loaded == null || !isAdded()) return;
                farmId = loaded.getFarmId();
                QualityBatchRepository batches = new QualityBatchRepository(requireContext(), farmId);
                SupplierRepository suppliers = new SupplierRepository(requireContext(), farmId);
                SupplierEntity loadedSupplier = loaded.getSupplierId() == null ? null
                        : suppliers.getSupplierByIdSync(loaded.getSupplierId());
                int count = loaded.getSupplierId() == null ? 0
                        : database.qualityBatchDao().getBatchCountBySupplierId(loaded.getSupplierId());
                BatchTraceabilityEntity trace = database.batchTraceabilityDao().getByQualityBatchId(batchId);
                requireActivity().runOnUiThread(() -> {
                    if (root == null || !isAdded()) return;
                    batch = loaded;
                    supplier = loadedSupplier;
                    repository = batches;
                    supplierRepository = suppliers;
                    supplierBatchCount = count;
                    render(trace != null);
                });
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Unable to load batch details", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void render(boolean hasTrace) {
        if (batch == null || root == null) return;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        long date = batch.getRelevantDate();
        String dateText = date > 0 ? dateFormat.format(date) : "—";
        tvBatchNumber.setText(batch.getBatchNumber() == null ? "—" : batch.getBatchNumber());
        String supplierLabel = supplier == null || supplier.getName() == null ? "" : supplier.getName();
        String coop = supplier != null && supplier.getCooperativeName() != null && !supplier.getCooperativeName().trim().isEmpty()
                ? supplier.getCooperativeName().trim() : batch.getSourceType() == null ? "" : batch.getSourceType().getDisplayName();
        StringBuilder subtitle = new StringBuilder();
        if (!supplierLabel.isEmpty()) subtitle.append(supplierLabel);
        if (!coop.isEmpty()) { if (subtitle.length() > 0) subtitle.append(" · "); subtitle.append(coop); }
        if (!dateText.equals("—")) { if (subtitle.length() > 0) subtitle.append(" · "); subtitle.append(dateText); }
        tvBatchDate.setText(subtitle.length() == 0 ? dateText : subtitle.toString());
        tvGrade.setText(batch.getQualityGrade().getDisplayName());
        int gradeColor = batch.getQualityGrade() == QualityBatchEntity.QualityGrade.PREMIUM ? 0xFF0A5530
                : batch.getQualityGrade() == QualityBatchEntity.QualityGrade.STANDARD ? 0xFF7A5F22 : 0xFFB3402A;
        tvGrade.setTextColor(gradeColor);
        int gradeFill = batch.getQualityGrade() == QualityBatchEntity.QualityGrade.PREMIUM ? 0xFFE3F0E7
                : batch.getQualityGrade() == QualityBatchEntity.QualityGrade.STANDARD ? 0xFFF3EAD8 : 0xFFF8E4DD;
        if (tvGrade instanceof Chip) ((Chip) tvGrade).setChipBackgroundColor(android.content.res.ColorStateList.valueOf(gradeFill));
        String bn = batch.getBatchNumber() == null ? "" : batch.getBatchNumber();
        if (bn.contains("-ORG-")) tvBatchType.setText("Organic");
        else if (bn.contains("-CONV-")) tvBatchType.setText("Conventional");
        else if (bn.contains("-RCN-")) tvBatchType.setText("RCN");
        else tvBatchType.setText("—");

        String cooperative = supplier != null && supplier.getCooperativeName() != null ? supplier.getCooperativeName()
                : batch.getSourceType() == null ? "—" : batch.getSourceType().getDisplayName();
        root.<TextView>findViewById(R.id.tvSourceType).setText(cooperative);
        tvSupplierName.setText(supplierLabel.isEmpty() ? (batch.getSourceType() == QualityBatchEntity.SourceType.OWN_FARM ? "AfriNuts farm" : "—") : supplierLabel);
        String location = supplier != null && supplier.getLocation() != null && !supplier.getLocation().trim().isEmpty() ? supplier.getLocation().trim() : "—";
        tvSupplierLocation.setText(location);
        tvSupplierCount.setText(String.valueOf(supplierBatchCount));
        tvReceivedAt.setText(location);
        root.<TextView>findViewById(R.id.tvDeliveryBatch).setText(bn.isEmpty() ? "—" : bn);
        root.<TextView>findViewById(R.id.tvDeliveryDate).setText(dateText);
        tvWeight.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));
        root.<TextView>findViewById(R.id.tvWeightValue).setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));

        double score = batch.isKernel() ? batch.getKernelQualityScore() : batch.getRcnQualityScore();
        root.<TextView>findViewById(R.id.tvQualityScoreLab).setText(String.format(Locale.getDefault(), "%.0f / 100", score));
        btnTrace.setVisibility(hasTrace || session.canEditData() ? View.VISIBLE : View.GONE);
        btnTrace.setText(hasTrace ? "View trace" : "Generate traceability");
        if (!session.canEditData()) btnEdit.setVisibility(View.GONE);

        cardKernel.setVisibility(View.VISIBLE);
        tvMoisture.setText(String.format(Locale.getDefault(), "%.1f", batch.getMoistureContent()));
        tvDefective.setText(String.format(Locale.getDefault(), "%.1f", batch.getDefectiveRate()));
        tvNutCount.setText(String.valueOf(batch.getNutCountPerKg()));
        tvKernel.setText(String.format(Locale.getDefault(), "%.1f", batch.isRcn() ? batch.getKorGPerKg() : batch.getKernelRatio()));
        root.<TextView>findViewById(R.id.tvKernelValueUnit).setText(batch.isRcn() ? "g/kg" : "%");
        if (batch.isKernel()) {
            if (batch.getAflatoxinPpb() > 0 || batch.getPesticideResiduePpb() > 0 || batch.getHeavyMetalsPpb() > 0) {
                tvAflatoxin.setText(String.format(Locale.getDefault(), "%.2f", batch.getAflatoxinPpb()));
                tvPesticide.setText(String.format(Locale.getDefault(), "%.2f", batch.getPesticideResiduePpb()));
                tvHeavyMetals.setText(String.format(Locale.getDefault(), "%.2f", batch.getHeavyMetalsPpb()));
            } else {
                QualityBatchEntity.TestResults legacy = QualityBatchEntity.TestResults.fromJson(batch.getTestResults());
                if (legacy.hasResults()) {
                    tvAflatoxin.setText(String.format(Locale.getDefault(), "%.2f", legacy.getAflatoxin()));
                    tvPesticide.setText(String.format(Locale.getDefault(), "%.2f", legacy.getPesticideResidue()));
                    tvHeavyMetals.setText(String.format(Locale.getDefault(), "%.2f", legacy.getHeavyMetals()));
                } else {
                    tvAflatoxin.setText("—"); tvPesticide.setText("—"); tvHeavyMetals.setText("—");
                }
            }
        } else {
            tvAflatoxin.setText("—"); tvPesticide.setText("—"); tvHeavyMetals.setText("—");
        }
        cardTests.setVisibility(View.VISIBLE);
        tvKor.setText(String.format(Locale.getDefault(), "%.0f g/kg", batch.getKorGPerKg()));
        tvRcnMoisture.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()));
        tvRcnNutCount.setText(batch.getNutCountPerKg() + "/kg");
        tvMouldy.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getMouldyRate()));
        tvImmature.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getImmatureRate()));
        tvInsectDamaged.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getInsectDamagedRate()));
        tvShrivelled.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getShrivelledRate()));
        tvTotalDefective.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getDefectiveRate()));
        tvForeignMatter.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getForeignMatter()));
        tvDryingMethod.setText(batch.getDryingMethod() == null ? "—" : batch.getDryingMethod());
        cardRcn.setVisibility(View.GONE);
        if (batch.isVerified()) {
            cardSustainability.setVisibility(View.VISIBLE);
            tvSustainabilityScore.setText(batch.getSustainabilityScore() + "%");
            tvVerifiedDate.setText(batch.getFormattedVerifiedDate());
            setYesNo(tvNoChildLabor, batch.isNoChildLabor()); setYesNo(tvFarmOs, batch.isFarmOsMonitored());
            setYesNo(tvFirebreak, batch.isFirebreakProtection()); setYesNo(tvSustainable, batch.isSustainableHarvesting());
        } else cardSustainability.setVisibility(View.GONE);
        if (batch.getNotes() != null && !batch.getNotes().isEmpty()) { cardNotes.setVisibility(View.VISIBLE); tvNotes.setText(batch.getNotes()); }
        else cardNotes.setVisibility(View.GONE);
        if (tablet) renderTabletSections();
    }

    /** Renders the tablet detail in the section order and hierarchy shown in the master-detail wireframe. */
    private void renderTabletSections() {
        if (!tabletHeaderInline) {
            inlineTabletHeader();
            tabletHeaderInline = true;
        }
        View scroll = ((ViewGroup) root).getChildAt(1);
        if (!(scroll instanceof android.widget.ScrollView)) return;
        ViewGroup body = (ViewGroup) ((android.widget.ScrollView) scroll).getChildAt(0);
        if (body.getChildCount() < 2) return;
        View hero = body.getChildAt(0);
        View actions = btnEdit.getParent() instanceof View ? (View) btnEdit.getParent() : null;
        if (actions != null) ((ViewGroup) actions.getParent()).removeView(actions);
        body.removeAllViews();
        body.addView(hero);
        body.addView(createSummaryCards());

        LinearLayout sections = new LinearLayout(requireContext());
        sections.setOrientation(LinearLayout.VERTICAL);
        sections.setPadding(dp(0), dp(8), dp(0), dp(8));
        addSectionTitle(sections, "RCN metrics");
        addMetric(sections, "Moisture", String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()));
        addMetric(sections, "Nut count", batch.getNutCountPerKg() + " / kg");
        addMetric(sections, "Defective", String.format(Locale.getDefault(), "%.1f%%", batch.getDefectiveRate()));
        addMetric(sections, "Weight received", String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));

        addSectionTitle(sections, "Kernel metrics");
        addMetric(sections, "Kernel ratio", String.format(Locale.getDefault(), "%.1f%%", batch.getKernelRatio()));
        addMetric(sections, "KOR", String.format(Locale.getDefault(), "%.0f g/kg", batch.getKorGPerKg()));
        double score = batch.isKernel() ? batch.getKernelQualityScore() : batch.getRcnQualityScore();
        addMetric(sections, "Quality score", String.format(Locale.getDefault(), "%.0f / 100", score));

        addSectionTitle(sections, "Test results");
        addMetric(sections, "Aflatoxin", testResultText(tvAflatoxin));
        addMetric(sections, "Pesticide", testResultText(tvPesticide));
        addMetric(sections, "Heavy metals", testResultText(tvHeavyMetals));

        addSectionTitle(sections, "KOR audit");
        addMetric(sections, "Cut-test sample", String.format(Locale.getDefault(), "%.0f g", batch.getKorSampleGrams()));
        addMetric(sections, "Good kernels", String.format(Locale.getDefault(), "%.0f g", batch.getKorKernelGrams()));
        String korGrade = batch.getKorGPerKg() >= 272 ? "Premium (≥ 272 g/kg)"
                : batch.getKorGPerKg() >= 249 ? "Standard (249–271 g/kg)" : "Reject (< 249 g/kg)";
        addMetric(sections, "KOR grade", korGrade);

        addSectionTitle(sections, "Supplier");
        addMetric(sections, "Name", supplier == null || supplier.getName() == null ? "—" : supplier.getName());
        addMetric(sections, "Cooperative", supplier == null || supplier.getCooperativeName() == null
                ? "—" : supplier.getCooperativeName());
        addMetric(sections, "Village", supplier == null || supplier.getLocation() == null ? "—" : supplier.getLocation());
        addMetric(sections, "Batches supplied", String.valueOf(supplierBatchCount));

        if (batch.isVerified()) {
            addSectionTitle(sections, "Sustainability");
            addCheck(sections, "No child labor", batch.isNoChildLabor());
            addCheck(sections, "Firebreak protection", batch.isFirebreakProtection());
            addCheck(sections, "Sustainable harvesting", batch.isSustainableHarvesting());
        }
        if (batch.getNotes() != null && !batch.getNotes().trim().isEmpty()) {
            addSectionTitle(sections, "Notes");
            TextView notes = text(batch.getNotes(), 15, 0xFF3D3A35, R.font.manrope_semibold);
            notes.setPadding(0, dp(3), 0, dp(12));
            sections.addView(notes);
        }
        body.addView(sections);
        body.addView(createTabletActions());
    }

    private void inlineTabletHeader() {
        ViewGroup heroContent = (ViewGroup) tvBatchNumber.getParent();
        ViewGroup oldMetaParent = (ViewGroup) tvBatchDate.getParent();
        ViewGroup badgeRow = (ViewGroup) tvGrade.getParent();
        oldMetaParent.removeView(tvBatchDate);
        badgeRow.removeView(tvGrade);
        badgeRow.removeView(tvBatchType);
        heroContent.removeView(badgeRow);

        LinearLayout meta = new LinearLayout(requireContext());
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-1, -2);
        metaParams.topMargin = dp(4);
        tvBatchDate.setMaxLines(1);
        tvBatchDate.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvBatchDate.setMaxWidth(dp(420));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-2, -2);
        meta.addView(tvBatchDate, dateParams);
        if (tvGrade instanceof Chip) {
            Chip gradeChip = (Chip) tvGrade;
            gradeChip.setEnsureMinTouchTargetSize(false);
            gradeChip.setChipMinHeight(dp(24));
            gradeChip.setTextSize(12);
        }
        LinearLayout.LayoutParams gradeParams = new LinearLayout.LayoutParams(-2, -2);
        gradeParams.leftMargin = dp(8);
        meta.addView(tvGrade, gradeParams);
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(-2, -2);
        typeParams.leftMargin = dp(8);
        meta.addView(tvBatchType, typeParams);
        heroContent.addView(meta, metaParams);
        heroContent.setPadding(dp(20), dp(14), dp(20), dp(14));
        ViewGroup.LayoutParams heroParams = ((View) heroContent.getParent()).getLayoutParams();
        if (heroParams instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) heroParams).bottomMargin = 0;
            ((View) heroContent.getParent()).setLayoutParams(heroParams);
        }
        heroContent.setPadding(dp(20), dp(14), dp(20), dp(4));
    }

    private View createSummaryCards() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(8));
        addSummaryCard(row, "Moisture", String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()), true);
        addSummaryCard(row, "KOR", String.format(Locale.getDefault(), "%.0f g/kg", batch.getKorGPerKg()), false);
        double score = batch.isKernel() ? batch.getKernelQualityScore() : batch.getRcnQualityScore();
        addSummaryCard(row, "Quality score", String.format(Locale.getDefault(), "%.0f / 100", score), false);
        return row;
    }

    private void addSummaryCard(LinearLayout row, String label, String value, boolean primary) {
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(112), 1f);
        params.setMargins(0, 0, dp(8), 0);
        card.setLayoutParams(params);
        card.setRadius(dp(18)); card.setCardElevation(0);
        card.setCardBackgroundColor(primary ? 0xFF0E6B3C : 0xFFF5F0E6);
        LinearLayout content = new LinearLayout(requireContext()); content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(android.view.Gravity.CENTER_VERTICAL); content.setPadding(dp(12), 0, dp(8), 0);
        content.addView(text(label, 12, primary ? 0xFFC7D9CD : 0xFF6E695F, R.font.manrope_bold));
        TextView valueView = text(value, 22, primary ? 0xFFFFFFFF : 0xFF1C1B1A, R.font.manrope_extrabold);
        int suffixStart = value.indexOf('%');
        if (suffixStart < 0) suffixStart = value.indexOf(" g/kg");
        if (suffixStart < 0) suffixStart = value.indexOf(" / 100");
        if (suffixStart >= 0) {
            SpannableString formattedValue = new SpannableString(value);
            formattedValue.setSpan(new RelativeSizeSpan(0.68f), suffixStart, value.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            valueView.setText(formattedValue);
        }
        valueView.setMaxLines(1); valueView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2); valueParams.topMargin = dp(4);
        content.addView(valueView, valueParams); card.addView(content); row.addView(card);
    }

    private View createTabletActions() {
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setPadding(0, dp(8), 0, dp(12));

        LinearLayout topRow = new LinearLayout(requireContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        MaterialButton edit = actionButton("Edit", 0xFF0E6B3C, 0xFFFFFFFF, false);
        MaterialButton export = actionButton("Export QR", 0xFFFFFFFF, 0xFF1C1B1A, true);
        MaterialButton trace = actionButton("Generate Traceability", 0xFFFFFFFF, 0xFF1C1B1A, true);
        MaterialButton share = actionButton("Share", 0xFFFFFFFF, 0xFF1C1B1A, true);
        addWeightedAction(topRow, edit, 0.8f);
        addWeightedAction(topRow, export, 1.15f);
        addWeightedAction(topRow, trace, 1.8f);
        addWeightedAction(topRow, share, 0.9f);
        actions.addView(topRow);

        MaterialButton delete = actionButton("Delete", 0xFFF8E4DD, 0xFFB3402A, false);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-1, dp(58));
        deleteParams.topMargin = dp(8);
        actions.addView(delete, deleteParams);

        boolean canEdit = session.canEditData();
        edit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        delete.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        boolean canSell = batch != null && batch.isSaleable();
        export.setEnabled(canSell); export.setAlpha(canSell ? 1f : 0.4f);
        share.setEnabled(canSell); share.setAlpha(canSell ? 1f : 0.4f);
        trace.setVisibility(btnTrace.getVisibility());
        if (btnTrace.getText().toString().equalsIgnoreCase("View trace")) {
            trace.setText("Generate Traceability");
            trace.setOnClickListener(v -> openTraceability());
        } else {
            trace.setOnClickListener(v -> generateTraceability());
        }
        edit.setOnClickListener(v -> showEditDialog());
        export.setOnClickListener(v -> exportQRCode());
        share.setOnClickListener(v -> shareTraceability());
        delete.setOnClickListener(v -> confirmDelete());
        return actions;
    }

    private MaterialButton actionButton(String label, int fill, int textColor, boolean outlined) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(label);
        button.setTextColor(textColor);
        button.setTextSize(14);
        button.setTypeface(getResources().getFont(R.font.manrope_extrabold));
        button.setMinHeight(dp(58));
        button.setMinimumHeight(dp(58));
        button.setInsetTop(0); button.setInsetBottom(0);
        button.setCornerRadius(dp(999));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fill));
        if (outlined) {
            button.setStrokeWidth(dp(1));
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE9E1CF));
        }
        button.setPadding(dp(12), 0, dp(12), 0);
        return button;
    }

    private void addWeightedAction(LinearLayout row, MaterialButton button, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), weight);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(button, params);
    }

    private void exportQRCode() {
        if (batch == null) return;
        Bitmap bitmap = QRCodeGenerator.generateQRCode("https://www.afrinutsexport.com/trace/" + batch.getBatchNumber());
        if (bitmap == null) { Toast.makeText(requireContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show(); return; }
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(20), dp(24), dp(20));
        TextView title = text("Export QR Code", 20, 0xFF1C1B1A, R.font.manrope_extrabold);
        content.addView(title);
        TextView subtitle = text("Choose how to export this batch’s QR code.", 14, 0xFF59544C, R.font.manrope_semibold);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(6);
        subtitleParams.bottomMargin = dp(16);
        content.addView(subtitle, subtitleParams);

        MaterialButton save = actionButton("Save to Gallery", 0xFF0E6B3C, 0xFFFFFFFF, false);
        MaterialButton shareQr = actionButton("Share QR Code", 0xFFF3EAD8, 0xFF1C1B1A, false);
        MaterialButton cancel = actionButton("Cancel", 0xFFFFFFFF, 0xFF59544C, true);
        content.addView(save, new LinearLayout.LayoutParams(-1, dp(58)));
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(-1, dp(58));
        shareParams.topMargin = dp(8);
        content.addView(shareQr, shareParams);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(58));
        cancelParams.topMargin = dp(8);
        content.addView(cancel, cancelParams);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(content).create();
        save.setOnClickListener(v -> {
            dialog.dismiss();
            String path = QRCodeGenerator.saveQRCodeToGallery(requireContext(), bitmap, batch.getBatchNumber());
            Toast.makeText(requireContext(), path == null ? "Failed to save QR code" : "QR Code saved to: " + path, Toast.LENGTH_LONG).show();
        });
        shareQr.setOnClickListener(v -> {
            dialog.dismiss();
            QRCodeGenerator.shareQRCode(requireContext(), bitmap, batch.getBatchNumber());
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnShowListener(ignored -> {
            android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
            background.setColor(android.graphics.Color.WHITE);
            background.setCornerRadius(dp(24));
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(background);
        });
        dialog.show();
    }

    private void shareTraceability() {
        if (batch == null) return;
        String text = "AfriNuts Traceability\nBatch: " + batch.getBatchNumber()
                + "\nGrade: " + batch.getQualityGrade().getDisplayName()
                + "\nWeight: " + String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg())
                + "\nQuality Score: " + (int) batch.getQualityScore() + "%"
                + "\nTrace: https://www.afrinutsexport.com/trace/" + batch.getBatchNumber();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain"); intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share Traceability"));
    }

    private void confirmDelete() {
        if (batch == null || !session.canEditData()) return;
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Batch")
                .setMessage("Are you sure you want to delete batch " + batch.getBatchNumber() + "?")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> executor.execute(() -> {
                    repository.deleteBatch(batch, new QualityBatchRepository.RepositoryCallback<Boolean>() {
                        @Override public void onSuccess(Boolean result) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Batch deleted", Toast.LENGTH_SHORT).show();
                                getParentFragmentManager().beginTransaction().remove(QualityBatchDetailFragment.this).commit();
                            });
                        }
                        @Override public void onError(String error) {
                            if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show());
                        }
                    });
                })).show();
    }

    private void addSectionTitle(LinearLayout parent, String title) {
        TextView heading = text(title, 17, 0xFF1C1B1A, R.font.manrope_extrabold);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(16); params.bottomMargin = dp(2); parent.addView(heading, params);
    }

    private void addMetric(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL); row.setPadding(dp(2), dp(10), dp(2), dp(10));
        TextView key = text(label, 14, 0xFF59544C, R.font.manrope_semibold);
        TextView val = text(value, 14, 0xFF1C1B1A, R.font.manrope_bold); val.setGravity(android.view.Gravity.END);
        row.addView(key, new LinearLayout.LayoutParams(0, -2, 1f)); row.addView(val, new LinearLayout.LayoutParams(0, -2, 1.2f));
        parent.addView(row); parent.addView(divider());
    }

    private String testResultText(TextView value) {
        String result = value.getText() == null ? "—" : value.getText().toString();
        if (result.equals("—") || result.equalsIgnoreCase("Below limit")) return result;
        return result + " ppb";
    }

    private void addCheck(LinearLayout parent, String label, boolean checked) {
        LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL); row.setPadding(0, dp(8), 0, dp(8));
        TextView icon = text(checked ? "✓" : "×", 16, checked ? 0xFF0A5530 : 0xFFB3402A, R.font.manrope_extrabold);
        icon.setGravity(android.view.Gravity.CENTER);
        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setShape(GradientDrawable.OVAL);
        iconBackground.setColor(checked ? 0xFFE3F0E7 : 0xFFF8E4DD);
        icon.setBackground(iconBackground);
        icon.setWidth(dp(28)); icon.setHeight(dp(28)); row.addView(icon);
        TextView labelView = text(label, 15, 0xFF1C1B1A, R.font.manrope_bold);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.leftMargin = dp(10); row.addView(labelView, p);
        parent.addView(row); parent.addView(divider());
    }

    private View divider() { View line = new View(requireContext()); line.setBackgroundColor(0xFFE9E1CF); line.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); return line; }
    private TextView text(String value, int size, int color, int font) {
        TextView v = new TextView(requireContext()); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setTypeface(getResources().getFont(font)); return v;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void setYesNo(TextView view, boolean yes) { view.setText(yes ? "Yes" : "No"); view.setTextColor(yes ? 0xFF0E6B3C : 0xFFB3402A); }

    private void showEditDialog() {
        if (batch == null || !session.canEditData()) return;
        if (batch.isRcn() || batch.getKorPercent() > 0) {
            RcnEditDialog d = RcnEditDialog.newInstance(farmId, batchId);
            d.setOnUpdatedListener(this::loadBatch);
            d.show(getParentFragmentManager(), "RcnEditDialog");
        } else {
            QualityBatchDialog d = QualityBatchDialog.newEditInstance(farmId, batchId);
            d.setOnBatchAddedListener(this::loadBatch);
            d.show(getParentFragmentManager(), "EditQualityBatchDialog");
        }
    }

    private void viewSupplier() {
        if (supplier == null || !isAdded()) return;
        Intent intent = new Intent(requireContext(), com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.class);
        intent.putExtra(com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.EXTRA_SUPPLIER_ID, supplier.getId());
        startActivity(intent);
    }

    private void openTraceability() {
        executor.execute(() -> {
            BatchTraceabilityEntity trace = database.batchTraceabilityDao().getByQualityBatchId(batchId);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (trace == null || !isAdded()) { Toast.makeText(requireContext(), "Trace record not available", Toast.LENGTH_SHORT).show(); return; }
                Intent intent = new Intent(requireContext(), com.afrinuts.farmos.ui.traceability.TraceabilityDetailActivity.class);
                intent.putExtra(com.afrinuts.farmos.ui.traceability.TraceabilityDetailActivity.EXTRA_TRACEABILITY_ID, trace.getId());
                startActivity(intent);
            });
        });
    }

    private void generateTraceability() {
        if (batch == null || !batch.isSaleable()) {
            if (isAdded()) Toast.makeText(requireContext(), batch == null ? "No batch to generate traceability for" : batch.getSaleBlockReason(), Toast.LENGTH_LONG).show();
            return;
        }
        executor.execute(() -> {
            try {
                if (database.batchTraceabilityDao().getByQualityBatchId(batch.getId()) != null) { loadBatch(); return; }
                BatchTraceabilityEntity trace = new BatchTraceabilityEntity(batch.getFarmId(), batch.getBatchNumber(), batch.getProcessingDate(), batch.getWeightKg(), batch.getQualityGrade().getDisplayName(), 0);
                trace.setQualityBatchId(batch.getId()); trace.setSupplierId(batch.getSupplierId());
                trace.setSupplierName(supplier == null ? "Supplier " + batch.getSupplierId() : supplier.getName());
                trace.setQualityGrade(batch.getQualityGrade().getDisplayName());
                trace.setTraceabilityUrl(trace.generateTraceabilityUrl(batch.getBatchNumber()));
                long id = database.batchTraceabilityDao().insert(trace);
                SyncManager sync = SyncManager.getInstance(requireContext());
                FirestoreSyncManager firestore = FirestoreSyncManager.getInstance(requireContext());
                if (!firestore.isRemoteUpdate()) { sync.queueForSync("batch_traceability", id, SyncEntity.SyncAction.CREATE); sync.triggerAutoSync(); }
                loadBatch();
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Unable to generate traceability", Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.shutdown();
        if (supplierRepository != null) supplierRepository.shutdown();
        executor.shutdownNow();
    }
}
