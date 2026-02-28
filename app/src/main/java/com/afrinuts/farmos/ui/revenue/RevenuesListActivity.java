package com.afrinuts.farmos.ui.revenue;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RevenuesListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RevenueAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView totalRevenueValue;
    private TextView totalHarvestValue;
    private ChipGroup filterChipGroup;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<RevenueWithBlockName> allRevenues = new ArrayList<>();
    private List<RevenueWithBlockName> filteredRevenues = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();

    private String currentFilter = "ALL"; // ALL, BLOCKS, PROCESSING, PREMIUM, STANDARD

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenues_list);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Setup filter listeners
        setupFilters();

        // Setup FAB
        setupFab();

        // Load data
        loadData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        totalRevenueValue = findViewById(R.id.totalRevenueValue);
        totalHarvestValue = findViewById(R.id.totalHarvestValue);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Harvest Revenue");
        }
    }

    private void setupFilters() {
        Chip chipAll = findViewById(R.id.chipAll);
        Chip chipBlocks = findViewById(R.id.chipBlocks);
        Chip chipProcessing = findViewById(R.id.chipProcessing);
        Chip chipPremium = findViewById(R.id.chipPremium);
        Chip chipStandard = findViewById(R.id.chipStandard);

        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                applyFilter();
            }
        });

        chipBlocks.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "BLOCKS";
                applyFilter();
            }
        });

        chipProcessing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "PROCESSING";
                applyFilter();
            }
        });

        chipPremium.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "PREMIUM";
                applyFilter();
            }
        });

        chipStandard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "STANDARD";
                applyFilter();
            }
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddRevenue);
        fab.setOnClickListener(v -> {
            if (currentFarm != null) {
                AddRevenueDialog dialog = AddRevenueDialog.newInstance(currentFarm.getId());
                dialog.setOnRevenueAddedListener(() -> {
                    loadData(); // Refresh list
                });
                dialog.show(getSupportFragmentManager(), "AddRevenueDialog");
            }
        });
    }

    private void loadData() {
        showLoading(true);

        new Thread(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                // Load all blocks for name mapping
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                blockNameMap.clear();
                for (BlockEntity block : blocks) {
                    blockNameMap.put(block.getId(), block.getBlockName());
                }

                // Load all revenues
                List<RevenueEntity> revenues = database.revenueDao().getRevenuesByFarmId(currentFarm.getId());

                allRevenues.clear();
                for (RevenueEntity revenue : revenues) {
                    String blockName = null;
                    if (revenue.getBlockId() != null) {
                        blockName = blockNameMap.get(revenue.getBlockId());
                    }
                    allRevenues.add(new RevenueWithBlockName(revenue, blockName));
                }

                runOnUiThread(() -> {
                    showLoading(false);

                    if (allRevenues.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        applyFilter();
                        updateSummary();
                    }
                });
            }
        }).start();
    }

    private void applyFilter() {
        filteredRevenues.clear();

        for (RevenueWithBlockName item : allRevenues) {
            RevenueEntity revenue = item.getRevenue();

            switch (currentFilter) {
                case "ALL":
                    filteredRevenues.add(item);
                    break;
                case "BLOCKS":
                    if (item.isFromBlock()) {
                        filteredRevenues.add(item);
                    }
                    break;
                case "PROCESSING":
                    if (item.isFromProcessingCenter()) {
                        filteredRevenues.add(item);
                    }
                    break;
                case "PREMIUM":
                    if (revenue.getQuality() == RevenueEntity.QualityGrade.PREMIUM) {
                        filteredRevenues.add(item);
                    }
                    break;
                case "STANDARD":
                    if (revenue.getQuality() == RevenueEntity.QualityGrade.STANDARD) {
                        filteredRevenues.add(item);
                    }
                    break;
            }
        }

        // Update adapter
        adapter = new RevenueAdapter(filteredRevenues, revenue -> {
            // TODO: Open RevenueDetailActivity
            android.widget.Toast.makeText(this,
                    "Revenue detail coming soon",
                    android.widget.Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);
        updateSummary();
    }

    private void updateSummary() {
        double totalRevenue = 0;
        double totalKg = 0;

        for (RevenueWithBlockName item : filteredRevenues) {
            RevenueEntity revenue = item.getRevenue();
            totalRevenue += revenue.getTotalAmount();
            totalKg += revenue.getQuantityKg();
        }

        totalRevenueValue.setText(String.format(Locale.getDefault(),
                "%,.0f XAF", totalRevenue));
        totalHarvestValue.setText(String.format(Locale.getDefault(),
                "%.1f kg", totalKg));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_revenues, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_sort_date) {
            sortByDate();
            return true;
        } else if (item.getItemId() == R.id.action_sort_amount) {
            sortByAmount();
            return true;
        } else if (item.getItemId() == R.id.action_sort_quantity) {
            sortByQuantity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sortByDate() {
        filteredRevenues.sort((e1, e2) ->
                Long.compare(e2.getRevenue().getHarvestDate(),
                        e1.getRevenue().getHarvestDate()));
        adapter.notifyDataSetChanged();
    }

    private void sortByAmount() {
        filteredRevenues.sort((e1, e2) ->
                Double.compare(e2.getRevenue().getTotalAmount(),
                        e1.getRevenue().getTotalAmount()));
        adapter.notifyDataSetChanged();
    }

    private void sortByQuantity() {
        filteredRevenues.sort((e1, e2) ->
                Double.compare(e2.getRevenue().getQuantityKg(),
                        e1.getRevenue().getQuantityKg()));
        adapter.notifyDataSetChanged();
    }
}