package com.afrinuts.farmos;
import com.afrinuts.farmos.ui.expenses.AddExpenseDialog;
import com.afrinuts.farmos.ui.expenses.ExpensesListActivity;
import com.afrinuts.farmos.ui.expenses.ExpenseChartsActivity;
import com.afrinuts.farmos.ui.revenue.AddRevenueDialog;
import com.afrinuts.farmos.ui.revenue.RevenuesListActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.ui.blocks.BlocksActivity;
import com.afrinuts.farmos.ui.blocks.BlockDialog;
import com.afrinuts.farmos.ui.expenses.AddExpenseDialog;
import com.afrinuts.farmos.utils.DataSeeder;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AfriNutsFarmOS";

    // UI Elements
    private TextView farmNameText;
    private TextView farmLocationText;
    private TextView plantingYearText;
    private TextView totalHectaresText;
    private TextView totalTreesText;
    private TextView blocksCountText;
    private TextView plantedBlocksText;
    private TextView survivalRateText;
    private ProgressBar plantedProgress;

    private CardView btnViewBlocksCard;
    private CardView btnAddBlockCard;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private CardView btnAddExpenseCard;
    private CardView btnViewExpensesCard;
    private CardView btnExpenseChartsCard;
    private CardView btnAddRevenueCard;
    private CardView btnViewRevenueCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        initViews();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // FIRST: Ensure farm exists
        ensureFarmExists();

        // THEN: Load farm data
        loadFarmData();

        // Setup click listeners
        setupClickListeners();
    }

    private void ensureFarmExists() {
        // Check if farm already exists
        FarmEntity existingFarm = database.farmDao().getFirstFarm();

        if (existingFarm == null) {
            Log.d(TAG, "No farm found. Creating default farm...");

            // Create Odienné farm
            FarmEntity odienneFarm = new FarmEntity(
                    "AfriNuts Odienné",
                    "Odienné, Côte d'Ivoire",
                    35.0,  // total hectares
                    35.0,  // cashew hectares
                    100,   // trees per hectare
                    2024   // planting year
            );

            long id = database.farmDao().insert(odienneFarm);
            Log.d(TAG, "Created farm with ID: " + id);

            // Seed initial expenses after farm is created
            DataSeeder.seedInitialExpenses(this);
        } else {
            Log.d(TAG, "Farm already exists with ID: " + existingFarm.getId());
        }
    }

    private void initViews() {
        farmNameText = findViewById(R.id.farmNameText);
        farmLocationText = findViewById(R.id.farmLocationText);
        plantingYearText = findViewById(R.id.plantingYearText);
        totalHectaresText = findViewById(R.id.totalHectaresText);
        totalTreesText = findViewById(R.id.totalTreesText);
        blocksCountText = findViewById(R.id.blocksCountText);
        plantedBlocksText = findViewById(R.id.plantedBlocksText);
        survivalRateText = findViewById(R.id.survivalRateText);
        plantedProgress = findViewById(R.id.plantedProgress);

        btnViewBlocksCard = findViewById(R.id.btnViewBlocksCard);
        btnAddBlockCard = findViewById(R.id.btnAddBlockCard);
        btnAddExpenseCard = findViewById(R.id.btnAddExpenseCard);
        btnViewExpensesCard = findViewById(R.id.btnViewExpensesCard);
        btnExpenseChartsCard = findViewById(R.id.btnExpenseChartsCard);
        btnAddRevenueCard = findViewById(R.id.btnAddRevenueCard);
        btnViewRevenueCard = findViewById(R.id.btnViewRevenueCard);
    }

    private void setupClickListeners() {
        btnViewBlocksCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                Intent intent = new Intent(MainActivity.this, BlocksActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Loading farm data... Please try again", Toast.LENGTH_SHORT).show();
                loadFarmData(); // Retry loading
            }
        });

        btnAddBlockCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                // Get next available block name
                String nextBlock = getNextAvailableBlockName();

                if (nextBlock.equals("FULL")) {
                    Toast.makeText(this,
                            "All 35 blocks (A1-E7) have been created. Cannot add more.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                BlockDialog dialog = BlockDialog.newInstance(currentFarm.getId());
                dialog.setNextBlockName(nextBlock);
                dialog.setOnBlockAddedListener(() -> {
                    loadFarmData(); // Refresh data
                    Toast.makeText(this, "Block " + nextBlock + " added successfully!",
                            Toast.LENGTH_SHORT).show();
                });
                dialog.show(getSupportFragmentManager(), "BlockDialog");
            } else {
                Toast.makeText(this, "Farm not configured. Please restart app.", Toast.LENGTH_LONG).show();
            }
        });
        btnAddExpenseCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                AddExpenseDialog dialog = AddExpenseDialog.newInstance(currentFarm.getId());
                dialog.setOnExpenseAddedListener(() -> {
                    Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show();
                    // Optionally refresh some summary
                });
                dialog.show(getSupportFragmentManager(), "AddExpenseDialog");
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewExpensesCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                Intent intent = new Intent(MainActivity.this, ExpensesListActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });

        btnExpenseChartsCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                Intent intent = new Intent(MainActivity.this, ExpenseChartsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddRevenueCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                AddRevenueDialog dialog = AddRevenueDialog.newInstance(currentFarm.getId());
                dialog.setOnRevenueAddedListener(() -> {
                    Toast.makeText(this, "Revenue added!", Toast.LENGTH_SHORT).show();
                });
                dialog.show(getSupportFragmentManager(), "AddRevenueDialog");
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewRevenueCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                Intent intent = new Intent(MainActivity.this, RevenuesListActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getNextAvailableBlockName() {
        // 5 rows (A through E) and 7 columns (1 through 7)
        char[] rows = {'A', 'B', 'C', 'D', 'E'};
        int columns = 7;

        // Get existing block names
        java.util.HashSet<String> existingNames = new java.util.HashSet<>();
        for (BlockEntity block : allBlocks) {
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

        return "FULL";
    }

    private void loadFarmData() {
        new Thread(() -> {
            // Refresh currentFarm from database
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                // Get all blocks
                allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());

                // Calculate stats
                int totalBlocks = allBlocks.size();
                int plantedBlocks = 0;
                int totalAliveTrees = 0;
                double totalSurvivalRate = 0;

                for (BlockEntity block : allBlocks) {
                    if (block.isPlanted()) {
                        plantedBlocks++;
                        totalAliveTrees += block.getAliveTrees();
                        totalSurvivalRate += block.getSurvivalRate();
                    }
                }

                double avgSurvivalRate = plantedBlocks > 0 ?
                        totalSurvivalRate / plantedBlocks : 0;
                int plantedProgressValue = totalBlocks > 0 ?
                        (plantedBlocks * 100 / totalBlocks) : 0;

                // Update UI on main thread
                final int finalTotalBlocks = totalBlocks;
                final int finalPlantedBlocks = plantedBlocks;
                final int finalTotalAliveTrees = totalAliveTrees;
                final double finalAvgSurvivalRate = avgSurvivalRate;
                final int finalPlantedProgress = plantedProgressValue;

                runOnUiThread(() -> updateUI(
                        finalTotalBlocks,
                        finalPlantedBlocks,
                        finalTotalAliveTrees,
                        finalAvgSurvivalRate,
                        finalPlantedProgress
                ));
            }
        }).start();
    }

    private void updateUI(int totalBlocks, int plantedBlocks, int totalAliveTrees,
                          double avgSurvivalRate, int plantedProgressValue) {

        // Farm details
        if (currentFarm != null) {
            farmNameText.setText(currentFarm.getName());
            farmLocationText.setText(currentFarm.getLocation());
            plantingYearText.setText(String.valueOf(currentFarm.getPlantingYear()));

            totalHectaresText.setText(String.format(Locale.getDefault(),
                    "%.0f", currentFarm.getTotalHectares()));

            int totalTrees = (int)(currentFarm.getCashewHectares() *
                    currentFarm.getTreesPerHectare());
            totalTreesText.setText(String.format(Locale.getDefault(),
                    "%,d", totalTrees));
        }

        // Blocks stats
        blocksCountText.setText(String.valueOf(totalBlocks));
        plantedBlocksText.setText(String.format(Locale.getDefault(),
                "%d/%d", plantedBlocks, totalBlocks));

        plantedProgress.setProgress(plantedProgressValue);

        survivalRateText.setText(String.format(Locale.getDefault(),
                "%.0f%%", avgSurvivalRate));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFarmData(); // Refresh data when returning to activity
    }
}