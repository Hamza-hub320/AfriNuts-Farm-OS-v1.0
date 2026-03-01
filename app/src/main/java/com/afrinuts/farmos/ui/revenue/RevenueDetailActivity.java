package com.afrinuts.farmos.ui.revenue;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RevenueDetailActivity extends AppCompatActivity {

    public static final String EXTRA_REVENUE_ID = "revenue_id";

    private long revenueId;
    private RevenueEntity revenue;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;

    private TextView tvQualityIcon;
    private TextView tvQualityGrade;
    private TextView tvSource;
    private TextView tvQuantity;
    private TextView tvPricePerKg;
    private TextView tvTotalAmount;
    private TextView tvHarvestDate;
    private TextView tvBuyer;
    private TextView tvNotes;
    private Button btnDelete;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_detail);

        // Get revenue ID from intent
        revenueId = getIntent().getLongExtra(EXTRA_REVENUE_ID, -1);
        if (revenueId == -1) {
            Toast.makeText(this, "Revenue not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Load data
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
        btnDelete = findViewById(R.id.btnDelete);

        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Revenue Details");
        }
    }

    private void loadData() {
        new Thread(() -> {
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
        }).start();
    }

    private void displayRevenueData() {
        tvQualityIcon.setText(revenue.getQuality().getIcon());
        tvQualityGrade.setText(revenue.getQuality().getDisplayName() + " Grade");

        if (revenue.getBlockId() != null) {
            String blockName = getBlockName(revenue.getBlockId());
            tvSource.setText("Block " + blockName);
        } else {
            tvSource.setText("Processing Center");
        }

        tvQuantity.setText(String.format(Locale.getDefault(), "%.1f kg", revenue.getQuantityKg()));
        tvPricePerKg.setText(String.format(Locale.getDefault(), "%,.0f XAF/kg", revenue.getPricePerKg()));
        tvTotalAmount.setText(String.format(Locale.getDefault(), "%,.0f XAF", revenue.getTotalAmount()));
        tvHarvestDate.setText(dateFormat.format(revenue.getHarvestDate()));

        if (revenue.getBuyer() != null && !revenue.getBuyer().isEmpty()) {
            tvBuyer.setText(revenue.getBuyer());
        } else {
            tvBuyer.setText("No buyer recorded");
        }

        if (revenue.getNotes() != null && !revenue.getNotes().isEmpty()) {
            tvNotes.setText(revenue.getNotes());
            tvNotes.setVisibility(View.VISIBLE);
        } else {
            tvNotes.setVisibility(View.GONE);
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

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Revenue")
                .setMessage("Are you sure you want to delete this revenue record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRevenue())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRevenue() {
        new Thread(() -> {
            database.revenueDao().delete(revenue);
            runOnUiThread(() -> {
                Toast.makeText(this, "Revenue deleted", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}