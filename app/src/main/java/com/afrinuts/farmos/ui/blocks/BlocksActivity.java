package com.afrinuts.farmos.ui.blocks;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlocksActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExpandableBlockAdapter expandableAdapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private LinearLayout summaryLayout;  // Now this is the container inside CardView
    private List<BlockGroup> blockGroups = new ArrayList<>();

    private AppDatabase database;
    private FarmEntity currentFarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocks);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Load data
        loadFarmData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        summaryLayout = findViewById(R.id.summaryLayout);  // This is the container inside CardView

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Floating action button
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> {
            if (currentFarm != null) {
                String nextBlock = getNextAvailableBlockName();
                if (nextBlock.equals("FULL")) {
                    android.widget.Toast.makeText(this,
                            "All 35 blocks (A1-E7) have been created. Cannot add more.",
                            android.widget.Toast.LENGTH_LONG).show();
                    return;
                }

                BlockDialog dialog = BlockDialog.newInstance(currentFarm.getId());
                dialog.setNextBlockName(nextBlock);
                dialog.setOnBlockAddedListener(() -> {
                    loadFarmData();
                });
                dialog.show(getSupportFragmentManager(), "BlockDialog");
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Farm Blocks");
        }
    }

    private void loadFarmData() {
        showLoading(true);

        new Thread(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                List<BlockEntity> allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());

                // Group blocks by row
                Map<String, List<BlockEntity>> groupedBlocks = new HashMap<>();
                for (BlockEntity block : allBlocks) {
                    String row = block.getBlockName().substring(0, 1);
                    if (!groupedBlocks.containsKey(row)) {
                        groupedBlocks.put(row, new ArrayList<>());
                    }
                    groupedBlocks.get(row).add(block);
                }

                // Create BlockGroups in order (A, B, C, D, E)
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

                // Update UI on main thread
                runOnUiThread(() -> {
                    showLoading(false);

                    if (newGroups.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        blockGroups = newGroups;
                        updateFarmSummary(allBlocks);

                        expandableAdapter = new ExpandableBlockAdapter(blockGroups, block -> {
                            openBlockDetail(block);
                        });
                        recyclerView.setAdapter(expandableAdapter);
                    }
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (emptyView != null) {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
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

    private void openBlockDetail(BlockEntity block) {
        android.content.Intent intent = new android.content.Intent(this, BlockDetailActivity.class);
        intent.putExtra(BlockDetailActivity.EXTRA_BLOCK_ID, block.getId());
        startActivity(intent);
    }

    private String getNextAvailableBlockName() {
        char[] rows = {'A', 'B', 'C', 'D', 'E'};
        int columns = 7;

        List<BlockEntity> allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
        java.util.HashSet<String> existingNames = new java.util.HashSet<>();
        for (BlockEntity block : allBlocks) {
            existingNames.add(block.getBlockName());
        }

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
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (summaryLayout != null) {
            summaryLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show) {
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (summaryLayout != null) {
            summaryLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}