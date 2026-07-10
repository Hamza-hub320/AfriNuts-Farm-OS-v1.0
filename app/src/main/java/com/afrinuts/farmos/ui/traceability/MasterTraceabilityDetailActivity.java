package com.afrinuts.farmos.ui.traceability;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.data.repository.MasterTraceabilityRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.QRCodeGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.afrinuts.farmos.ui.traceability.TraceabilityAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterTraceabilityDetailActivity extends BaseActivity {

    public static final String EXTRA_MASTER_ID = "master_id";

    private long masterId;
    private MasterBatchTraceabilityEntity master;
    private List<BatchTraceabilityEntity> linkedBatches = new ArrayList<>();

    private AppDatabase database;
    private MasterTraceabilityRepository repository;
    private BatchTraceabilityRepository batchRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvMasterBatchNumber;
    private TextView tvShipmentName;
    private TextView tvBuyer;
    private TextView tvDestination;
    private TextView tvExportDate;
    private TextView tvTotalWeight;
    private TextView tvTotalBatches;
    private TextView tvGrades;
    private TextView tvCertifications;
    private TextView tvTraceabilityUrl;
    private CardView layoutSustainability;
    private RecyclerView recyclerViewBatches;
    private TraceabilityAdapter batchAdapter;  // ✅ Use TraceabilityAdapter instead
    private MaterialButton btnShare;
    private MaterialButton btnDelete;
    private MaterialButton btnGenerateQR;

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_traceability_detail);

        masterId = getIntent().getLongExtra(EXTRA_MASTER_ID, -1);
        if (masterId == -1) {
            Toast.makeText(this, "Master traceability not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        repository = new MasterTraceabilityRepository(this, 1);
        batchRepository = new BatchTraceabilityRepository(this, 1);

        initViews();
        setupToolbar();
        loadData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvMasterBatchNumber = findViewById(R.id.tvMasterBatchNumber);
        tvShipmentName = findViewById(R.id.tvShipmentName);
        tvBuyer = findViewById(R.id.tvBuyer);
        tvDestination = findViewById(R.id.tvDestination);
        tvExportDate = findViewById(R.id.tvExportDate);
        tvTotalWeight = findViewById(R.id.tvTotalWeight);
        tvTotalBatches = findViewById(R.id.tvTotalBatches);
        tvGrades = findViewById(R.id.tvGrades);
        tvCertifications = findViewById(R.id.tvCertifications);
        tvTraceabilityUrl = findViewById(R.id.tvTraceabilityUrl);
        layoutSustainability = findViewById(R.id.layoutSustainability);
        recyclerViewBatches = findViewById(R.id.recyclerViewBatches);
        btnShare = findViewById(R.id.btnShare);
        btnDelete = findViewById(R.id.btnDelete);
        btnGenerateQR = findViewById(R.id.btnGenerateQR);

        recyclerViewBatches.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Use TraceabilityAdapter with click listener
        batchAdapter = new TraceabilityAdapter(linkedBatches, traceability -> {
            Intent intent = new Intent(this, TraceabilityDetailActivity.class);
            intent.putExtra(TraceabilityDetailActivity.EXTRA_TRACEABILITY_ID, traceability.getId());
            startActivity(intent);
        });
        recyclerViewBatches.setAdapter(batchAdapter);

        btnShare.setOnClickListener(v -> shareMasterTraceability());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnGenerateQR.setOnClickListener(v -> generateQRCode());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Master Traceability");
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

    private void loadData() {
        executor.execute(() -> {
            master = repository.getMasterByIdSync(masterId);

            if (master == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Master traceability not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            // Load linked batches
            List<Long> batchIds = master.getBatchTraceabilityIds();
            linkedBatches.clear();
            for (Long id : batchIds) {
                BatchTraceabilityEntity batch = batchRepository.getTraceabilityByIdSync(id);
                if (batch != null) {
                    linkedBatches.add(batch);
                }
            }

            runOnUiThread(() -> displayMasterData());
        });
    }

    private void displayMasterData() {
        if (master == null) return;

        tvMasterBatchNumber.setText("#" + master.getMasterBatchNumber());
        tvShipmentName.setText(master.getShipmentName());
        tvBuyer.setText(master.getBuyer() != null ? master.getBuyer() : "—");
        tvDestination.setText(master.getDestination() != null ? master.getDestination() : "—");
        tvExportDate.setText(master.getFormattedExportDate());
        tvTotalWeight.setText(master.getFormattedWeight());
        tvTotalBatches.setText(master.getTotalBatches() + " batches");
        tvGrades.setText(master.getGrades() != null ? master.getGrades() : "—");
        tvCertifications.setText(master.getCertifications() != null ? master.getCertifications() : "—");
        tvTraceabilityUrl.setText(master.getTraceabilityUrl());

        // ✅ Update adapter
        batchAdapter.updateBatches(linkedBatches);

        // Role-based UI
        if (!sessionManager.canEditData()) {
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void generateQRCode() {
        String qrUrl = master.getQrCodeUrl();
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrUrl);
        if (qrBitmap != null) {
            // Show QR code in a dialog
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Master QR Code")
                    .setMessage("Scan this QR code to view the master traceability")
                    .setPositiveButton("Share", (dialog, which) -> {
                        // Share QR code
                        Toast.makeText(this, "Share QR code", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Close", null)
                    .show();
            // TODO: Show QR code image in dialog with custom view
        } else {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareMasterTraceability() {
        StringBuilder shareText = new StringBuilder();
        shareText.append("🌿 AfriNuts Master Traceability\n");
        shareText.append("=================================\n");
        shareText.append("Master Batch: ").append(master.getMasterBatchNumber()).append("\n");
        shareText.append("Shipment: ").append(master.getShipmentName()).append("\n");
        shareText.append("Buyer: ").append(master.getBuyer()).append("\n");
        shareText.append("Destination: ").append(master.getDestination()).append("\n");
        shareText.append("Total Weight: ").append(master.getFormattedWeight()).append("\n");
        shareText.append("Total Batches: ").append(master.getTotalBatches()).append("\n");
        shareText.append("Grades: ").append(master.getGrades()).append("\n");
        shareText.append("Export Date: ").append(master.getFormattedExportDate()).append("\n");
        shareText.append("\n🔗 Trace URL: ").append(master.getTraceabilityUrl());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Master Traceability"));
    }

    private void confirmDelete() {
        if (!sessionManager.isOwner()) {
            Toast.makeText(this, "Only owners can delete master traceability records", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Master Traceability")
                .setMessage("Are you sure you want to delete this master traceability record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMasterTraceability())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMasterTraceability() {
        Toast.makeText(this, "Deleting...", Toast.LENGTH_SHORT).show();

        repository.deleteMasterTraceability(master, new MasterTraceabilityRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(MasterTraceabilityDetailActivity.this,
                            "Master traceability deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(MasterTraceabilityDetailActivity.this,
                                "Error deleting: " + error, Toast.LENGTH_LONG).show()
                );
            }
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