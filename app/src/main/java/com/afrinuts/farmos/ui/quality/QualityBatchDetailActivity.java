package com.afrinuts.farmos.ui.quality;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
    private MaterialButton btnViewSupplier;
    private MaterialButton btnGenerateTraceability;
    private MaterialButton btnExportQR;
    private MaterialButton btnShare;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Header
        tvBatchNumber = findViewById(R.id.tvBatchNumber);
        tvBatchDate = findViewById(R.id.tvBatchDate);
        tvGrade = findViewById(R.id.tvGrade);
        tvQualityScore = findViewById(R.id.tvQualityScore);
        qualityProgress = findViewById(R.id.qualityProgress);

        // Source Info
        tvSourceType = findViewById(R.id.tvSourceType);
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

        // Buttons
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnViewSupplier = findViewById(R.id.btnViewSupplier);
        btnGenerateTraceability = findViewById(R.id.btnGenerateTraceability);
        btnExportQR = findViewById(R.id.btnExportQR);
        btnShare = findViewById(R.id.btnShare);

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnViewSupplier.setOnClickListener(v -> viewSupplier());

        if (btnGenerateTraceability != null) {
            btnGenerateTraceability.setOnClickListener(v -> generateTraceability());
        }

        if (btnExportQR != null) {
            btnExportQR.setOnClickListener(v -> exportQRCode());
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> shareTraceability());
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Batch Details");
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

    private void loadBatchData() {
        executor.execute(() -> {
            // Get farm ID
            farmId = database.farmDao().getFirstFarm().getId();
            repository = new QualityBatchRepository(this, farmId);
            supplierRepository = new SupplierRepository(this, farmId);

            // Load batch
            batch = repository.getBatchByIdSync(batchId);

            if (batch != null && batch.getSupplierId() != null) {
                supplier = supplierRepository.getSupplierByIdSync(batch.getSupplierId());
            }

            runOnUiThread(() -> {
                if (batch != null) {
                    displayBatchData();
                } else {
                    Toast.makeText(this, "Batch not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void displayBatchData() {
        // Header
        tvBatchNumber.setText("#" + batch.getBatchNumber());
        tvBatchDate.setText(dateFormat.format(batch.getProcessingDate()));
        tvGrade.setText(batch.getQualityGrade().getDisplayName());

        // Quality Score
        int score = (int) batch.getQualityScore();
        tvQualityScore.setText(String.valueOf(score));
        qualityProgress.setProgress(score);
        qualityProgress.setMax(100);

        // Source Info
        if (batch.getSourceType() != null) {
            tvSourceType.setText(batch.getSourceType().getDisplayName());
        }

        // Supplier Name (if external)
        if (supplier != null && batch.getSupplierId() != null) {
            tvSupplierName.setText(supplier.getName());
            tvSupplierName.setVisibility(View.VISIBLE);
            btnViewSupplier.setVisibility(View.VISIBLE);
        } else {
            tvSupplierName.setVisibility(View.GONE);
            btnViewSupplier.setVisibility(View.GONE);
        }

        // Batch Type (Organic/Conventional)
        String batchNumber = batch.getBatchNumber();
        if (batchNumber.contains("-ORG-")) {
            tvBatchType.setText("Organic");
        } else if (batchNumber.contains("-CONV-")) {
            tvBatchType.setText("Conventional");
        } else {
            tvBatchType.setText("—");
        }

        // Metrics
        tvMoistureValue.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()));
        tvDefectiveValue.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getDefectiveRate()));
        tvNutCountValue.setText(String.valueOf(batch.getNutCountPerKg()));
        tvKernelValue.setText(String.format(Locale.getDefault(), "%.0f%%", batch.getKernelRatio()));
        tvWeightValue.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));

        // Test Results
        QualityBatchEntity.TestResults results =
                QualityBatchEntity.TestResults.fromJson(batch.getTestResults());

        if (results.hasResults()) {
            cardTestResults.setVisibility(View.VISIBLE);
            tvAflatoxinValue.setText(String.format(Locale.getDefault(), "%.2f", results.getAflatoxin()));
            tvPesticideValue.setText(String.format(Locale.getDefault(), "%.2f", results.getPesticideResidue()));
            tvHeavyMetalsValue.setText(String.format(Locale.getDefault(), "%.2f", results.getHeavyMetals()));
        } else {
            cardTestResults.setVisibility(View.GONE);
        }

        // ✅ Display Sustainability Data
        displaySustainabilityData();

        // Notes
        if (batch.getNotes() != null && !batch.getNotes().isEmpty()) {
            cardNotes.setVisibility(View.VISIBLE);
            tvNotesValue.setText(batch.getNotes());
        } else {
            cardNotes.setVisibility(View.GONE);
        }

        // Role-based UI
        if (!sessionManager.canEditData()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void displaySustainabilityData() {
        if (batch == null) return;

        if (batch.isVerified()) {
            layoutSustainability.setVisibility(View.VISIBLE);

            int score = batch.getSustainabilityScore();
            tvSustainabilityScore.setText(score + "%");

            tvVerifiedDate.setText("Verified: " + batch.getFormattedVerifiedDate());

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
                traceability.setHarvestCycleId(harvestCycleId);
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
    private void generateTraceability() {
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
            Toast.makeText(this, "Viewing supplier: " + supplier.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDialog() {
        QualityBatchDialog dialog = QualityBatchDialog.newEditInstance(farmId, batchId);
        dialog.setOnBatchAddedListener(() -> {
            loadBatchData();
            Toast.makeText(this, "Batch updated!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "EditQualityBatchDialog");
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