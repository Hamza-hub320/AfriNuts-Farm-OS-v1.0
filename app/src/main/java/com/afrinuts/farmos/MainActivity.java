package com.afrinuts.farmos;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.FarmEntity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AfriNutsFarmOS";
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);

        // Test database
        testDatabase();
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
            Log.d(TAG, "✅ Inserted farm with ID: " + id);

            // Retrieve the farm with generated ID
            existingFarm = db.farmDao().getFarmById(id);
        }

        // Display farm info
        if (existingFarm != null) {
            String farmInfo = String.format(Locale.getDefault(),
                    "🌍 Farm: %s\n📍 Location: %s\n📊 Total Hectares: %.1f\n🌳 Cashew Hectares: %.1f\n🌱 Trees per Hectare: %d\n🧮 Total Trees: %.0f\n📅 Planting Year: %d",
                    existingFarm.getName(),
                    existingFarm.getLocation(),
                    existingFarm.getTotalHectares(),
                    existingFarm.getCashewHectares(),
                    existingFarm.getTreesPerHectare(),
                    existingFarm.getCashewHectares() * existingFarm.getTreesPerHectare(),
                    existingFarm.getPlantingYear()
            );

            textView.setText(farmInfo);
            Log.d(TAG, "✅ Farm data: " + existingFarm.toString());
        }
    }
}