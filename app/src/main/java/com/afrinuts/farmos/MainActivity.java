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
        //AppDatabase db = AppDatabase.getInstance(this);
        //testBlocks(db);
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