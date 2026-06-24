package com.afrinuts.farmos.ui.quality;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QualityBatchDetailActivity extends BaseActivity {

    public static final String EXTRA_BATCH_ID = "batch_id";

    private long batchId;
    private QualityBatchEntity batch;
    private QualityBatchRepository repository;
    private AppDatabase database;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvBatchNumber;
    private TextView tvGrade;
    private TextView tvQualityScore;
    private TextView tvMoistureValue;
    private TextView tvDefectiveValue;
    private TextView tvNutCountValue;
    private TextView tvKernelValue;
    private TextView tvWeightValue;
    private TextView tvDateValue;
    private TextView tvAflatoxinValue;
    private TextView tvPesticideValue;
    private TextView tvHeavyMetalsValue;
    private TextView tvNotesValue;
    private MaterialButton btnEdit;
    private MaterialButton btnDelete;
    private View cardTestResults;
    private View cardNotes;

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

        tvBatchNumber = findViewById(R.id.tvBatchNumber);
        tvGrade = findViewById(R.id.tvGrade);
        tvQualityScore = findViewById(R.id.tvQualityScore);
        tvMoistureValue = findViewById(R.id.tvMoistureValue);
        tvDefectiveValue = findViewById(R.id.tvDefectiveValue);
        tvNutCountValue = findViewById(R.id.tvNutCountValue);
        tvKernelValue = findViewById(R.id.tvKernelValue);
        tvWeightValue = findViewById(R.id.tvWeightValue);
        tvDateValue = findViewById(R.id.tvDateValue);
        tvAflatoxinValue = findViewById(R.id.tvAflatoxinValue);
        tvPesticideValue = findViewById(R.id.tvPesticideValue);
        tvHeavyMetalsValue = findViewById(R.id.tvHeavyMetalsValue);
        tvNotesValue = findViewById(R.id.tvNotesValue);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        cardTestResults = findViewById(R.id.cardTestResults);
        cardNotes = findViewById(R.id.cardNotes);

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
            // First get farm ID
            farmId = database.farmDao().getFirstFarm().getId();
            repository = new QualityBatchRepository(this, farmId);

            // Load batch
            batch = repository.getBatchByIdSync(batchId);

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
        // Batch number
        tvBatchNumber.setText("#" + batch.getBatchNumber());

        // Grade
        tvGrade.setText(batch.getQualityGrade().getIcon() + " " +
                batch.getQualityGrade().getDisplayName());

        // Quality score
        tvQualityScore.setText(String.valueOf((int) batch.getQualityScore()));

        // Metrics
        tvMoistureValue.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()));
        tvDefectiveValue.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getDefectiveRate()));
        tvNutCountValue.setText(String.valueOf(batch.getNutCountPerKg()));
        tvKernelValue.setText(String.format(Locale.getDefault(), "%.0f%%", batch.getKernelRatio()));
        tvWeightValue.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));
        tvDateValue.setText(dateFormat.format(batch.getProcessingDate()));

        // Test results
        QualityBatchEntity.TestResults results =
                QualityBatchEntity.TestResults.fromJson(batch.getTestResults());

        if (results.hasResults()) {
            cardTestResults.setVisibility(View.VISIBLE);
            tvAflatoxinValue.setText(String.valueOf(results.getAflatoxin()));
            tvPesticideValue.setText(String.valueOf(results.getPesticideResidue()));
            tvHeavyMetalsValue.setText(String.valueOf(results.getHeavyMetals()));
        } else {
            cardTestResults.setVisibility(View.GONE);
        }

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
        executor.shutdown();
    }
}