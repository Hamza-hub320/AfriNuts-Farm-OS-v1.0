package com.afrinuts.farmos.ui.quality;

import android.os.Bundle;
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
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QualityBatchDetailActivity extends BaseActivity {

    public static final String EXTRA_BATCH_ID = "batch_id";

    private long batchId;
    private QualityBatchEntity batch;
    private SupplierEntity supplier;
    private QualityBatchRepository repository;
    private SupplierRepository supplierRepository;
    private AppDatabase database;
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

    // ✅ Sustainability UI Elements (only declare once)
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

        // ✅ Sustainability UI Elements (only initialize once)
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

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnViewSupplier.setOnClickListener(v -> viewSupplier());
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

            // Set the text values
            tvNoChildLabor.setText(batch.isNoChildLabor() ? "Yes" : "No");
            tvFarmOsMonitored.setText(batch.isFarmOsMonitored() ? "Yes" : "No");
            tvFirebreakProtection.setText(batch.isFirebreakProtection() ? "Yes" : "No");
            tvSustainableHarvesting.setText(batch.isSustainableHarvesting() ? "Yes" : "No");

            // ✅ Use android.R.color for built-in colors or define custom
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