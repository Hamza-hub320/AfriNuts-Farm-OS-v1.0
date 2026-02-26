package com.afrinuts.farmos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.ui.blocks.BlocksActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AfriNutsFarmOS";
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnViewBlocks = findViewById(R.id.btnViewBlocks);
        btnViewBlocks.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BlocksActivity.class);
            startActivity(intent);
        });

        textView = findViewById(R.id.textView);

        // Test database
        testDatabase();

        // Test blocks
        AppDatabase db = AppDatabase.getInstance(this);
        testBlocks(db);
    }

    private void testBlocks(AppDatabase db) {
        FarmEntity farm = db.farmDao().getFirstFarm();
        if (farm != null) {
            // Check if blocks already exist
            List<BlockEntity> existingBlocks = db.blockDao().getBlocksByFarmId(farm.getId());

            if (existingBlocks.isEmpty()) {
                // Create sample blocks for demonstration

                BlockEntity block1 = new BlockEntity(
                        farm.getId(),
                        "A1",
                        1.0,
                        BlockEntity.BlockStatus.PLANTED.name()
                );

                BlockEntity block2 = new BlockEntity(
                        farm.getId(),
                        "A2",
                        1.0,
                        BlockEntity.BlockStatus.PLANTED.name()
                );

                BlockEntity block3 = new BlockEntity(
                        farm.getId(),
                        "B1",
                        1.0,
                        BlockEntity.BlockStatus.CLEARED.name()
                );

                db.blockDao().insert(block1);
                db.blockDao().insert(block2);
                db.blockDao().insert(block3);

                Log.d(TAG, "Added 3 sample blocks");
            }

            // Log block count
            int blockCount = db.blockDao().getBlockCount(farm.getId());
            Log.d(TAG, "Total blocks: " + blockCount);
        }
    }

    private void testDatabase() {
        // Get database instance
        AppDatabase db = AppDatabase.getInstance(this);

        // Check if farm already exists
        FarmEntity existingFarm = db.farmDao().getFirstFarm();

        if (existingFarm == null) {
            // Create your Odienné farm
            FarmEntity odienneFarm = new FarmEntity(
                    "AfriNuts Odienné",
                    "Odienné, Côte d'Ivoire",
                    35.0,  // total hectares
                    35.0,  // cashew hectares (all cashew)
                    100,   // trees per hectare
                    2024   // planting year
            );

            // Insert and get ID
            long id = db.farmDao().insert(odienneFarm);
            Log.d(TAG, "Inserted farm with ID: " + id);

            // Retrieve the farm with generated ID
            existingFarm = db.farmDao().getFarmById(id);
        }

        // Display farm info
        if (existingFarm != null) {
            String farmInfo = String.format(Locale.getDefault(),
                    "Farm: %s\nLocation: %s\nTotal Hectares: %.1f\nCashew Hectares: %.1f\nTrees per Hectare: %d\n \nTotal Trees: %.0f\nPlanting Year: %d",
                    existingFarm.getName(),
                    existingFarm.getLocation(),
                    existingFarm.getTotalHectares(),
                    existingFarm.getCashewHectares(),
                    existingFarm.getTreesPerHectare(),
                    existingFarm.getCashewHectares() * existingFarm.getTreesPerHectare(),
                    existingFarm.getPlantingYear()
            );

            textView.setText(farmInfo);
            Log.d(TAG, "Farm data: " + existingFarm.toString());
        }
    }
}