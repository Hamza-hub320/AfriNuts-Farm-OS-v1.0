package com.afrinuts.farmos.ui.blocks;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlocksActivity extends AppCompatActivity {

    private static final String TAG = "BlocksActivity";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private TextView summaryText;

    private AppDatabase database;
    private FarmEntity currentFarm;

    // New fields for expandable adapter
    private ExpandableBlockAdapter expandableAdapter;
    private List<BlockGroup> blockGroups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocks);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Get database instance
        database = AppDatabase.getInstance(this);

        // Load data
        loadFarmData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        summaryText = findViewById(R.id.summaryText);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Floating action button for adding new blocks
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> {
            if (currentFarm != null) {
                String nextBlock = getNextAvailableBlockName();
                if (nextBlock.equals("FULL")) {
                    Snackbar.make(v,
                            "All 35 blocks (A1-E7) have been created. Cannot add more.",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                BlockDialog dialog = BlockDialog.newInstance(currentFarm.getId());
                dialog.setNextBlockName(nextBlock);
                dialog.setOnBlockAddedListener(() -> {
                    loadFarmData();
                });
                dialog.show(getSupportFragmentManager(), "BlockDialog");
            } else {
                Snackbar.make(v, "Farm not configured", Snackbar.LENGTH_LONG).show();
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
            // Get the first farm (we only have one in v1)
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                // Get all blocks for this farm
                List<BlockEntity> allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());

                // Group blocks by row (first character of block name)
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
                        // Sort blocks by number (A1, A2, A3, etc.)
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
                List<BlockGroup> finalNewGroups = newGroups;
                runOnUiThread(() -> {
                    showLoading(false);

                    if (finalNewGroups.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        blockGroups = finalNewGroups;

                        // Calculate farm summary
                        updateFarmSummary(allBlocks);

                        // Setup expandable adapter
                        expandableAdapter = new ExpandableBlockAdapter(blockGroups, block -> {
                            openBlockDetail(block);
                        });
                        recyclerView.setAdapter(expandableAdapter);
                    }
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    emptyView.setText("No farm configured. Please check database.");
                    emptyView.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private String getNextAvailableBlockName() {
        // 5 rows (A through E) and 7 columns (1 through 7)
        char[] rows = {'A', 'B', 'C', 'D', 'E'};
        int columns = 7;

        if (currentFarm == null) return "A1";

        // Get existing block names
        List<BlockEntity> existingBlocks =
                database.blockDao().getBlocksByFarmId(currentFarm.getId());

        java.util.HashSet<String> existingNames = new java.util.HashSet<>();
        for (BlockEntity block : existingBlocks) {
            existingNames.add(block.getBlockName());
        }

        // Find first available name in the grid
        for (char row : rows) {
            for (int col = 1; col <= columns; col++) {
                String blockName = row + String.valueOf(col);
                if (!existingNames.contains(blockName)) {
                    return blockName;
                }
            }
        }

        // All 35 blocks are already created
        return "FULL";
    }

    private void updateFarmSummary(List<BlockEntity> allBlocks) {
        int totalBlocks = allBlocks.size();
        int plantedBlocks = 0;
        int totalAliveTrees = 0;

        for (BlockEntity block : allBlocks) {
            if (block.isPlanted()) {
                plantedBlocks++;
                totalAliveTrees += block.getAliveTrees();
            }
        }

        String summary = String.format(Locale.getDefault(),
                "📊 Farm Summary: %d/%d blocks planted | 🌳 %d/%d trees alive",
                plantedBlocks, totalBlocks, totalAliveTrees, totalBlocks * 100);

        summaryText.setText(summary);
        summaryText.setVisibility(View.VISIBLE);
    }

    private void openBlockDetail(BlockEntity block) {
        // Start BlockDetailActivity
        android.content.Intent intent = new android.content.Intent(this, BlockDetailActivity.class);
        intent.putExtra(BlockDetailActivity.EXTRA_BLOCK_ID, block.getId());
        startActivity(intent);

        Log.d(TAG, "Opening block: " + block.getBlockName());
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmpty(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        summaryText.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning to this activity
        loadFarmData();
    }
}