package com.afrinuts.farmos.ui.revenue;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.ui.BaseSyncActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevenuesListActivity extends BaseSyncActivity {

    private RecyclerView recyclerView;
    private RevenueAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView totalRevenueValue;
    private TextView totalHarvestValue;
    private ChipGroup filterChipGroup;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<RevenueWithBlockName> allRevenues = new ArrayList<>();
    private List<RevenueWithBlockName> filteredRevenues = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private String currentFilter = "ALL"; // ALL, BLOCKS, PROCESSING, PREMIUM, STANDARD

    @Override
    protected boolean checkRoleAccess() {
        // Investors cannot view revenue data
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // This will trigger role check
        setContentView(R.layout.activity_revenues_list);

        // If we get here, user has permission (Owner or Manager)

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup filter listeners
        setupFilters();

        // Setup FAB (only show for users who can add data)
        setupFab();

        // Load data
        loadData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyLayout = findViewById(R.id.emptyView);
        totalRevenueValue = findViewById(R.id.totalRevenueValue);
        totalHarvestValue = findViewById(R.id.totalHarvestValue);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(
                    getResources().getColor(android.R.color.white)
            );
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Harvest Revenue");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            // Navigate to MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_sort_date) {
            sortByDate();
            return true;
        } else if (itemId == R.id.action_sort_amount) {
            sortByAmount();
            return true;
        } else if (itemId == R.id.action_sort_quantity) {
            sortByQuantity();
            return true;
        }

        return super.onOptionsItemSelected(item);
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

        // Only show FAB for users who can add data (Owners and Managers)
        if (sessionManager.canEditData()) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                if (currentFarm != null) {
                    AddRevenueDialog dialog = AddRevenueDialog.newInstance(currentFarm.getId());
                    dialog.setOnRevenueAddedListener(() -> {
                        loadData(); // Refresh list
                    });
                    dialog.show(getSupportFragmentManager(), "AddRevenueDialog");
                }
            });
        } else {
            // Investors cannot add revenue
            fab.setVisibility(View.GONE);
        }
    }

    private void loadData() {
        showLoading(true);

        databaseExecutor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();

            if (farm != null) {
                // Load all blocks for name mapping
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());
                Map<Long, String> blockMap = new HashMap<>();
                for (BlockEntity block : blocks) {
                    blockMap.put(block.getId(), block.getBlockName());
                }

                // Load all revenues
                List<RevenueEntity> revenues = database.revenueDao().getRevenuesByFarmId(farm.getId());

                List<RevenueWithBlockName> revenueList = new ArrayList<>();
                for (RevenueEntity revenue : revenues) {
                    String blockName = null;
                    if (revenue.getBlockId() != null) {
                        blockName = blockMap.get(revenue.getBlockId());
                    }
                    revenueList.add(new RevenueWithBlockName(revenue, blockName));
                }

                // Update UI on main thread
                final FarmEntity finalFarm = farm;
                final List<RevenueWithBlockName> finalRevenueList = revenueList;
                final Map<Long, String> finalBlockMap = blockMap;

                runOnUiThread(() -> {
                    currentFarm = finalFarm;
                    if (currentFarm != null) {
                        startListeningForRevenues();
                    }
                    allRevenues.clear();
                    allRevenues.addAll(finalRevenueList);
                    blockNameMap.clear();
                    blockNameMap.putAll(finalBlockMap);

                    showLoading(false);

                    if (allRevenues.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        applyFilter();
                        updateSummary();
                    }
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true);
                });
            }
        });
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
            // Navigate to RevenueDetailActivity
            Intent intent = new Intent(this, RevenueDetailActivity.class);
            intent.putExtra(RevenueDetailActivity.EXTRA_REVENUE_ID, revenue.getId());
            startActivity(intent);
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
        emptyLayout.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        emptyLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_revenues, menu);
        return true;
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

    @Override
    protected void handleRevenueAdded(RevenueEntity revenue) {
        runOnUiThread(() -> {
            databaseExecutor.execute(() -> {
                database.revenueDao().insert(revenue);
                loadData();
                showToast("New revenue recorded");
            });
        });
    }

    @Override
    protected void handleRevenueModified(RevenueEntity revenue) {
        runOnUiThread(() -> {
            databaseExecutor.execute(() -> {
                database.revenueDao().update(revenue);
                loadData();
            });
        });
    }

    @Override
    protected void handleRevenueRemoved(RevenueEntity revenue) {
        runOnUiThread(() -> {
            databaseExecutor.execute(() -> {
                database.revenueDao().delete(revenue);
                loadData();
            });
        });
    }

    // Empty implementations for other BaseSyncActivity handlers
    @Override protected void handleBlockAdded(BlockEntity b) {}
    @Override protected void handleBlockModified(BlockEntity b) {}
    @Override protected void handleBlockRemoved(BlockEntity b) {}
    @Override protected void handleExpenseAdded(com.afrinuts.farmos.data.local.entity.ExpenseEntity e) {}
    @Override protected void handleExpenseModified(com.afrinuts.farmos.data.local.entity.ExpenseEntity e) {}
    @Override protected void handleExpenseRemoved(com.afrinuts.farmos.data.local.entity.ExpenseEntity e) {}
    @Override protected void handleTaskAdded(com.afrinuts.farmos.data.local.entity.TaskEntity t) {}
    @Override protected void handleTaskModified(com.afrinuts.farmos.data.local.entity.TaskEntity t) {}
    @Override protected void handleTaskRemoved(com.afrinuts.farmos.data.local.entity.TaskEntity t) {}

    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}