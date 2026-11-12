package com.afrinuts.farmos.ui.suppliers;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// --- new imports (add at the top of the file) ---
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.local.entity.SupplierScoreSnapshotEntity;
import com.afrinuts.farmos.data.repository.SupplierCommitmentRepository;
import com.afrinuts.farmos.data.repository.SupplierDisputeRepository;
import com.afrinuts.farmos.data.repository.SupplierInspectionRepository;
import com.afrinuts.farmos.data.repository.SupplierScoreSnapshotRepository;

import java.util.Date;


public class SupplierDetailActivity extends BaseActivity {

    public static final String EXTRA_SUPPLIER_ID = "supplier_id";

    private long supplierId;
    private SupplierEntity supplier;
    private SupplierRepository supplierRepository;
    private QualityBatchRepository qualityBatchRepository;
    private AppDatabase database;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvSupplierName;
    private TextView tvSupplierType;
    private TextView tvRating;
    private Chip chipStatus;
    private TextView tvContactPerson;
    private TextView tvPhone;
    private TextView tvEmail;
    private TextView tvLocation;
    private TextView tvAnnualVolume;
    private TextView tvAvgQualityScore;
    private LinearLayout layoutCertification;
    private TextView tvCertification;
    private TextView tvCertificationExpiry;
    private TextView tvTotalBatches;
    private TextView tvTotalBatchVolume;
    private RecyclerView batchesRecyclerView;
    private SupplierBatchAdapter batchAdapter;
    private MaterialButton btnRecordDelivery;
    private MaterialButton btnEditSupplier;
    private MaterialButton btnEditBatch;
    private MaterialButton btnDelete;
    private Button btnShowMap;
    private View layoutFarmerProfile;


    // ✅ Vector icon ImageViews
    private ImageView ivCertificationIcon;
    private ImageView ivStatusIcon;

    private List<QualityBatchEntity> batches = new ArrayList<>();

    private CardView cardCommitments, cardInspections, cardDisputes, cardScoreSnapshot;
    private LinearLayout containerCommitments, containerInspections, containerDisputes;
    private TextView tvCommitmentsEmpty, tvInspectionsEmpty, tvDisputesEmpty;
    private TextView tvOverallScore, tvConfidence, tvScoreBreakdown, tvScoreMeta;
    private com.google.android.material.button.MaterialButton btnAddCommitment, btnAddInspection;

    private SupplierCommitmentRepository commitmentRepository;
    private SupplierInspectionRepository inspectionRepository;
    private SupplierDisputeRepository disputeRepository;
    private SupplierScoreSnapshotRepository snapshotRepository;
    private SimpleDateFormat shortDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return sessionManager.isOwner() || sessionManager.isManager();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier_detail);

        supplierId = getIntent().getLongExtra(EXTRA_SUPPLIER_ID, -1);
        if (supplierId == -1) {
            Toast.makeText(this, "Supplier not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        supplierRepository = new SupplierRepository(this, farmId);
        qualityBatchRepository = new QualityBatchRepository(this, farmId);

        initViews();
        setupToolbar();
        loadSupplierData();
        loadSupplierBatches();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvSupplierName = findViewById(R.id.tvSupplierName);
        tvSupplierType = findViewById(R.id.tvSupplierType);
        tvRating = findViewById(R.id.tvRating);
        chipStatus = findViewById(R.id.chipStatus);
        tvContactPerson = findViewById(R.id.tvContactPerson);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvLocation = findViewById(R.id.tvLocation);
        tvAnnualVolume = findViewById(R.id.tvAnnualVolume);
        tvAvgQualityScore = findViewById(R.id.tvAvgQualityScore);
        layoutCertification = findViewById(R.id.layoutCertification);
        tvCertification = findViewById(R.id.tvCertification);
        tvCertificationExpiry = findViewById(R.id.tvCertificationExpiry);
        tvTotalBatches = findViewById(R.id.tvTotalBatches);
        tvTotalBatchVolume = findViewById(R.id.tvTotalBatchVolume);
        batchesRecyclerView = findViewById(R.id.batchesRecyclerView);
        btnRecordDelivery = findViewById(R.id.btnRecordDelivery);
        btnEditSupplier = findViewById(R.id.btnEditSupplier);
        btnEditBatch = findViewById(R.id.btnEditBatch);
        btnDelete = findViewById(R.id.btnDelete);
        btnShowMap = findViewById(R.id.btnShowMap);

        // ✅ Vector icon ImageViews
        ivCertificationIcon = findViewById(R.id.ivCertificationIcon);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);

        // Setup RecyclerView for batches
        batchesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        batchAdapter = new SupplierBatchAdapter(batches);
        batchAdapter.setOnBatchUpdatedListener(() -> {
            loadSupplierBatches();
            loadSupplierData();
        });

        batchAdapter.setOnBatchDisputeClickListener(batch -> {
            openDisputeDialog(
                    SupplierDisputeEntity.TargetEntity.BATCH,
                    batch.getId(),
                    batch.getBatchNumber());
        });

        batchesRecyclerView.setAdapter(batchAdapter);

        cardCommitments      = findViewById(R.id.cardCommitments);
        cardInspections      = findViewById(R.id.cardInspections);
        cardDisputes         = findViewById(R.id.cardDisputes);
        cardScoreSnapshot    = findViewById(R.id.cardScoreSnapshot);
        containerCommitments = findViewById(R.id.containerCommitments);
        containerInspections = findViewById(R.id.containerInspections);
        containerDisputes    = findViewById(R.id.containerDisputes);
        tvCommitmentsEmpty   = findViewById(R.id.tvCommitmentsEmpty);
        tvInspectionsEmpty   = findViewById(R.id.tvInspectionsEmpty);
        tvDisputesEmpty      = findViewById(R.id.tvDisputesEmpty);
        tvOverallScore       = findViewById(R.id.tvOverallScore);
        tvConfidence         = findViewById(R.id.tvConfidence);
        tvScoreBreakdown     = findViewById(R.id.tvScoreBreakdown);
        tvScoreMeta          = findViewById(R.id.tvScoreMeta);
        btnAddCommitment     = findViewById(R.id.btnAddCommitment);
        btnAddInspection     = findViewById(R.id.btnAddInspection);

        // Buttons
        btnShowMap.setOnClickListener(v -> openMap());
        btnRecordDelivery.setOnClickListener(v -> showRecordDeliveryDialog());
        btnEditSupplier.setOnClickListener(v -> showEditSupplierDialog());
        btnEditBatch.setOnClickListener(v -> showSelectBatchToEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        chipStatus.setOnClickListener(v -> showStatusChangeDialog());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Supplier Details");
        }
    }

    private void openMap() {
        if (supplier == null || !supplier.hasLocation()) {
            Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show();
            return;
        }

        String uri = "geo:" + supplier.getLatitude() + "," + supplier.getLongitude() +
                "?q=" + supplier.getLatitude() + "," + supplier.getLongitude();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSupplierData() {
        supplierRepository.getSupplierById(supplierId).observe(this, supplier -> {
            if (supplier != null) {
                this.supplier = supplier;
                displaySupplierData();
                loadTrustSections();
            } else {
                Toast.makeText(this, "Supplier not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadSupplierBatches() {
        executor.execute(() -> {
            try {
                List<QualityBatchEntity> supplierBatches =
                        qualityBatchRepository.getBatchesBySupplierIdSync(supplierId);

                runOnUiThread(() -> {
                    batches.clear();
                    if (supplierBatches != null && !supplierBatches.isEmpty()) {
                        batches.addAll(supplierBatches);
                    }
                    batchAdapter.updateBatches(batches);
                    updateBatchStats();

                    Log.d("SupplierDetail", "Loaded " + batches.size() + " batches for supplier " + supplierId);
                });
            } catch (Exception e) {
                Log.e("SupplierDetail", "Error loading batches", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading batches", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateBatchStats() {
        if (batches.isEmpty()) {
            tvTotalBatches.setText("Total: 0 batches");
            tvTotalBatchVolume.setText("Total: 0 kg");
            return;
        }

        int count = batches.size();
        double totalVolume = 0;
        for (QualityBatchEntity batch : batches) {
            totalVolume += batch.getWeightKg();
        }

        tvTotalBatches.setText("Total: " + count + " batch" + (count > 1 ? "es" : ""));
        tvTotalBatchVolume.setText(String.format(Locale.getDefault(), "Total: %,.0f kg", totalVolume));
    }

    private void displaySupplierData() {
        tvSupplierName.setText(supplier.getName());
        tvSupplierType.setText(supplier.getTypeDisplay());
        tvRating.setText(supplier.getFormattedRating());

        updateStatusDisplay();

        // ✅ Set icons with vector drawables
        setVectorIcon(tvContactPerson, R.drawable.ic_person);
        setVectorIcon(tvPhone, R.drawable.ic_phone);
        setVectorIcon(tvEmail, R.drawable.ic_email);
        setVectorIcon(tvLocation, R.drawable.ic_location);

        tvContactPerson.setText(supplier.getContactPerson() != null ?
                supplier.getContactPerson() : "No contact");
        tvPhone.setText(supplier.getPhone() != null ? supplier.getPhone() : "No phone");
        tvEmail.setText(supplier.getEmail() != null ? supplier.getEmail() : "No email");
        tvLocation.setText(supplier.getLocation() != null ? supplier.getLocation() : "No location");

        tvAnnualVolume.setText(String.format(Locale.getDefault(),
                "%,.0f kg", supplier.getAnnualVolumeKg()));
        tvAvgQualityScore.setText(String.format(Locale.getDefault(),
                "%.0f", supplier.getAverageQualityScore()));

        // ✅ Certification with vector icon
        if (supplier.isCertified() && supplier.getCertificationType() != null) {
            layoutCertification.setVisibility(View.VISIBLE);

            // Set certification icon
            if (ivCertificationIcon != null) {
                ivCertificationIcon.setImageResource(R.drawable.ic_certified);
                ivCertificationIcon.setVisibility(View.VISIBLE);
            }

            tvCertification.setText(supplier.getCertificationType());

            if (supplier.getCertificationExpiryDate() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                tvCertificationExpiry.setText("Expires: " +
                        sdf.format(supplier.getCertificationExpiryDate()));
            } else {
                tvCertificationExpiry.setText("No expiry");
            }
        } else {
            layoutCertification.setVisibility(View.GONE);
        }

        if (!sessionManager.canEditData()) {
            btnDelete.setVisibility(View.GONE);
        }

        // ✅ Fix: Use View instead of LinearLayout
        TextView tvYearsFarming = findViewById(R.id.tvYearsFarming);
        TextView tvFarmSize = findViewById(R.id.tvFarmSize);
        TextView tvQuote = findViewById(R.id.tvQuote);
        TextView tvBio = findViewById(R.id.tvBio);
        View layoutFirebreak = findViewById(R.id.layoutFirebreak);
        View layoutSustainable = findViewById(R.id.layoutSustainable);
        View layoutChildLabor = findViewById(R.id.layoutChildLabor);
        CardView layoutFarmerProfile = findViewById(R.id.layoutFarmerProfile);

        // Show farmer profile section only if there's data
        boolean hasProfileData = supplier.getYearsFarming() > 0 ||
                supplier.getFarmSize() > 0 ||
                !TextUtils.isEmpty(supplier.getQuote()) ||
                !TextUtils.isEmpty(supplier.getBio());

        if (hasProfileData) {
            layoutFarmerProfile.setVisibility(View.VISIBLE);

            tvYearsFarming.setText(supplier.getFormattedYearsFarming());
            tvFarmSize.setText(String.format(Locale.getDefault(), "%.1f hectares", supplier.getFarmSize()));

            if (!TextUtils.isEmpty(supplier.getQuote())) {
                tvQuote.setText("\"" + supplier.getQuote() + "\"");
                tvQuote.setVisibility(View.VISIBLE);
            } else {
                tvQuote.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(supplier.getBio())) {
                tvBio.setText(supplier.getBio());
                tvBio.setVisibility(View.VISIBLE);
            } else {
                tvBio.setVisibility(View.GONE);
            }

            // Sustainability badges
            layoutFirebreak.setVisibility(supplier.isFirebreakProtection() ? View.VISIBLE : View.GONE);
            layoutSustainable.setVisibility(supplier.isSustainableHarvesting() ? View.VISIBLE : View.GONE);
            layoutChildLabor.setVisibility(supplier.isNoChildLabor() ? View.VISIBLE : View.GONE);

        } else {
            layoutFarmerProfile.setVisibility(View.GONE);
        }
    }

    // ✅ Helper method to set compound drawable vector icons
    private void setVectorIcon(TextView textView, int iconRes) {
        Drawable drawable = ContextCompat.getDrawable(this, iconRes);
        if (drawable != null) {
            // Apply tint using DrawableCompat
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, getColor(com.google.android.material.R.color.design_default_color_on_primary));
            drawable.setBounds(0, 0, 24, 24);
            textView.setCompoundDrawables(drawable, null, null, null);
            textView.setCompoundDrawablePadding(12);
        }
    }

    private void updateStatusDisplay() {
        String statusText = supplier.getStatusDisplay();
        chipStatus.setText(statusText);

        // ✅ Set status icon
        if (ivStatusIcon != null) {
            switch (supplier.getStatus()) {
                case ACTIVE:
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(getColor(android.R.color.holo_green_light));
                    break;
                case PENDING:
                    ivStatusIcon.setImageResource(R.drawable.ic_hourglass);
                    ivStatusIcon.setColorFilter(getColor(android.R.color.holo_orange_light));
                    break;
                case SUSPENDED:
                    ivStatusIcon.setImageResource(R.drawable.ic_warning);
                    ivStatusIcon.setColorFilter(getColor(android.R.color.holo_red_light));
                    break;
                case INACTIVE:
                    ivStatusIcon.setImageResource(R.drawable.ic_cancel);
                    ivStatusIcon.setColorFilter(getColor(android.R.color.darker_gray));
                    break;
                default:
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(getColor(R.color.primary));
                    break;
            }
        }

        switch (supplier.getStatus()) {
            case ACTIVE:
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
                break;
            case PENDING:
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                break;
            case SUSPENDED:
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
                break;
            case INACTIVE:
                chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
                break;
            default:
                chipStatus.setChipBackgroundColorResource(R.color.primary);
                break;
        }
    }

    private void showStatusChangeDialog() {
        if (supplier == null) return;

        String[] statusOptions = {
                "Active - Currently supplying",
                "Pending - Awaiting approval",
                "Suspended - Temporarily suspended",
                "Inactive - No longer supplying"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Status")
                .setItems(statusOptions, (dialog, which) -> {
                    SupplierEntity.SupplierStatus newStatus;
                    switch (which) {
                        case 0:
                            newStatus = SupplierEntity.SupplierStatus.ACTIVE;
                            break;
                        case 1:
                            newStatus = SupplierEntity.SupplierStatus.PENDING;
                            break;
                        case 2:
                            newStatus = SupplierEntity.SupplierStatus.SUSPENDED;
                            break;
                        case 3:
                            newStatus = SupplierEntity.SupplierStatus.INACTIVE;
                            break;
                        default:
                            return;
                    }
                    updateSupplierStatus(newStatus);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSupplierStatus(SupplierEntity.SupplierStatus newStatus) {
        if (supplier == null) return;

        supplier.setStatus(newStatus);
        supplier.setUpdatedAt(System.currentTimeMillis());

        supplierRepository.updateSupplier(supplier, new SupplierRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    updateStatusDisplay();
                    Toast.makeText(SupplierDetailActivity.this,
                            "Status updated to: " + newStatus.getDisplayName(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(SupplierDetailActivity.this,
                                "Error updating status: " + error,
                                Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void showEditSupplierDialog() {
        EditSupplierDialog dialog = EditSupplierDialog.newInstance(supplierId);
        dialog.setOnSupplierUpdatedListener(() -> {
            loadSupplierData();
            Toast.makeText(this, "Supplier updated!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "EditSupplierDialog");
    }

    private void showSelectBatchToEditDialog() {
        if (batches.isEmpty()) {
            Toast.makeText(this, "No batches to edit. Record a delivery first.", Toast.LENGTH_SHORT).show();
            return;
        }

        SelectBatchToEditDialog dialog = SelectBatchToEditDialog.newInstance(supplierId);
        dialog.setOnBatchSelectedListener(batch -> {
            EditRecordDeliveryDialog editDialog = EditRecordDeliveryDialog.newInstance(batch.getId());
            editDialog.setOnDeliveryUpdatedListener(() -> {
                loadSupplierBatches();
                loadSupplierData();
                Toast.makeText(this, "Batch updated successfully!", Toast.LENGTH_SHORT).show();
            });
            editDialog.show(getSupportFragmentManager(), "EditRecordDeliveryDialog");
        });
        dialog.show(getSupportFragmentManager(), "SelectBatchToEditDialog");
    }

    private void confirmDelete() {
        if (supplier == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Supplier")
                .setMessage("Are you sure you want to delete " + supplier.getName() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteSupplier())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSupplier() {
        if (supplier == null) return;

        Toast.makeText(this, "Deleting supplier...", Toast.LENGTH_SHORT).show();

        supplierRepository.deleteSupplier(supplier, new SupplierRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(SupplierDetailActivity.this,
                            "Supplier deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(SupplierDetailActivity.this,
                                "Error deleting: " + error,
                                Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void showRecordDeliveryDialog() {
        RecordDeliveryDialog dialog = RecordDeliveryDialog.newInstance(supplierId);
        dialog.setOnDeliveryRecordedListener(() -> {
            loadSupplierData();
            loadSupplierBatches();
            Toast.makeText(this, "Delivery recorded!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "RecordDeliveryDialog");
    }

    // ============================================================
// TRUST SECTIONS — 6a
// ============================================================

    private void loadTrustSections() {
        if (supplier == null) return;

        // (Re)build repositories against the currently loaded farm
        if (commitmentRepository == null) {
            commitmentRepository = new SupplierCommitmentRepository(this, farmId);
            inspectionRepository = new SupplierInspectionRepository(this, farmId);
            disputeRepository    = new SupplierDisputeRepository(this, farmId);
            snapshotRepository   = new SupplierScoreSnapshotRepository(this, farmId);
        }

        loadCommitments();
        loadInspections();
        loadDisputes();
        loadScoreSnapshot();
        wireSectionActions();
    }

    private void wireSectionActions() {
        btnAddCommitment.setOnClickListener(v -> {
            if (supplier == null) return;
            RecordCommitmentDialog d = RecordCommitmentDialog.newInstance(supplier.getId(), farmId);
            d.setOnCommitmentCreatedListener(id -> loadCommitments());
            d.show(getSupportFragmentManager(), "RecordCommitmentDialog");
        });

        btnAddInspection.setOnClickListener(v -> {
            if (supplier == null) return;
            RecordInspectionDialog d = RecordInspectionDialog.newInstance(supplier.getId(), farmId);
            d.setOnInspectionCreatedListener(id -> loadInspections());
            d.show(getSupportFragmentManager(), "RecordInspectionDialog");
        });
    }

// ---------- Commitments ----------

    private void loadCommitments() {
        if (supplier == null) return;
        commitmentRepository.getBySupplierId(supplier.getId())
                .thenAccept(list -> runOnUiThread(() -> renderCommitments(list)));
    }

    private void renderCommitments(List<SupplierCommitmentEntity> list) {
        cardCommitments.setVisibility(View.VISIBLE);
        containerCommitments.removeAllViews();

        if (list == null || list.isEmpty()) {
            tvCommitmentsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvCommitmentsEmpty.setVisibility(View.GONE);

        for (SupplierCommitmentEntity c : list) {
            View row = getLayoutInflater().inflate(R.layout.item_commitment, containerCommitments, false);
            bindCommitmentRow(row, c);
            containerCommitments.addView(row);
        }
    }

    private void bindCommitmentRow(View row, SupplierCommitmentEntity c) {
        TextView tvContract = row.findViewById(R.id.tvContract);
        TextView tvSeason   = row.findViewById(R.id.tvSeason);
        TextView tvCommitted = row.findViewById(R.id.tvCommitted);
        TextView tvDelivered = row.findViewById(R.id.tvDelivered);
        TextView tvStatus   = row.findViewById(R.id.tvStatus);
        TextView tvDate     = row.findViewById(R.id.tvDate);
        View btnRev         = row.findViewById(R.id.btnAppendRevision);
        View btnLink        = row.findViewById(R.id.btnLinkBatches);
        View btnClose       = row.findViewById(R.id.btnCloseCommitment);
        View btnDispute     = row.findViewById(R.id.btnDisputeCommitment);

        String contract = (c.getContractId() == null || c.getContractId().isEmpty())
                ? "No contract" : c.getContractId();
        tvContract.setText(contract);
        tvSeason.setText("Season " + c.getSeason());
        tvCommitted.setText(String.format(Locale.getDefault(), "Committed: %,.0f kg", c.getCommittedKg()));
        tvDate.setText(shortDate.format(new Date(c.getCommittedDate())));
        tvStatus.setText(c.getStatus() != null ? c.getStatus().name() : "—");

        // Derived delivered kg — only shown once the repository answers
        commitmentRepository.getDeliveredKg(c.getId()).thenAccept(kg ->
                runOnUiThread(() -> tvDelivered.setText(
                        String.format(Locale.getDefault(), "Delivered: %,.0f kg", kg))));

        btnRev.setOnClickListener(v -> {
            AppendRevisionDialog d = AppendRevisionDialog.newInstance(c.getId(), farmId);
            d.setOnRevisionAppendedListener(this::loadCommitments);
            d.show(getSupportFragmentManager(), "AppendRevisionDialog");
        });

        btnLink.setOnClickListener(v -> {
            LinkBatchesDialog d = LinkBatchesDialog.newInstance(c.getId(), supplier.getId(), farmId);
            d.setOnBatchesLinkedListener(this::loadCommitments);
            d.show(getSupportFragmentManager(), "LinkBatchesDialog");
        });

        btnClose.setOnClickListener(v -> {
            CloseCommitmentDialog d = CloseCommitmentDialog.newInstance(c.getId(), farmId);
            d.setOnClosedListener(this::loadCommitments);
            d.show(getSupportFragmentManager(), "CloseCommitmentDialog");
        });

        btnDispute.setOnClickListener(v -> openDisputeDialog(
                SupplierDisputeEntity.TargetEntity.COMMITMENT,
                c.getId(),
                c.getContractId()));
    }

// ---------- Inspections ----------

    private void loadInspections() {
        if (supplier == null) return;
        inspectionRepository.getBySupplierId(supplier.getId())
                .thenAccept(list -> runOnUiThread(() -> renderInspections(list)));
    }

    private void renderInspections(List<SupplierInspectionEntity> list) {
        cardInspections.setVisibility(View.VISIBLE);
        containerInspections.removeAllViews();

        if (list == null || list.isEmpty()) {
            tvInspectionsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvInspectionsEmpty.setVisibility(View.GONE);

        for (SupplierInspectionEntity i : list) {
            View row = getLayoutInflater().inflate(R.layout.item_supplier_inspection, containerInspections, false);
            bindInspectionRow(row, i);
            containerInspections.addView(row);
        }
    }

    private void bindInspectionRow(View row, SupplierInspectionEntity i) {
        TextView tvType     = row.findViewById(R.id.tvInspectionType);
        TextView tvOutcome  = row.findViewById(R.id.tvOutcome);
        TextView tvFindings = row.findViewById(R.id.tvFindings);
        TextView tvDate     = row.findViewById(R.id.tvDate);
        TextView tvInspector = row.findViewById(R.id.tvInspector);
        View btnDispute     = row.findViewById(R.id.btnDisputeInspection);

        tvType.setText(i.getInspectionType() != null ? i.getInspectionType().name() : "—");
        tvOutcome.setText(i.getOutcome() != null ? i.getOutcome().name() : "—");
        tvFindings.setText(i.getFindings() != null ? i.getFindings() : "");
        tvDate.setText(shortDate.format(new Date(i.getInspectionDate())));
        tvInspector.setText(i.getInspector() != null ? i.getInspector() : "");

        btnDispute.setOnClickListener(v -> openDisputeDialog(
                SupplierDisputeEntity.TargetEntity.INSPECTION,
                i.getId(),
                (i.getInspectionType() != null ? i.getInspectionType().name() : "Inspection")
                        + " " + shortDate.format(new Date(i.getInspectionDate()))));
    }

// ---------- Disputes ----------

    private void loadDisputes() {
        if (supplier == null) return;
        disputeRepository.getBySupplierId(supplier.getId())
                .thenAccept(list -> runOnUiThread(() -> renderDisputes(list)));
    }

    private void renderDisputes(List<SupplierDisputeEntity> list) {
        cardDisputes.setVisibility(View.VISIBLE);
        containerDisputes.removeAllViews();

        if (list == null || list.isEmpty()) {
            tvDisputesEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvDisputesEmpty.setVisibility(View.GONE);

        for (SupplierDisputeEntity d : list) {
            View row = getLayoutInflater().inflate(R.layout.item_supplier_dispute, containerDisputes, false);
            bindDisputeRow(row, d);
            containerDisputes.addView(row);
        }
    }

    private void bindDisputeRow(View row, SupplierDisputeEntity d) {
        TextView tvTarget = row.findViewById(R.id.tvTarget);
        TextView tvStatus = row.findViewById(R.id.tvStatus);
        TextView tvReason = row.findViewById(R.id.tvReason);
        TextView tvDate   = row.findViewById(R.id.tvDisputeDate);

        tvTarget.setText("Target: " + d.getTargetEntity().name() + " #" + d.getTargetId());
        tvStatus.setText(d.getReviewStatus() != null ? d.getReviewStatus().name() : "OPEN");
        tvReason.setText(d.getReason() != null ? d.getReason() : "");
        tvDate.setText(shortDate.format(new Date(d.getSubmittedDate())));
    }

    private void openDisputeDialog(SupplierDisputeEntity.TargetEntity entity,
                                   long targetId,
                                   String targetLabel) {
        if (supplier == null) return;
        RaiseDisputeDialog d = RaiseDisputeDialog.newInstance(
                supplier.getId(), farmId, entity, targetId, targetLabel);
        d.setOnDisputeRaisedListener(id -> loadDisputes());
        d.show(getSupportFragmentManager(), "RaiseDisputeDialog");
    }

// ---------- Latest snapshot (read-only) ----------

    private void loadScoreSnapshot() {
        if (supplier == null) return;
        snapshotRepository.getLatestLive(supplier.getId())
                .observe(this, this::renderSnapshot);
    }

    private void renderSnapshot(SupplierScoreSnapshotEntity s) {
        cardScoreSnapshot.setVisibility(View.VISIBLE);

        if (s == null) {
            tvOverallScore.setText("—");
            tvConfidence.setText("No score yet");
            tvScoreBreakdown.setText("");
            tvScoreMeta.setText("Scores are computed on the portal from validated evidence.");
            return;
        }

        if (s.getOverall() == null) {
            tvOverallScore.setText("—");
        } else {
            tvOverallScore.setText(String.format(Locale.getDefault(), "%.0f", s.getOverall()));
        }
        tvConfidence.setText(s.getConfidence() != null ? s.getConfidence().name() : "—");

        StringBuilder b = new StringBuilder();
        appendDim(b, "Quality",       s.getQuality());
        appendDim(b, "Delivery",      s.getDelivery());
        appendDim(b, "Traceability",  s.getTraceability());
        appendDim(b, "Compliance",    s.getCompliance());
        appendDim(b, "Sustainability", s.getSustainability());
        appendDim(b, "Cooperation",   s.getCooperation());
        tvScoreBreakdown.setText(b.toString().trim());

        String meta = "v" + (s.getScoringVersion() != null ? s.getScoringVersion() : "?")
                + " · " + shortDate.format(new Date(s.getCalculatedAt()))
                + " · " + s.getVerifiedTransactions() + " verified transactions";
        tvScoreMeta.setText(meta);
    }

    private void appendDim(StringBuilder b, String name, Double value) {
        if (value == null) return;
        b.append(name).append(": ")
                .append(String.format(Locale.getDefault(), "%.0f", value))
                .append("   ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commitmentRepository != null) commitmentRepository.shutdown();
        if (inspectionRepository != null) inspectionRepository.shutdown();
        if (disputeRepository != null)    disputeRepository.shutdown();
        if (snapshotRepository != null)   snapshotRepository.shutdown();
        if (supplierRepository != null)   supplierRepository.shutdown();
        if (qualityBatchRepository != null) qualityBatchRepository.shutdown();
        executor.shutdown();
    }
}