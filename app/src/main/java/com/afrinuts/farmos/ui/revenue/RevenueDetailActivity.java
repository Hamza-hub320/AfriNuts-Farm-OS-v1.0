package com.afrinuts.farmos.ui.revenue;

import android.content.Intent;
import android.os.Bundle;
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

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.revenue.AddRevenueDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevenueDetailActivity extends BaseActivity {

    public static final String EXTRA_REVENUE_ID = "revenue_id";
    private static final String TAG = "RevenueDetailActivity";

    private long revenueId;
    private RevenueEntity revenue;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;
    private FirebaseFirestore firestore;
    private ListenerRegistration revenueListener;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // UI Elements
    private ImageView tvQualityIcon;
    private TextView tvQualityGrade;
    private TextView tvSource;
    private TextView tvQuantity;
    private TextView tvPricePerKg;
    private TextView tvTotalAmount;
    private TextView tvHarvestDate;
    private TextView tvBuyer;
    private TextView tvNotes;
    private LinearLayout notesLayout;
    private Button btnDelete;
    private MaterialButton btnEdit;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        // Investors cannot view revenue details
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_detail);

        // Check if user has permission
        if (!checkRoleAccess()) {
            Toast.makeText(this, "You don't have permission to view revenue details",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Get revenue ID from intent
        revenueId = getIntent().getLongExtra(EXTRA_REVENUE_ID, -1);
        if (revenueId == -1) {
            Toast.makeText(this, "Revenue not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize database and Firestore
        database = AppDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup role-based UI
        setupRoleBasedUI();

        // Load data
        loadData();

        // Start real-time listener for this revenue
        startRevenueListener();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvQualityIcon = findViewById(R.id.tvQualityIcon);
        tvQualityGrade = findViewById(R.id.tvQualityGrade);
        tvSource = findViewById(R.id.tvSource);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvPricePerKg = findViewById(R.id.tvPricePerKg);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvHarvestDate = findViewById(R.id.tvHarvestDate);
        tvBuyer = findViewById(R.id.tvBuyer);
        tvNotes = findViewById(R.id.tvNotes);
        notesLayout = findViewById(R.id.notesLayout);
        btnDelete = findViewById(R.id.btnDelete);

        // Try to find btnEdit if it exists in your layout
        btnEdit = findViewById(R.id.btnEdit);

        btnDelete.setOnClickListener(v -> confirmDelete());

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> openEditDialog());
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Revenue Details");
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

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    private void startRevenueListener() {
        Log.d(TAG, "Starting real-time listener for revenue: " + revenueId);

        revenueListener = firestore.collection("revenues")
                .whereEqualTo("id", revenueId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Revenue listener failed", error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        RevenueEntity updatedRevenue = documentToRevenue(doc);

                        if (updatedRevenue != null) {
                            Log.d(TAG, "Revenue updated remotely: " + updatedRevenue.getId());

                            // Update local database
                            databaseExecutor.execute(() -> {
                                database.revenueDao().update(updatedRevenue);

                                runOnUiThread(() -> {
                                    revenue = updatedRevenue;
                                    displayRevenueData();
                                    Toast.makeText(this,
                                            "Revenue updated by another user",
                                            Toast.LENGTH_SHORT).show();
                                });
                            });
                        }
                    } else {
                        // Revenue was deleted remotely
                        Log.d(TAG, "Revenue no longer exists in Firestore");
                        runOnUiThread(() -> {
                            Toast.makeText(this,
                                    "This revenue has been deleted by another user",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                });
    }

    private RevenueEntity documentToRevenue(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            long farmId = doc.getLong("farmId");
            Long blockId = doc.getLong("blockId");
            long harvestDate = doc.getLong("harvestDate");
            double quantityKg = doc.getDouble("quantityKg");
            double pricePerKg = doc.getDouble("pricePerKg");
            double totalAmount = doc.getDouble("totalAmount");
            String buyer = doc.getString("buyer");
            String quality = doc.getString("quality");
            String notes = doc.getString("notes");
            long createdAt = doc.getLong("createdAt");
            long updatedAt = doc.getLong("updatedAt");

            RevenueEntity revenue = new RevenueEntity(
                    farmId,
                    blockId,
                    harvestDate,
                    quantityKg,
                    pricePerKg,
                    buyer,
                    RevenueEntity.QualityGrade.valueOf(quality),
                    notes
            );
            revenue.setId(id);
            revenue.setCreatedAt(createdAt);
            revenue.setUpdatedAt(updatedAt);

            return revenue;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to revenue", e);
            return null;
        }
    }

    private void setupRoleBasedUI() {
        // Only Owners and Managers can edit/delete
        if (sessionManager.canEditData()) {
            if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        } else {
            // Investors - view only
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        }
    }

    private void loadData() {
        databaseExecutor.execute(() -> {
            revenue = database.revenueDao().getRevenueById(revenueId);

            if (revenue == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Revenue not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            currentFarm = database.farmDao().getFirstFarm();
            allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());

            runOnUiThread(() -> displayRevenueData());
        });
    }

    private void displayRevenueData() {
        if (revenue == null) return;

        // Set quality icon based on grade
        switch (revenue.getQuality()) {
            case PREMIUM:
                tvQualityIcon.setImageResource(R.drawable.ic_star);
                tvQualityIcon.setColorFilter(getColor(R.color.accent));
                break;
            case STANDARD:
                tvQualityIcon.setImageResource(R.drawable.ic_check_circle);
                tvQualityIcon.setColorFilter(getColor(R.color.primary));
                break;
            case PROCESSING:
                tvQualityIcon.setImageResource(R.drawable.ic_construction);
                tvQualityIcon.setColorFilter(getColor(R.color.navy));
                break;
            default:
                tvQualityIcon.setImageResource(R.drawable.ic_star);
                tvQualityIcon.setColorFilter(getColor(R.color.accent));
                break;
        }

        tvQualityGrade.setText(revenue.getQuality().getDisplayName() + " Grade");

        if (revenue.getBlockId() != null) {
            String blockName = getBlockName(revenue.getBlockId());
            tvSource.setText("Block " + blockName);
        } else {
            tvSource.setText("Processing Center");
        }

        tvQuantity.setText(String.format(Locale.getDefault(), "%.1f kg", revenue.getQuantityKg()));
        tvPricePerKg.setText(String.format(Locale.getDefault(), "%,.0f XAF", revenue.getPricePerKg()));
        tvTotalAmount.setText(String.format(Locale.getDefault(), "%,.0f XAF", revenue.getTotalAmount()));
        tvHarvestDate.setText(dateFormat.format(revenue.getHarvestDate()));

        if (revenue.getBuyer() != null && !revenue.getBuyer().isEmpty()) {
            tvBuyer.setText(revenue.getBuyer());
        } else {
            tvBuyer.setText("No buyer recorded");
        }

        if (revenue.getNotes() != null && !revenue.getNotes().isEmpty()) {
            tvNotes.setText(revenue.getNotes());
            notesLayout.setVisibility(View.VISIBLE);
        } else {
            notesLayout.setVisibility(View.GONE);
        }
    }

    private String getBlockName(Long blockId) {
        if (blockId == null) return null;
        for (BlockEntity block : allBlocks) {
            if (block.getId() == blockId) {
                return block.getBlockName();
            }
        }
        return null;
    }

    private void openEditDialog() {
        if (revenue == null || currentFarm == null) return;

        AddRevenueDialog dialog =
                AddRevenueDialog.newEditInstance(currentFarm.getId(), revenue.getId());

        dialog.setOnRevenueAddedListener(() -> {
            // Reload data after editing
            loadData();
        });

        dialog.show(getSupportFragmentManager(), "EditRevenueDialog");
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Revenue")
                .setMessage("Are you sure you want to delete this revenue record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRevenue())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRevenue() {
        Toast.makeText(this, "Deleting revenue...", Toast.LENGTH_SHORT).show();

        databaseExecutor.execute(() -> {
            database.revenueDao().delete(revenue);

            // Note: Firestore deletion will be handled by the sync queue
            // The revenue will be removed from Firestore by your existing sync mechanism

            runOnUiThread(() -> {
                Toast.makeText(this, "Revenue deleted", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (revenueListener != null) {
            revenueListener.remove();
        }
        databaseExecutor.shutdown();
    }
}