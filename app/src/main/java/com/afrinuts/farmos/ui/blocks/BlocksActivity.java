package com.afrinuts.farmos.ui.blocks;

import android.content.Intent;
import android.os.Bundle;
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
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlocksActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ExpandableBlockAdapter expandableAdapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private LinearLayout summaryLayout;
    private FloatingActionButton fabAdd;
    private List<BlockGroup> blockGroups = new ArrayList<>();

    private BlockRepository blockRepository;
    private FarmEntity currentFarm;
    private AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocks);

        // Initialize database
        database = AppDatabase.getInstance(this);

        initViews();
        setupToolbar();
        setupFabClickListener();

        // Load farm data (this will initialize repository)
        loadFarmData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning from detail activity
        loadFarmData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        summaryLayout = findViewById(R.id.summaryLayout);
        fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Farm Blocks");
        }
    }

    private void setupFabClickListener() {
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                if (currentFarm != null) {
                    // Show loading on FAB
                    fabAdd.setEnabled(false);

                    // Get next available block name in background
                    executor.execute(() -> {
                        String nextBlock = getNextAvailableBlockName();

                        runOnUiThread(() -> {
                            fabAdd.setEnabled(true);

                            if (nextBlock.equals("FULL")) {
                                Snackbar.make(v,
                                        "All 35 blocks (A1-E7) have been created. Cannot add more.",
                                        Snackbar.LENGTH_LONG).show();
                                return;
                            }

                            BlockDialog dialog = BlockDialog.newInstance(currentFarm.getId());
                            dialog.setNextBlockName(nextBlock);
                            dialog.setOnBlockAddedListener(() -> loadFarmData());
                            dialog.show(getSupportFragmentManager(), "BlockDialog");
                        });
                    });
                } else {
                    Toast.makeText(this, "Farm not configured. Please try again.",
                            Toast.LENGTH_LONG).show();
                    loadFarmData();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFarmData() {
        showLoading(true);

        executor.execute(() -> {
            // Get farm from database
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                long farmId = currentFarm.getId();

                // Initialize repository with correct farmId
                blockRepository = new BlockRepository(BlocksActivity.this, farmId);

                // Use LiveData from repository
                runOnUiThread(() -> {
                    blockRepository.getAllBlocks().observe(this, new Observer<List<BlockEntity>>() {
                        @Override
                        public void onChanged(List<BlockEntity> blocks) {
                            processBlocks(blocks);
                        }
                    });
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void processBlocks(List<BlockEntity> allBlocks) {
        if (allBlocks == null) allBlocks = new ArrayList<>();

        Map<String, List<BlockEntity>> groupedBlocks = new HashMap<>();
        for (BlockEntity block : allBlocks) {
            String row = block.getBlockName().substring(0, 1);
            if (!groupedBlocks.containsKey(row)) {
                groupedBlocks.put(row, new ArrayList<>());
            }
            groupedBlocks.get(row).add(block);
        }

        List<BlockGroup> newGroups = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E"};

        for (String row : rows) {
            if (groupedBlocks.containsKey(row)) {
                BlockGroup group = new BlockGroup(row);
                List<BlockEntity> blocksForRow = groupedBlocks.get(row);
                blocksForRow.sort((b1, b2) -> {
                    String num1 = b1.getBlockName().substring(1);
                    String num2 = b2.getBlockName().substring(1);
                    return Integer.compare(Integer.parseInt(num1), Integer.parseInt(num2));
                });
                group.setBlocks(blocksForRow);
                newGroups.add(group);
            }
        }

        final List<BlockEntity> finalAllBlocks = allBlocks;
        final List<BlockGroup> finalNewGroups = newGroups;

        runOnUiThread(() -> {
            if (finalNewGroups.isEmpty()) {
                showEmpty(true);
            } else {
                showEmpty(false);
                blockGroups = finalNewGroups;
                updateFarmSummary(finalAllBlocks);

                expandableAdapter = new ExpandableBlockAdapter(blockGroups, block -> {
                    Intent intent = new Intent(BlocksActivity.this, BlockDetailActivity.class);
                    intent.putExtra(BlockDetailActivity.EXTRA_BLOCK_ID, block.getId());
                    startActivity(intent);
                });
                recyclerView.setAdapter(expandableAdapter);
            }
            showLoading(false);
        });
    }

    private void updateFarmSummary(List<BlockEntity> allBlocks) {
        int totalBlocks = allBlocks.size();
        int plantedBlocks = 0;
        int totalAliveTrees = 0;
        int totalExpectedTrees = totalBlocks * 100;

        for (BlockEntity block : allBlocks) {
            if (block.isPlanted()) {
                plantedBlocks++;
                totalAliveTrees += block.getAliveTrees();
            }
        }

        if (summaryLayout == null) return;

        summaryLayout.removeAllViews();
        summaryLayout.setVisibility(View.VISIBLE);

        View summaryView = getLayoutInflater().inflate(R.layout.item_farm_summary, summaryLayout, false);

        TextView blocksPlantedText = summaryView.findViewById(R.id.blocksPlantedText);
        TextView treesAliveText = summaryView.findViewById(R.id.treesAliveText);
        ProgressBar blocksProgress = summaryView.findViewById(R.id.blocksProgress);
        ProgressBar treesProgress = summaryView.findViewById(R.id.treesProgress);
        TextView blocksPercentText = summaryView.findViewById(R.id.blocksPercentText);
        TextView treesPercentText = summaryView.findViewById(R.id.treesPercentText);

        int blocksPercent = totalBlocks > 0 ? (plantedBlocks * 100 / totalBlocks) : 0;
        int treesPercent = totalExpectedTrees > 0 ? (totalAliveTrees * 100 / totalExpectedTrees) : 0;

        blocksPlantedText.setText(String.format(Locale.getDefault(),
                "%d/%d blocks planted", plantedBlocks, totalBlocks));
        treesAliveText.setText(String.format(Locale.getDefault(),
                "%d/%d trees alive", totalAliveTrees, totalExpectedTrees));

        blocksProgress.setProgress(blocksPercent);
        treesProgress.setProgress(treesPercent);

        blocksPercentText.setText(blocksPercent + "%");
        treesPercentText.setText(treesPercent + "%");

        summaryLayout.addView(summaryView);
    }

    private String getNextAvailableBlockName() {
        if (currentFarm == null || blockRepository == null) return "A1";

        // Use repository to get blocks - this runs on background thread (executor)
        List<BlockEntity> allBlocks = blockRepository.getBlocksSync();
        java.util.HashSet<String> existingNames = new java.util.HashSet<>();
        for (BlockEntity block : allBlocks) {
            existingNames.add(block.getBlockName());
        }

        char[] rows = {'A', 'B', 'C', 'D', 'E'};
        int columns = 7;

        for (char row : rows) {
            for (int col = 1; col <= columns; col++) {
                String blockName = row + String.valueOf(col);
                if (!existingNames.contains(blockName)) {
                    return blockName;
                }
            }
        }
        return "FULL";
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            if (summaryLayout != null) summaryLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        });
    }

    private void showEmpty(boolean show) {
        if (emptyView != null) emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (summaryLayout != null) summaryLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (blockRepository != null) {
            blockRepository.shutdown();
        }
        executor.shutdown();
    }
}

