package com.afrinuts.farmos.ui.traceability;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.repository.MasterTraceabilityRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.QRCodeGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterTraceabilityDetailActivity extends BaseActivity {

    public static final String EXTRA_MASTER_ID = "master_id";
    private static final String TAG = "MasterTraceabilityDetail";

    private long masterId;
    private MasterBatchTraceabilityEntity master;
    private List<BatchTraceabilityEntity> linkedBatches = new ArrayList<>();

    private AppDatabase database;
    private MasterTraceabilityRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvMasterBatchNumber;
    private TextView tvShipmentName;
    private TextView tvHeroSubtitle;
    private TextView tvBuyer;
    private TextView tvDestination;
    private TextView tvExportDate;
    private TextView tvTotalWeight;
    private TextView tvTotalBatches;
    private TextView tvGrades;
    private TextView tvCertifications;
    private TextView tvTraceabilityUrl;
    private RecyclerView recyclerViewBatches;
    private MasterBatchAdapter batchAdapter;
    private MaterialButton btnShare;
    private MaterialButton btnDelete;
    private MaterialButton btnGenerateQR;
    private MaterialButton btnGenerateQRBottom;

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
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

        initViews();
        loadData();
    }

    private void initViews() {
        tvMasterBatchNumber = findViewById(R.id.tvMasterBatchNumber);
        tvShipmentName = findViewById(R.id.tvShipmentName);
        tvHeroSubtitle = findViewById(R.id.tvHeroSubtitle);
        tvBuyer = findViewById(R.id.tvBuyer);
        tvDestination = findViewById(R.id.tvDestination);
        tvExportDate = findViewById(R.id.tvExportDate);
        tvTotalWeight = findViewById(R.id.tvTotalWeight);
        tvTotalBatches = findViewById(R.id.tvTotalBatches);
        tvGrades = findViewById(R.id.tvGrades);
        tvCertifications = findViewById(R.id.tvCertifications);
        tvTraceabilityUrl = findViewById(R.id.tvTraceabilityUrl);
        recyclerViewBatches = findViewById(R.id.recyclerViewBatches);
        btnShare = findViewById(R.id.btnShare);
        btnDelete = findViewById(R.id.btnDelete);
        btnGenerateQR = findViewById(R.id.btnGenerateQR);
        btnGenerateQRBottom = findViewById(R.id.btnGenerateQRBottom);

        findViewById(R.id.btnHeaderBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnHeaderShare).setOnClickListener(v -> shareMasterTraceability());

        recyclerViewBatches.setLayoutManager(new LinearLayoutManager(this));

        batchAdapter = new MasterBatchAdapter(linkedBatches, batch -> {
            Intent intent = new Intent(this, TraceabilityDetailActivity.class);
            intent.putExtra(TraceabilityDetailActivity.EXTRA_TRACEABILITY_ID, batch.getId());
            startActivity(intent);
        });
        recyclerViewBatches.setAdapter(batchAdapter);

        btnShare.setOnClickListener(v -> shareMasterTraceability());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnGenerateQR.setOnClickListener(v -> generateQRCode());
        btnGenerateQRBottom.setOnClickListener(v -> generateQRCode());
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

            String masterBatchNumber = master.getMasterBatchNumber();
            List<BatchTraceabilityEntity> children =
                    repository.getChildrenOfMaster(masterBatchNumber);
            if (children == null) children = new ArrayList<>();

            List<Long> storedIds = master.getBatchTraceabilityIds();
            List<String> storedBatchNumbers = master.getBatchNumbers();
            int jsonIdCount = storedIds != null ? storedIds.size() : 0;
            int jsonNumberCount = storedBatchNumbers != null ? storedBatchNumbers.size() : 0;
            int linkedCount = children.size();
            Log.i(TAG, "Master children before repair: linked=" + linkedCount
                    + ", JSON ids=" + jsonIdCount
                    + ", JSON numbers=" + jsonNumberCount
                    + ", totalBatches=" + master.getTotalBatches());

            if (linkedCount < master.getTotalBatches()) {
                LinkedHashSet<Long> recoveryIds = new LinkedHashSet<>();

                if (storedIds != null) {
                    for (Long id : storedIds) {
                        if (id != null && database.batchTraceabilityDao().getById(id) != null) {
                            recoveryIds.add(id);
                        }
                    }
                }
                if (storedBatchNumbers != null) {
                    for (String batchNumber : storedBatchNumbers) {
                        if (batchNumber == null || batchNumber.trim().isEmpty()) continue;
                        BatchTraceabilityEntity child =
                                database.batchTraceabilityDao().getByBatchNumber(batchNumber);
                        if (child != null) recoveryIds.add(child.getId());
                    }
                }

                repository.addChildrenToMasterSync(masterBatchNumber,
                        new ArrayList<>(recoveryIds));
                children = repository.getChildrenOfMaster(masterBatchNumber);
                if (children == null) children = new ArrayList<>();
                Log.i(TAG, "Master children after repair: linked=" + children.size()
                        + ", JSON ids=" + jsonIdCount
                        + ", JSON numbers=" + jsonNumberCount
                        + ", totalBatches=" + master.getTotalBatches()
                        + ", recovery candidates=" + recoveryIds.size());
            }

            linkedBatches.clear();
            for (BatchTraceabilityEntity batch : children) {
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
        String buyer = master.getBuyer() != null ? master.getBuyer() : "—";
        tvHeroSubtitle.setText(buyer + " · " + master.getFormattedExportDate());
        tvBuyer.setText(buyer);
        tvDestination.setText(master.getDestination() != null ? master.getDestination() : "—");
        tvExportDate.setText(master.getFormattedExportDate());
        tvTotalWeight.setText(String.format(Locale.getDefault(), "%,.0f", master.getTotalWeightKg()));
        tvTotalBatches.setText(String.valueOf(master.getTotalBatches()));
        String grades = master.getGrades();
        tvGrades.setText(grades != null && !grades.trim().isEmpty() ? grades : "—");
        String certifications = master.getCertifications();
        tvCertifications.setText(certifications != null && !certifications.trim().isEmpty()
                ? certifications : "—");
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
        shareText.append("AfriNuts Master Traceability\n");
        shareText.append("=================================\n");
        shareText.append("Master Batch: ").append(master.getMasterBatchNumber()).append("\n");
        shareText.append("Shipment: ").append(master.getShipmentName()).append("\n");
        shareText.append("Buyer: ").append(master.getBuyer()).append("\n");
        shareText.append("Destination: ").append(master.getDestination()).append("\n");
        shareText.append("Total Weight: ").append(master.getFormattedWeight()).append("\n");
        shareText.append("Total Batches: ").append(master.getTotalBatches()).append("\n");
        shareText.append("Grades: ").append(master.getGrades()).append("\n");
        shareText.append("Export Date: ").append(master.getFormattedExportDate()).append("\n");
        shareText.append("\n Trace URL: ").append(master.getTraceabilityUrl());

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

    private interface OnMasterBatchClickListener {
        void onBatchClick(BatchTraceabilityEntity batch);
    }

    private static class MasterBatchAdapter extends RecyclerView.Adapter<MasterBatchAdapter.BatchViewHolder> {
        private final List<BatchTraceabilityEntity> batches = new ArrayList<>();
        private final OnMasterBatchClickListener listener;

        MasterBatchAdapter(List<BatchTraceabilityEntity> initialBatches,
                           OnMasterBatchClickListener listener) {
            this.listener = listener;
            updateBatches(initialBatches);
        }

        void updateBatches(List<BatchTraceabilityEntity> newBatches) {
            batches.clear();
            if (newBatches != null) batches.addAll(newBatches);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_master_batch, parent, false);
            return new BatchViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
            BatchTraceabilityEntity batch = batches.get(position);
            holder.bind(batch, listener);
            holder.divider.setVisibility(position == batches.size() - 1 ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return batches.size();
        }

        private static class BatchViewHolder extends RecyclerView.ViewHolder {
            private final TextView batchNumber;
            private final TextView grade;
            private final TextView weightAndDate;
            private final View divider;

            BatchViewHolder(@NonNull View itemView) {
                super(itemView);
                batchNumber = itemView.findViewById(R.id.tvMasterChildBatchNumber);
                grade = itemView.findViewById(R.id.tvMasterChildGrade);
                weightAndDate = itemView.findViewById(R.id.tvMasterChildWeightDate);
                divider = itemView.findViewById(R.id.masterChildDivider);
            }

            void bind(BatchTraceabilityEntity batch, OnMasterBatchClickListener listener) {
                batchNumber.setText("#" + batch.getBatchNumber());
                String qualityGrade = batch.getQualityGrade();
                String gradeLabel = qualityGrade != null && !qualityGrade.trim().isEmpty()
                        ? qualityGrade.trim() : "—";
                grade.setText(gradeLabel.toUpperCase(Locale.ROOT));
                boolean premium = gradeLabel.toLowerCase(Locale.ROOT).contains("premium");
                grade.setBackgroundResource(premium
                        ? R.drawable.bg_revenue_premium_badge
                        : R.drawable.bg_revenue_standard_badge);
                grade.setTextColor(premium ? 0xFF0A5530 : 0xFF80601E);
                weightAndDate.setText(batch.getFormattedWeight() + " · "
                        + batch.getFormattedProcessingDate());
                itemView.setOnClickListener(v -> listener.onBatchClick(batch));
            }
        }
    }
}
