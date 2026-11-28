package com.afrinuts.farmos.ui.quality;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.QRCodeGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QualityBatchDetailActivity extends BaseActivity {

    public static final String EXTRA_BATCH_ID = "batch_id";
    private static final String TAG = "QualityBatchDetail";

    private long batchId;
    private QualityBatchEntity batch;
    private SupplierEntity supplier;
    private QualityBatchRepository repository;
    private SupplierRepository supplierRepository;
    private AppDatabase database;
    private SyncManager syncManager;
    private FirestoreSyncManager firestoreSyncManager;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements - Header
    private TextView tvBatchNumber;
    private TextView tvBatchDate;
    private TextView tvGrade;
    private TextView tvQualityScore;
    private CircularProgressIndicator qualityProgress;

    // UI Elements - Source Info
    private TextView tvSourceType;
    private TextView tvSupplierName;
    private TextView tvBatchType;

    // UI Elements - Metrics
    private TextView tvMoistureValue;
    private TextView tvDefectiveValue;
    private TextView tvNutCountValue;
    private TextView tvKernelValue;
    private TextView tvWeightValue;
    private TextView tvSupplierLocation;
    private TextView tvSupplierBatchCount;
    private TextView tvReceivedAt;
    private int supplierBatchCount;

    // UI Elements - Test Results
    private TextView tvAflatoxinValue;
    private TextView tvPesticideValue;
    private TextView tvHeavyMetalsValue;
    private CardView cardTestResults;

    // ✅ Sustainability UI Elements
    private CardView layoutSustainability;
    private TextView tvSustainabilityScore;
    private TextView tvNoChildLabor;
    private TextView tvFarmOsMonitored;
    private TextView tvFirebreakProtection;
    private TextView tvSustainableHarvesting;
    private TextView tvVerifiedDate;

    // UI Elements - Notes
    private TextView tvNotesValue;
    private CardView cardNotes;

    // UI Elements - Buttons
    private MaterialButton btnEdit;
    private MaterialButton btnDelete;
    private MaterialButton btnGenerateTraceability;
    private MaterialButton btnExportQR;
    private MaterialButton btnShare;
    private CardView cardKernelMetrics;
    private CardView cardRcnMetrics;
    private TextView tvKorValue;
    private TextView tvRcnMoistureValue;
    private TextView tvRcnNutCountValue;
    private TextView tvMouldyRate;
    private TextView tvImmatureRate;
    private TextView tvInsectDamagedRate;
    private TextView tvShrivelledRate;
    private TextView tvTotalDefective;
    private TextView tvForeignMatter;
    private TextView tvDryingMethod;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality_batch_detail);

        batchId = getIntent().getLongExtra(EXTRA_BATCH_ID, -1);
        if (batchId == -1) {
            Toast.makeText(this, "Batch not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        syncManager = SyncManager.getInstance(this);
        firestoreSyncManager = FirestoreSyncManager.getInstance(this);
        initViews();
        setupToolbar();
        loadBatchData();
    }

    private void initViews() {

        // Header
        tvBatchNumber = findViewById(R.id.tvBatchNumber);
        tvBatchDate = findViewById(R.id.tvBatchDate);
        tvGrade = findViewById(R.id.tvGrade);
        tvQualityScore = findViewById(R.id.tvQualityScore);
        qualityProgress = findViewById(R.id.qualityProgress);

        // Source Info
        tvSourceType = findViewById(R.id.tvSourceType);
        tvSupplierLocation = findViewById(R.id.tvSupplierLocation);
        tvSupplierBatchCount = findViewById(R.id.tvSupplierBatchCount);
        tvReceivedAt = findViewById(R.id.tvReceivedAt);
        tvSupplierName = findViewById(R.id.tvSupplierName);
        tvBatchType = findViewById(R.id.tvBatchType);

        // Metrics
        tvMoistureValue = findViewById(R.id.tvMoistureValue);
        tvDefectiveValue = findViewById(R.id.tvDefectiveValue);
        tvNutCountValue = findViewById(R.id.tvNutCountValue);
        tvKernelValue = findViewById(R.id.tvKernelValue);
        tvWeightValue = findViewById(R.id.tvWeightValue);

        // Test Results
        tvAflatoxinValue = findViewById(R.id.tvAflatoxinValue);
        tvPesticideValue = findViewById(R.id.tvPesticideValue);
        tvHeavyMetalsValue = findViewById(R.id.tvHeavyMetalsValue);
        cardTestResults = findViewById(R.id.cardTestResults);

        // ✅ Sustainability UI Elements
        layoutSustainability = findViewById(R.id.layoutSustainability);
        tvSustainabilityScore = findViewById(R.id.tvSustainabilityScore);
        tvNoChildLabor = findViewById(R.id.tvNoChildLabor);
        tvFarmOsMonitored = findViewById(R.id.tvFarmOsMonitored);
        tvFirebreakProtection = findViewById(R.id.tvFirebreakProtection);
        tvSustainableHarvesting = findViewById(R.id.tvSustainableHarvesting);
        tvVerifiedDate = findViewById(R.id.tvVerifiedDate);

        // Notes
        tvNotesValue = findViewById(R.id.tvNotesValue);
        cardNotes = findViewById(R.id.cardNotes);

        cardKernelMetrics = findViewById(R.id.cardKernelMetrics);
        cardRcnMetrics = findViewById(R.id.cardRcnMetrics);
        tvKorValue = findViewById(R.id.tvKorValue);
        tvRcnMoistureValue = findViewById(R.id.tvRcnMoistureValue);
        tvRcnNutCountValue = findViewById(R.id.tvRcnNutCountValue);
        tvMouldyRate = findViewById(R.id.tvMouldyRate);
        tvImmatureRate = findViewById(R.id.tvImmatureRate);
        tvInsectDamagedRate = findViewById(R.id.tvInsectDamagedRate);
        tvShrivelledRate = findViewById(R.id.tvShrivelledRate);
        tvTotalDefective = findViewById(R.id.tvTotalDefective);
        tvForeignMatter = findViewById(R.id.tvForeignMatter);
        tvDryingMethod = findViewById(R.id.tvDryingMethod);


        // Buttons
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnGenerateTraceability = findViewById(R.id.btnGenerateTraceability);
        btnExportQR = findViewById(R.id.btnExportQR);
        btnShare = findViewById(R.id.btnShare);

        findViewById(R.id.tvSupplierSeeAll).setOnClickListener(v -> viewSupplier());

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());

        if (btnGenerateTraceability != null) {
            btnGenerateTraceability.setVisibility(View.GONE);
        }

        if (btnExportQR != null) {
            btnExportQR.setOnClickListener(v -> exportQRCode());
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> shareTraceability());
        }
    }

    private void setupToolbar() {
        com.afrinuts.farmos.ui.farm.FarmDetailChrome.apply(this,
                findViewById(R.id.farmDetailTitle), findViewById(R.id.farmDetailBack),
                findViewById(R.id.farmDetailMore), "Batch details", R.id.cardKernelMetrics);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadBatchData() {
        executor.execute(() -> {
            // Get farm ID
            farmId = database.farmDao().getFirstFarm().getId();
            repository = new QualityBatchRepository(this, farmId);
            supplierRepository = new SupplierRepository(this, farmId);

            // Load batch and resolve trace existence off the main thread.
            batch = repository.getBatchByIdSync(batchId);
            boolean hasTraceability = batch != null
                    && database.batchTraceabilityDao().getByQualityBatchId(batch.getId()) != null;

            if (batch != null && batch.getSupplierId() != null) {
                supplier = supplierRepository.getSupplierByIdSync(batch.getSupplierId());
                supplierBatchCount = database.qualityBatchDao().getBatchCountBySupplierId(batch.getSupplierId());
            }

            runOnUiThread(() -> {
                if (batch != null) {
                    displayBatchData(hasTraceability);
                } else {
                    Toast.makeText(this, "Batch not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void displayBatchData(boolean hasTraceability) {
        // Header
        tvBatchNumber.setText(batch.getBatchNumber() == null ? "—" : batch.getBatchNumber());
        long displayDate = batch.getRelevantDate();
        String dateText = displayDate > 0 ? dateFormat.format(displayDate) : "—";
        String supplierLabel = supplier == null ? "" : supplier.getName();
        String cooperativeLabel = supplier != null && supplier.getCooperativeName() != null
                && !supplier.getCooperativeName().trim().isEmpty()
                ? supplier.getCooperativeName().trim()
                : (batch.getSourceType() == null ? "" : batch.getSourceType().getDisplayName());
        StringBuilder subtitle = new StringBuilder();
        if (!supplierLabel.isEmpty()) subtitle.append(supplierLabel);
        if (!cooperativeLabel.isEmpty()) {
            if (subtitle.length() > 0) subtitle.append(" · ");
            subtitle.append(cooperativeLabel);
        }
        if (!dateText.equals("—")) {
            if (subtitle.length() > 0) subtitle.append(" · ");
            subtitle.append(dateText);
        }
        tvBatchDate.setText(subtitle.length() == 0 ? dateText : subtitle.toString());
        tvGrade.setText(batch.getQualityGrade().getDisplayName());
        tvBatchType.setText(batch.isRcn() ? "RCN batch" : "Kernel batch");
        TextView deliveryBatch = findViewById(R.id.tvDeliveryBatch);
        TextView deliveryDate = findViewById(R.id.tvDeliveryDate);
        TextView harvestDateView = findViewById(R.id.tvHarvestDate);
        TextView qualityScoreLab = findViewById(R.id.tvQualityScoreLab);
        if (deliveryBatch != null) deliveryBatch.setText(batch.getBatchNumber() == null ? "—" : batch.getBatchNumber());
        if (deliveryDate != null) deliveryDate.setText(dateText);
        if (harvestDateView != null) harvestDateView.setText(batch.getHarvestDate() > 0
                ? dateFormat.format(new java.util.Date(batch.getHarvestDate())) : "—");
        if (qualityScoreLab != null) qualityScoreLab.setText("— / 100");

        int gradeColor;
        int gradeIcon;
        switch (batch.getQualityGrade()) {
            case PREMIUM:
                gradeColor = getColor(R.color.accent_ink);
                gradeIcon = R.drawable.ic_star;
                break;
            case STANDARD:
                gradeColor = getColor(R.color.primary);
                gradeIcon = R.drawable.ic_check_circle;
                break;
            case REJECT:
            default:
                gradeColor = getColor(android.R.color.holo_red_dark);
                gradeIcon = R.drawable.ic_cancel;
                break;
        }
        tvGrade.setTextColor(gradeColor);
        int gradeFill = batch.getQualityGrade() == QualityBatchEntity.QualityGrade.PREMIUM
                ? 0xFFE3F0E7 : batch.getQualityGrade() == QualityBatchEntity.QualityGrade.STANDARD
                ? 0xFFF3EAD8 : 0xFFF8E4DD;
        if (tvGrade instanceof Chip) {
            ((Chip) tvGrade).setChipBackgroundColor(android.content.res.ColorStateList.valueOf(gradeFill));
            ((Chip) tvGrade).setCheckedIconVisible(false);
        }
        if (qualityProgress != null) qualityProgress.setIndicatorColor(gradeColor);

        double score = batch.isKernel()
                ? batch.getKernelQualityScore()
                : batch.getRcnQualityScore();
        int scoreInt = (int) Math.round(score);
        tvQualityScore.setText(scoreInt + "%");
        if (qualityProgress != null) {
            qualityProgress.setProgress(scoreInt);
            qualityProgress.setMax(100);
        }
        TextView qualityScoreLabValue = findViewById(R.id.tvQualityScoreLab);
        if (qualityScoreLabValue != null) qualityScoreLabValue.setText(scoreInt + " / 100");

        // Source
        String cooperativeName = supplier != null && supplier.getCooperativeName() != null
                && !supplier.getCooperativeName().trim().isEmpty()
                ? supplier.getCooperativeName().trim()
                : (batch.getSourceType() == null ? "—" : batch.getSourceType().getDisplayName());
        tvSourceType.setText(cooperativeName);
        tvSupplierName.setText(supplier != null && supplier.getName() != null
                ? supplier.getName() : (batch.getSourceType() == QualityBatchEntity.SourceType.OWN_FARM
                ? "AfriNuts farm" : "—"));
        String supplierLocation = supplier != null && supplier.getLocation() != null
                && !supplier.getLocation().trim().isEmpty() ? supplier.getLocation().trim() : "—";
        if (tvSupplierLocation != null) tvSupplierLocation.setText(supplierLocation);
        if (tvSupplierBatchCount != null) tvSupplierBatchCount.setText(String.valueOf(supplierBatchCount));
        if (tvReceivedAt != null) tvReceivedAt.setText(supplierLocation);
        TextView deliveryWeight = findViewById(R.id.tvWeightValue);
        if (deliveryWeight != null) deliveryWeight.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));

        // View existing traceability for all roles; generation is editor-only.
        if (btnGenerateTraceability != null) {
            if (hasTraceability) {
                btnGenerateTraceability.setVisibility(View.VISIBLE);
                btnGenerateTraceability.setText("View trace");
                btnGenerateTraceability.setOnClickListener(v -> openTraceability());
            } else if (sessionManager.canEditData()) {
                btnGenerateTraceability.setVisibility(View.VISIBLE);
                btnGenerateTraceability.setText("Generate traceability");
                btnGenerateTraceability.setOnClickListener(v -> generateTraceability());
            } else {
                btnGenerateTraceability.setVisibility(View.GONE);
                btnGenerateTraceability.setOnClickListener(null);
            }
        }

        if (btnExportQR != null) {
            boolean canSell = batch.isSaleable();
            btnExportQR.setEnabled(canSell);
            btnExportQR.setAlpha(canSell ? 1f : 0.4f);
        }

        if (btnShare != null) {
            boolean canSell = batch.isSaleable();
            btnShare.setEnabled(canSell);
            btnShare.setAlpha(canSell ? 1f : 0.4f);
        }

        String bn = batch.getBatchNumber();
        if (bn.contains("-ORG-")) tvBatchType.setText("Organic");
        else if (bn.contains("-CONV-")) tvBatchType.setText("Conventional");
        else if (bn.contains("-RCN-")) tvBatchType.setText("RCN");
        else tvBatchType.setText("—");

        tvWeightValue.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));



        // ✅ Three-state branch
        boolean isKernel = batch.isKernel();
        boolean isRcn = batch.isRcn();
        boolean hasRcnData = batch.getKorPercent() > 0;

        cardKernelMetrics.setVisibility(View.VISIBLE);
        cardRcnMetrics.setVisibility(View.GONE);
        if (isKernel) {
            displayKernelMetrics();
        } else if (isRcn || hasRcnData) {
            displayRcnMetrics();
            tvMoistureValue.setText(String.format(Locale.getDefault(), "%.1f", batch.getMoistureContent()));
            tvDefectiveValue.setText(String.format(Locale.getDefault(), "%.1f", batch.getDefectiveRate()));
            tvKernelValue.setText(batch.getKorGPerKg() > 0
                    ? String.format(Locale.getDefault(), "%.0f", batch.getKorGPerKg()) : "—");
            TextView kernelUnit = findViewById(R.id.tvKernelValueUnit);
            if (kernelUnit != null) kernelUnit.setText("g/kg");
        } else {
            showEmptyRcnMetrics();
        }

        displaySustainabilityData();

        if (batch.getNotes() != null && !batch.getNotes().isEmpty()) {
            cardNotes.setVisibility(View.VISIBLE);
            tvNotesValue.setText(batch.getNotes());
        } else {
            cardNotes.setVisibility(View.GONE);
        }

        if (!sessionManager.canEditData()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            View headerEdit = findViewById(R.id.farmDetailEdit);
            if (headerEdit != null) headerEdit.setVisibility(View.GONE);
            if (btnExportQR != null) btnExportQR.setVisibility(View.GONE);
            if (btnShare != null) btnShare.setVisibility(View.GONE);
        }

        // Guard against mixed records
        if (hasRcnData && batch.getKernelQualityScore() > 0) {
            Log.w(TAG, "Mixed record: batch " + batch.getBatchNumber()
                    + " has both RCN and kernel quality scores");
        }
    }

    private void showEmptyRcnMetrics() {
        tvKorValue.setText("—");
        tvRcnMoistureValue.setText("—");
        tvRcnNutCountValue.setText("—");
        tvMouldyRate.setText("—");
        tvImmatureRate.setText("—");
        tvInsectDamagedRate.setText("—");
        tvShrivelledRate.setText("—");
        tvTotalDefective.setText("—");
        tvForeignMatter.setText("—");
        tvDryingMethod.setText("—");
        cardTestResults.setVisibility(View.GONE);
    }

    private void displayKernelMetrics() {
        tvMoistureValue.setText(String.format(Locale.getDefault(), "%.1f", batch.getMoistureContent()));
        tvDefectiveValue.setText(String.format(Locale.getDefault(), "%.1f", batch.getDefectiveRate()));
        tvNutCountValue.setText(String.valueOf(batch.getNutCountPerKg()));
        tvKernelValue.setText(String.format(Locale.getDefault(), "%.0f", batch.getKernelRatio()));
        TextView moistureUnit = findViewById(R.id.tvMoistureValueUnit);
        TextView defectiveUnit = findViewById(R.id.tvDefectiveValueUnit);
        TextView kernelUnit = findViewById(R.id.tvKernelValueUnit);
        if (moistureUnit != null) moistureUnit.setText("%");
        if (defectiveUnit != null) defectiveUnit.setText("%");
        if (kernelUnit != null) kernelUnit.setText("%");
        // Weight is already set above

        // Test results: prefer the promoted ppb fields, fall back to legacy JSON
        if (batch.getAflatoxinPpb() > 0
                || batch.getHeavyMetalsPpb() > 0
                || batch.getPesticideResiduePpb() > 0) {
            cardTestResults.setVisibility(View.VISIBLE);
            tvAflatoxinValue.setText(String.format(Locale.getDefault(), "%.2f", batch.getAflatoxinPpb()));
            tvPesticideValue.setText(String.format(Locale.getDefault(), "%.2f", batch.getPesticideResiduePpb()));
            tvHeavyMetalsValue.setText(String.format(Locale.getDefault(), "%.2f", batch.getHeavyMetalsPpb()));
        } else {
            QualityBatchEntity.TestResults legacy =
                    QualityBatchEntity.TestResults.fromJson(batch.getTestResults());
            if (legacy.hasResults()) {
                cardTestResults.setVisibility(View.VISIBLE);
                tvAflatoxinValue.setText(String.format(Locale.getDefault(), "%.2f", legacy.getAflatoxin()));
                tvPesticideValue.setText(String.format(Locale.getDefault(), "%.2f", legacy.getPesticideResidue()));
                tvHeavyMetalsValue.setText(String.format(Locale.getDefault(), "%.2f", legacy.getHeavyMetals()));
            } else {
                tvAflatoxinValue.setText("—");
                tvPesticideValue.setText("—");
                tvHeavyMetalsValue.setText("—");
                cardTestResults.setVisibility(View.VISIBLE);
            }
        }
    }

    private void displayRcnMetrics() {
        tvKorValue.setText(String.format(Locale.getDefault(), "%.0f g/kg", batch.getKorGPerKg()));
        tvRcnMoistureValue.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()));
        tvRcnNutCountValue.setText(batch.getNutCountPerKg() + "/kg");

        tvMouldyRate.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getMouldyRate()));
        tvImmatureRate.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getImmatureRate()));
        tvInsectDamagedRate.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getInsectDamagedRate()));
        tvShrivelledRate.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getShrivelledRate()));
        tvTotalDefective.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getDefectiveRate()));
        tvForeignMatter.setText(String.format(Locale.getDefault(), "%.2f%%", batch.getForeignMatter()));
        tvDryingMethod.setText(batch.getDryingMethod() != null ? batch.getDryingMethod() : "—");

        TextView tvKorAudit = findViewById(R.id.tvKorAudit);
        View layoutKorAudit = findViewById(R.id.layoutKorAudit);

        if (batch.getKorSampleGrams() > 0 && batch.getKorKernelGrams() > 0) {
            layoutKorAudit.setVisibility(View.VISIBLE);
            tvKorAudit.setText(String.format(Locale.getDefault(),
                    "%.0f g → %.0f g", batch.getKorSampleGrams(), batch.getKorKernelGrams()));
        } else {
            layoutKorAudit.setVisibility(View.GONE);
        }

        // Keep the standard Lab results section visible; unavailable tests show an em dash.
        cardTestResults.setVisibility(View.VISIBLE);
        tvAflatoxinValue.setText("—");
        tvPesticideValue.setText("—");
        tvHeavyMetalsValue.setText("—");
    }

    private void displaySustainabilityData() {
        if (batch == null) return;

        if (batch.isVerified()) {
            layoutSustainability.setVisibility(View.VISIBLE);

            int score = batch.getSustainabilityScore();
            tvSustainabilityScore.setText(score + "%");

            tvVerifiedDate.setText(batch.getFormattedVerifiedDate());

            tvNoChildLabor.setText(batch.isNoChildLabor() ? "Yes" : "No");
            tvFarmOsMonitored.setText(batch.isFarmOsMonitored() ? "Yes" : "No");
            tvFirebreakProtection.setText(batch.isFirebreakProtection() ? "Yes" : "No");
            tvSustainableHarvesting.setText(batch.isSustainableHarvesting() ? "Yes" : "No");

            int colorYes = getColor(android.R.color.holo_green_light);
            int colorNo = getColor(android.R.color.holo_red_light);

            tvNoChildLabor.setTextColor(batch.isNoChildLabor() ? colorYes : colorNo);
            tvFarmOsMonitored.setTextColor(batch.isFarmOsMonitored() ? colorYes : colorNo);
            tvFirebreakProtection.setTextColor(batch.isFirebreakProtection() ? colorYes : colorNo);
            tvSustainableHarvesting.setTextColor(batch.isSustainableHarvesting() ? colorYes : colorNo);

        } else {
            layoutSustainability.setVisibility(View.GONE);
        }
    }

    public void updateTraceabilityWithFullData(long traceabilityId) {
        executor.execute(() -> {
            try {
                BatchTraceabilityEntity traceability = database.batchTraceabilityDao()
                        .getById(traceabilityId);
                if (traceability == null) {
                    Log.e(TAG, "Traceability not found: " + traceabilityId);
                    return;
                }

                // Get quality batch
                QualityBatchEntity batch = database.qualityBatchDao()
                        .getBatchById(traceability.getQualityBatchId());
                if (batch == null) {
                    Log.e(TAG, "Quality batch not found: " + traceability.getQualityBatchId());
                    return;
                }

                // Get supplier
                String supplierName = null;
                if (batch.getSupplierId() != null) {
                    SupplierEntity supplier = database.supplierDao()
                            .getSupplierById(batch.getSupplierId());
                    if (supplier != null) {
                        supplierName = supplier.getName();
                    }
                }

                // Get block
                String blockName = null;
                Long blockId = batch.getBlockId();
                if (blockId != null) {
                    BlockEntity block = database.blockDao().getBlockById(blockId);
                    if (block != null) {
                        blockName = block.getBlockName();
                    }
                }

                // Get revenue
                double pricePerKg = 0;
                double totalAmount = 0;
                String buyer = null;
                Long harvestCycleId = null;
                try {
                    List<RevenueEntity> revenues = database.revenueDao()
                            .getRevenuesByBatchId(batch.getId());
                    if (revenues != null && !revenues.isEmpty()) {
                        RevenueEntity revenue = revenues.get(0);
                        pricePerKg = revenue.getPricePerKg();
                        totalAmount = revenue.getTotalAmount();
                        buyer = revenue.getBuyer();
                        harvestCycleId = revenue.getId();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not get revenue data", e);
                }

                // Update traceability with all data
                traceability.setSupplierName(supplierName != null ? supplierName : "Unknown Supplier");
                traceability.setRevenueId(harvestCycleId);
                traceability.setBuyer(buyer);
                traceability.setPricePerKg(pricePerKg);
                traceability.setTotalAmount(totalAmount);
                traceability.setGrade(batch.getQualityGrade().getDisplayName());
                traceability.setUpdatedAt(System.currentTimeMillis());

                database.batchTraceabilityDao().update(traceability);
                Log.d(TAG, "Updated traceability with full data: " + traceability.getBatchNumber());

                // Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("batch_traceability", traceability.getId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error updating traceability with full data", e);
            }
        });
    }

    // ✅ Generate Traceability
    private void openTraceability() {
        if (batch == null) return;
        executor.execute(() -> {
            BatchTraceabilityEntity trace = database.batchTraceabilityDao().getByQualityBatchId(batch.getId());
            runOnUiThread(() -> {
                if (trace == null) {
                    Toast.makeText(this, "Trace record not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, com.afrinuts.farmos.ui.traceability.TraceabilityDetailActivity.class);
                intent.putExtra(com.afrinuts.farmos.ui.traceability.TraceabilityDetailActivity.EXTRA_TRACEABILITY_ID, trace.getId());
                startActivity(intent);
            });
        });
    }

    private void generateTraceability() {
        if (batch == null) {
            Toast.makeText(this, "No batch to generate traceability for", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ SOP §4.5 + §4.6 guard
        if (!batch.isSaleable()) {
            Toast.makeText(this, batch.getSaleBlockReason(), Toast.LENGTH_LONG).show();
            return;
        }
        if (batch == null) {
            Toast.makeText(this, "No batch to generate traceability for", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if batch is complete
        if (batch.getMoistureContent() <= 0 && batch.getNutCountPerKg() <= 0) {
            Toast.makeText(this, "Batch needs quality metrics first", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Generating traceability...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Check if traceability already exists
                BatchTraceabilityEntity existing = database.batchTraceabilityDao()
                        .getByQualityBatchId(batch.getId());

                if (existing != null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Traceability already exists for this batch", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Create new traceability
                BatchTraceabilityEntity traceability = new BatchTraceabilityEntity(
                        batch.getFarmId(),
                        batch.getBatchNumber(),
                        batch.getProcessingDate(),
                        batch.getWeightKg(),
                        batch.getQualityGrade().getDisplayName(),
                        0
                );

                traceability.setQualityBatchId(batch.getId());
                traceability.setSupplierId(batch.getSupplierId());

                String supplierName = "Supplier " + batch.getSupplierId();
                if (supplier != null) {
                    supplierName = supplier.getName();
                }
                traceability.setSupplierName(supplierName);
                traceability.setQualityGrade(batch.getQualityGrade().getDisplayName());
                traceability.setTraceabilityUrl(
                        traceability.generateTraceabilityUrl(batch.getBatchNumber()));

                // Save to database
                long id = database.batchTraceabilityDao().insert(traceability);
                Log.d(TAG, "Created traceability with ID: " + id);

                // Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("batch_traceability", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Traceability generated!", Toast.LENGTH_SHORT).show();
                    loadBatchData();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error creating traceability", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error generating traceability: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ✅ Export QR Code
    private void exportQRCode() {
        if (batch == null) {
            Toast.makeText(this, "No batch to export QR for", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate QR code
        String traceUrl = "https://www.afrinutsexport.com/trace/" + batch.getBatchNumber();
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(traceUrl);

        if (qrBitmap == null) {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show options: Save or Share
        String[] options = {"Save to Gallery", "Share QR Code"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Export QR Code")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        saveQRCodeToGallery(qrBitmap);
                    } else {
                        shareQRCode(qrBitmap);
                    }
                })
                .show();
    }

    private void saveQRCodeToGallery(Bitmap qrBitmap) {
        String savedPath = QRCodeGenerator.saveQRCodeToGallery(this, qrBitmap, batch.getBatchNumber());
        if (savedPath != null) {
            Toast.makeText(this, "QR Code saved to: " + savedPath, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Failed to save QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQRCode(Bitmap qrBitmap) {
        QRCodeGenerator.shareQRCode(this, qrBitmap, batch.getBatchNumber());
    }

    // ✅ Share Traceability
    private void shareTraceability() {
        if (batch == null) {
            Toast.makeText(this, "No batch to share", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareText = "AfriNuts Traceability\n" +
                "Batch: " + batch.getBatchNumber() + "\n" +
                "Grade: " + batch.getQualityGrade().getDisplayName() + "\n" +
                "Weight: " + String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()) + "\n" +
                "Quality Score: " + (int) batch.getQualityScore() + "%\n" +
                "Trace: https://www.afrinutsexport.com/trace/" + batch.getBatchNumber();

        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Traceability"));
    }

    private void viewSupplier() {
        if (supplier != null) {
            Intent intent = new Intent(this, com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.class);
            intent.putExtra(com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.EXTRA_SUPPLIER_ID, supplier.getId());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Supplier not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDialog() {
        if (batch == null) return;

        if (batch.isRcn() || batch.getKorPercent() > 0) {
            RcnEditDialog d = RcnEditDialog.newInstance(farmId, batchId);
            d.setOnUpdatedListener(() -> {
                loadBatchData();
                Toast.makeText(this, "RCN intake updated!", Toast.LENGTH_SHORT).show();
            });
            d.show(getSupportFragmentManager(), "RcnEditDialog");
        } else {
            QualityBatchDialog d = QualityBatchDialog.newEditInstance(farmId, batchId);
            d.setOnBatchAddedListener(() -> {
                loadBatchData();
                Toast.makeText(this, "Batch updated!", Toast.LENGTH_SHORT).show();
            });
            d.show(getSupportFragmentManager(), "EditQualityBatchDialog");
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Batch")
                .setMessage("Are you sure you want to delete batch " + batch.getBatchNumber() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBatch())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBatch() {
        Toast.makeText(this, "Deleting batch...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            repository.deleteBatch(batch, new QualityBatchRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    runOnUiThread(() -> {
                        Toast.makeText(QualityBatchDetailActivity.this,
                                "Batch deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            Toast.makeText(QualityBatchDetailActivity.this,
                                    "Error: " + error, Toast.LENGTH_LONG).show()
                    );
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
        if (supplierRepository != null) {
            supplierRepository.shutdown();
        }
        executor.shutdown();
    }
}
