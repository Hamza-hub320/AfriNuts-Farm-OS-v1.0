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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlocksActivity extends AppCompatActivity {

    private static final String TAG = "BlocksActivity";

    private RecyclerView recyclerView;
    private BlockAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private TextView summaryText;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<BlockEntity> blocks;

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
                BlockDialog dialog = BlockDialog.newInstance(currentFarm.getId());
                dialog.setOnBlockAddedListener(() -> {
                    // Refresh the blocks list
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

        // Run database operations on background thread
        new Thread(() -> {
            // Get the first farm (we only have one in v1)
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                // Get all blocks for this farm
                blocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());

                // Calculate summary stats
                int totalBlocks = blocks.size();
                int plantedBlocks = 0;
                int totalAliveTrees = 0;
                int totalReplacements = 0;

                for (BlockEntity block : blocks) {
                    if (block.getStatus() == BlockEntity.BlockStatus.PLANTED) {
                        plantedBlocks++;
                    }
                    totalAliveTrees += block.getAliveTrees();
                    totalReplacements += block.getReplacementCount();
                }

                int totalExpectedTrees = totalBlocks * 100;
                double overallSurvivalRate = totalExpectedTrees > 0 ?
                        (totalAliveTrees * 100.0 / totalExpectedTrees) : 0;

                String summary = String.format(Locale.getDefault(),
                        "Summary: %d blocks | Planted: %d | Alive: %d/%d (%.1f%%) | Replaced: %d",
                        totalBlocks, plantedBlocks, totalAliveTrees, totalExpectedTrees,
                        overallSurvivalRate, totalReplacements);

                int finalTotalBlocks = totalBlocks;
                String finalSummary = summary;

                // Update UI on main thread
                runOnUiThread(() -> {
                    showLoading(false);

                    if (finalTotalBlocks == 0) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        summaryText.setText(finalSummary);
                        summaryText.setVisibility(View.VISIBLE);

                        // Setup adapter
                        adapter = new BlockAdapter(blocks, block -> {
                            // Handle block click - open detail view
                            openBlockDetail(block);
                        });
                        recyclerView.setAdapter(adapter);
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

    private void openBlockDetail(BlockEntity block) {
        // TODO: Start BlockDetailActivity
        Snackbar.make(recyclerView,
                "Opening block " + block.getBlockName() + " - Coming soon",
                Snackbar.LENGTH_LONG).show();

        Log.d(TAG, "Block clicked: " + block.toString());
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
}