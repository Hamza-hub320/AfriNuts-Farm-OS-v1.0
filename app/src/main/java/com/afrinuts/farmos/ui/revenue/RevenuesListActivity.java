package com.afrinuts.farmos.ui.revenue;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.ui.BaseActivity;
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

public class RevenuesListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private RevenueAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView totalRevenueValue;
    private TextView totalQuantityValue;  // Changed from totalHarvestValue
    private ChipGroup filterChipGroup;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private RevenueRepository revenueRepository;
    private QualityBatchRepository batchRepository;
    private String farmIdString = "1";
    private List<RevenueWithBlockName> allRevenues = new ArrayList<>();
    private List<RevenueWithBlockName> filteredRevenues = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();
    private Map<Long, String> batchNumberMap = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String currentFilter = "ALL";

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenues_list);

        database = AppDatabase.getInstance(this);

        initViews();
        setupToolbar();
        setupFilters();
        setupFab();
        loadFarmAndBlocks();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyLayout = findViewById(R.id.emptyView);
        totalRevenueValue = findViewById(R.id.totalRevenueValue);
        totalQuantityValue = findViewById(R.id.totalQuantityValue);  // Updated ID
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
            getSupportActionBar().setTitle("Processing Revenue");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
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
        Chip chipPremium = findViewById(R.id.chipPremium);
        Chip chipStandard = findViewById(R.id.chipStandard);
        Chip chipProcessing = findViewById(R.id.chipProcessing);

        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
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

        chipProcessing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "PROCESSING";
                applyFilter();
            }
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddRevenue);

        if (sessionManager.canEditData()) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                if (currentFarm != null) {
                    AddRevenueDialog dialog = AddRevenueDialog.newInstance(currentFarm.getId());
                    dialog.setOnRevenueAddedListener(() -> {
                        loadFarmAndBlocks();
                    });
                    dialog.show(getSupportFragmentManager(), "AddRevenueDialog");
                } else {
                    Toast.makeText(this, "Loading farm data...", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    private void loadFarmAndBlocks() {
        showLoading(true);

        executor.execute(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                revenueRepository = new RevenueRepository(this, currentFarm.getId());
                batchRepository = new QualityBatchRepository(this, currentFarm.getId());
                farmIdString = String.valueOf(currentFarm.getId());

                // Load block names
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                Map<Long, String> blockMap = new HashMap<>();
                for (BlockEntity block : blocks) {
                    blockMap.put(block.getId(), block.getBlockName());
                }

                // Load batch numbers
                List<QualityBatchEntity> batches = batchRepository.getAllBatchesSync();
                Map<Long, String> batchMap = new HashMap<>();
                for (QualityBatchEntity batch : batches) {
                    batchMap.put(batch.getId(), batch.getBatchNumber());
                }

                final Map<Long, String> finalBlockMap = blockMap;
                final Map<Long, String> finalBatchMap = batchMap;
                final FarmEntity finalFarm = currentFarm;

                runOnUiThread(() -> {
                    currentFarm = finalFarm;
                    blockNameMap.clear();
                    blockNameMap.putAll(finalBlockMap);
                    batchNumberMap.clear();
                    batchNumberMap.putAll(finalBatchMap);

                    // Use LiveData from repository
                    revenueRepository.getAllRevenues().observe(this, new Observer<List<RevenueEntity>>() {
                        @Override
                        public void onChanged(List<RevenueEntity> revenues) {
                            processRevenues(revenues);
                        }
                    });
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true);
                });
            }
        });
    }

    private void processRevenues(List<RevenueEntity> revenues) {
        allRevenues.clear();
        for (RevenueEntity revenue : revenues) {
            String blockName = null;
            if (revenue.getBlockId() != null) {
                blockName = blockNameMap.get(revenue.getBlockId());
            }

            // Enrich with batch number if missing
            if (revenue.getBatchId() != null &&
                    (revenue.getBatchNumber() == null || revenue.getBatchNumber().isEmpty())) {
                String batchNumber = batchNumberMap.get(revenue.getBatchId());
                if (batchNumber != null) {
                    revenue.setBatchNumber(batchNumber);
                }
            }

            allRevenues.add(new RevenueWithBlockName(revenue, blockName));
        }

        showLoading(false);

        if (allRevenues.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            applyFilter();
            updateSummary();
        }
    }

    private void applyFilter() {
        filteredRevenues.clear();

        for (RevenueWithBlockName item : allRevenues) {
            RevenueEntity revenue = item.getRevenue();

            switch (currentFilter) {
                case "ALL":
                    filteredRevenues.add(item);
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
                case "PROCESSING":
                    if (revenue.getQuality() == RevenueEntity.QualityGrade.PROCESSING) {
                        filteredRevenues.add(item);
                    }
                    break;
            }
        }

        List<RevenueEntity> revenueEntities = new ArrayList<>();
        for (RevenueWithBlockName item : filteredRevenues) {
            revenueEntities.add(item.getRevenue());
        }

        adapter = new RevenueAdapter();
        adapter.setOnRevenueClickListener(new RevenueAdapter.OnRevenueClickListener() {
            @Override
            public void onRevenueClick(RevenueEntity revenue) {
                Intent intent = new Intent(RevenuesListActivity.this, RevenueDetailActivity.class);
                intent.putExtra(RevenueDetailActivity.EXTRA_REVENUE_ID, revenue.getId());
                startActivity(intent);
            }

            @Override
            public void onRevenueLongClick(RevenueEntity revenue) {
                // Handle long click if needed
            }
        });
        adapter.setRevenues(revenueEntities);
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
        totalQuantityValue.setText(String.format(Locale.getDefault(),
                "%,.1f kg", totalKg));
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
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void sortByAmount() {
        filteredRevenues.sort((e1, e2) ->
                Double.compare(e2.getRevenue().getTotalAmount(),
                        e1.getRevenue().getTotalAmount()));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void sortByQuantity() {
        filteredRevenues.sort((e1, e2) ->
                Double.compare(e2.getRevenue().getQuantityKg(),
                        e1.getRevenue().getQuantityKg()));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (revenueRepository != null) {
            revenueRepository.shutdown();
        }
        if (batchRepository != null) {
            batchRepository.shutdown();
        }
        executor.shutdown();
    }
}