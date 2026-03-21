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
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.ui.BaseActivity;
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
    private RevenueRepository revenueRepository;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_detail);

        if (!checkRoleAccess()) {
            Toast.makeText(this, "You don't have permission to view revenue details",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        revenueId = getIntent().getLongExtra(EXTRA_REVENUE_ID, -1);
        if (revenueId == -1) {
            Toast.makeText(this, "Revenue not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        revenueRepository = new RevenueRepository(this, farmId);

        initViews();
        setupToolbar();
        setupRoleBasedUI();
        loadData();
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

    private void setupRoleBasedUI() {
        if (sessionManager.canEditData()) {
            if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        } else {
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        }
    }

    private void loadData() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Loading revenue with ID: " + revenueId);

                // First, check if database is accessible
                if (database == null) {
                    Log.e(TAG, "Database is null");
                    runOnUiThread(() -> {
                        Toast.makeText(RevenueDetailActivity.this,
                                "Database error", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                revenue = database.revenueDao().getRevenueById(revenueId);

                if (revenue == null) {
                    Log.e(TAG, "Revenue not found in database with ID: " + revenueId);
                    runOnUiThread(() -> {
                        Toast.makeText(RevenueDetailActivity.this,
                                "Revenue not found", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                Log.d(TAG, "Revenue loaded: " + revenue.getId() +
                        ", farmId: " + revenue.getFarmId() +
                        ", quality: " + revenue.getQuality());

                // Update repository with correct farmId
                farmId = revenue.getFarmId();
                revenueRepository = new RevenueRepository(RevenueDetailActivity.this, farmId);

                // Load farm data
                currentFarm = database.farmDao().getFirstFarm();
                if (currentFarm == null) {
                    Log.e(TAG, "No farm found in database");
                } else {
                    Log.d(TAG, "Current farm ID: " + currentFarm.getId());
                }

                // Load blocks for block name lookup
                allBlocks = database.blockDao().getBlocksByFarmId(currentFarm != null ?
                        currentFarm.getId() : 1L);
                Log.d(TAG, "Loaded " + (allBlocks != null ? allBlocks.size() : 0) + " blocks");

                runOnUiThread(() -> {
                    try {
                        displayRevenueData();
                    } catch (Exception e) {
                        Log.e(TAG, "Error displaying revenue data", e);
                        Toast.makeText(RevenueDetailActivity.this,
                                "Error displaying revenue: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Critical error in loadData", e);
                runOnUiThread(() -> {
                    Toast.makeText(RevenueDetailActivity.this,
                            "Error loading revenue: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void displayRevenueData() {
        if (revenue == null) return;

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

        revenueRepository.deleteRevenue(revenue, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(RevenueDetailActivity.this,
                            "Revenue deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(RevenueDetailActivity.this,
                                "Error deleting revenue: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (revenueRepository != null) {
            revenueRepository.shutdown();
        }
        executor.shutdown();
    }
}