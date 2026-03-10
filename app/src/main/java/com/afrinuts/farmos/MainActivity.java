package com.afrinuts.farmos;

import com.afrinuts.farmos.ui.analytics.AnalyticsDashboardActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.ui.expenses.AddExpenseDialog;
import com.afrinuts.farmos.ui.expenses.ExpensesListActivity;
import com.afrinuts.farmos.ui.expenses.ExpenseChartsActivity;
import com.afrinuts.farmos.ui.reports.ReportsActivity;
import com.afrinuts.farmos.ui.revenue.AddRevenueDialog;
import com.afrinuts.farmos.ui.revenue.RevenuesListActivity;
import com.afrinuts.farmos.ui.tasks.TasksListActivity;
import com.afrinuts.farmos.ui.tasks.AddTaskDialog;
import com.afrinuts.farmos.ui.users.UsersListActivity;
import com.afrinuts.farmos.ui.weather.WeatherActivity;
import com.afrinuts.farmos.ui.weather.WeatherForecastDialog;
import com.afrinuts.farmos.ui.workers.WorkerDashboardActivity;
import com.afrinuts.farmos.ui.profit.ProfitDashboardActivity;

import com.afrinuts.farmos.data.remote.WeatherResponse;
import com.afrinuts.farmos.data.repository.WeatherRepository;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.view.MotionEvent;
import java.text.SimpleDateFormat;

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
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.ui.blocks.BlocksActivity;
import com.afrinuts.farmos.ui.blocks.BlockDialog;
import com.afrinuts.farmos.utils.DataSeeder;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.gson.Gson;

import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.appbar.MaterialToolbar;


import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AfriNutsFarmOS";

    // UI Elements
    private CardView btnAnalyticsCard;
    private MaterialToolbar toolbar;
    private CardView btnUsersCard;
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
    private CardView btnTasksListCard;
    private CardView btnAddTaskCard;
    private CardView btnWorkerAnalyticsCard;
    private CardView btnProfitDashboardCard;
    private CardView btnReportsCard;

    private WeatherRepository weatherRepository;
    private WeatherResponse currentWeather;
    private LinearLayout weatherContent;
    private TextView weatherLoadingText;
    private TextView weatherUpdateTime;
    private TextView weatherIconFallback;
    private ImageView weatherIcon;
    private TextView weatherTemp;
    private TextView weatherCondition;
    private TextView weatherHumidity;
    private TextView weatherWind;
    private LinearLayout forecastContainer;
    private Button btnRefreshWeather;
    private CardView weatherCard;

    private SessionManager sessionManager;
    private UserEntity currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        // Initialize views
        initViews();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Load current user
        loadCurrentUser();

        // FIRST: Ensure farm exists
        ensureFarmExists();

        // THEN: Load farm data
        loadFarmData();

        // Setup role-based UI
        setupRoleBasedUI();

        // Setup click listeners
        setupClickListeners();

        // Setup animations and effects
        setupAnimations();
        setupHoverEffects();

        // Load weather data
        weatherRepository = new WeatherRepository(this);
        loadWeatherData();
    }

    private void loadCurrentUser() {
        new Thread(() -> {
            long userId = sessionManager.getUserId();
            if (userId != -1) {
                currentUser = database.userDao().getUserById(userId);
            }
            runOnUiThread(() -> {
                if (currentUser == null) {
                    // User not found in database, logout
                    sessionManager.logout();
                    redirectToLogin();
                }
            });
        }).start();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.logout();
                    redirectToLogin();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupRoleBasedUI() {
        // Team card - only visible to Owner
        if (!sessionManager.isOwner()) {
            btnUsersCard.setVisibility(View.GONE);
        }

        // Add operation cards - only Owners and Managers can add
        if (!sessionManager.canEditData()) {
            btnAddBlockCard.setVisibility(View.GONE);
            btnAddExpenseCard.setVisibility(View.GONE);
            btnAddRevenueCard.setVisibility(View.GONE);
            btnAddTaskCard.setVisibility(View.GONE);
        }

        // Workers Dashboard - hide from Investors
        if (sessionManager.isInvestor()) {
            btnWorkerAnalyticsCard.setVisibility(View.GONE);
        }

        // Profit Dashboard and Charts - visible to all
        // No change needed - these are visible to everyone
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
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        btnUsersCard = findViewById(R.id.btnUsersCard);
        farmNameText = findViewById(R.id.farmNameText);
        btnUsersCard = findViewById(R.id.btnUsersCard);
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
        btnTasksListCard = findViewById(R.id.btnTasksListCard);
        btnAddTaskCard = findViewById(R.id.btnAddTaskCard);
        btnWorkerAnalyticsCard = findViewById(R.id.btnWorkerAnalyticsCard);
        btnProfitDashboardCard = findViewById(R.id.btnProfitDashboardCard);
        btnReportsCard = findViewById(R.id.btnReportsCard);
        btnAnalyticsCard = findViewById(R.id.btnAnalyticsCard);

        weatherContent = findViewById(R.id.weatherContent);
        weatherLoadingText = findViewById(R.id.weatherLoadingText);
        weatherUpdateTime = findViewById(R.id.weatherUpdateTime);
        weatherIconFallback = findViewById(R.id.weatherIconFallback);
        weatherTemp = findViewById(R.id.weatherTemp);
        weatherCondition = findViewById(R.id.weatherCondition);
        weatherHumidity = findViewById(R.id.weatherHumidity);
        weatherWind = findViewById(R.id.weatherWind);
        forecastContainer = findViewById(R.id.forecastContainer);
        btnRefreshWeather = findViewById(R.id.btnRefreshWeather);
        weatherCard = findViewById(R.id.weatherCard);
    }

    private void setupClickListeners() {
        btnViewBlocksCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                Intent intent = new Intent(MainActivity.this, BlocksActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Loading farm data... Please try again", Toast.LENGTH_SHORT).show();
                loadFarmData();
            }
        });

        btnAddBlockCard.setOnClickListener(v -> {
            if (currentFarm != null && sessionManager.canEditData()) {
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
                    loadFarmData();
                    Toast.makeText(this, "Block " + nextBlock + " added successfully!",
                            Toast.LENGTH_SHORT).show();
                });
                dialog.show(getSupportFragmentManager(), "BlockDialog");
            } else {
                Toast.makeText(this, "You don't have permission to add blocks", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddExpenseCard.setOnClickListener(v -> {
            if (currentFarm != null && sessionManager.canEditData()) {
                AddExpenseDialog dialog = AddExpenseDialog.newInstance(currentFarm.getId());
                dialog.setOnExpenseAddedListener(() -> {
                    Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show();
                });
                dialog.show(getSupportFragmentManager(), "AddExpenseDialog");
            } else {
                Toast.makeText(this, "You don't have permission to add expenses", Toast.LENGTH_SHORT).show();
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
            if (currentFarm != null && sessionManager.canEditData()) {
                AddRevenueDialog dialog = AddRevenueDialog.newInstance(currentFarm.getId());
                dialog.setOnRevenueAddedListener(() -> {
                    Toast.makeText(this, "Revenue added!", Toast.LENGTH_SHORT).show();
                });
                dialog.show(getSupportFragmentManager(), "AddRevenueDialog");
            } else {
                Toast.makeText(this, "You don't have permission to add revenue", Toast.LENGTH_SHORT).show();
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

        btnTasksListCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                Intent intent = new Intent(MainActivity.this, TasksListActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddTaskCard.setOnClickListener(v -> {
            if (currentFarm != null && sessionManager.canEditData()) {
                AddTaskDialog dialog = AddTaskDialog.newInstance(currentFarm.getId());
                dialog.setOnTaskAddedListener(() -> {
                    Toast.makeText(this, "Task created successfully!", Toast.LENGTH_SHORT).show();
                });
                dialog.show(getSupportFragmentManager(), "AddTaskDialog");
            } else {
                Toast.makeText(this, "You don't have permission to add tasks", Toast.LENGTH_SHORT).show();
            }
        });

        btnWorkerAnalyticsCard.setOnClickListener(v -> {
            if (currentFarm != null && !sessionManager.isInvestor()) {
                Intent intent = new Intent(MainActivity.this, WorkerDashboardActivity.class);
                startActivity(intent);
            } else if (sessionManager.isInvestor()) {
                Toast.makeText(this, "Investors cannot view worker analytics", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });

        btnProfitDashboardCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                Intent intent = new Intent(MainActivity.this, ProfitDashboardActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });

        btnReportsCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });

        btnRefreshWeather.setOnClickListener(v -> loadWeatherData());

        setupWeatherClick();

        btnUsersCard.setOnClickListener(v -> {
            if (currentFarm != null && sessionManager.isOwner()) {
                Intent intent = new Intent(MainActivity.this, UsersListActivity.class);
                startActivity(intent);
            } else if (!sessionManager.isOwner()) {
                Toast.makeText(this, "Only owners can access team management", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });

        btnAnalyticsCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                Intent intent = new Intent(MainActivity.this, AnalyticsDashboardActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAnimations() {
        // Animate stats cards on load
        View[] statsCards = {
                findViewById(R.id.totalHectaresText).getParent().getParent() instanceof View ?
                        (View) findViewById(R.id.totalHectaresText).getParent().getParent() : null,
                findViewById(R.id.totalTreesText).getParent().getParent() instanceof View ?
                        (View) findViewById(R.id.totalTreesText).getParent().getParent() : null,
                findViewById(R.id.blocksCountText).getParent().getParent() instanceof View ?
                        (View) findViewById(R.id.blocksCountText).getParent().getParent() : null
        };

        for (int i = 0; i < statsCards.length; i++) {
            View card = statsCards[i];
            if (card == null) continue;

            card.setAlpha(0f);
            card.setTranslationY(20f);
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(i * 100)
                    .start();
        }

        // Animate progress card
        View progressCard = findViewById(R.id.weatherCard);
        if (progressCard != null) {
            progressCard.setAlpha(0f);
            progressCard.setTranslationY(20f);
            progressCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(300)
                    .start();
        }
    }

    // Hover effect for cards (touch feedback)
    private void setupHoverEffects() {
        View[] clickableCards = {
                findViewById(R.id.btnViewBlocksCard),
                findViewById(R.id.btnAddBlockCard),
                findViewById(R.id.btnAddExpenseCard),
                findViewById(R.id.btnViewExpensesCard),
                findViewById(R.id.btnAddRevenueCard),
                findViewById(R.id.btnViewRevenueCard),
                findViewById(R.id.btnExpenseChartsCard),
                findViewById(R.id.weatherCard)
        };

        for (View card : clickableCards) {
            if (card != null) {
                card.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            break;
                    }
                    return false;
                });
            }
        }
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

    private void loadWeatherData() {
        weatherLoadingText.setVisibility(View.VISIBLE);
        weatherContent.setVisibility(View.GONE);
        btnRefreshWeather.setVisibility(View.GONE);

        weatherRepository.getWeatherForecast(new WeatherRepository.WeatherCallback() {
            @Override
            public void onSuccess(WeatherResponse weather) {
                runOnUiThread(() -> displayWeather(weather));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    weatherLoadingText.setText("Unable to load weather data");
                    btnRefreshWeather.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Weather error: " + error);
                });
            }
        });
    }

    private void displayWeather(WeatherResponse weather) {
        this.currentWeather = weather;
        weatherLoadingText.setVisibility(View.GONE);
        weatherContent.setVisibility(View.VISIBLE);

        // Current weather
        WeatherResponse.Current current = weather.getCurrent();
        WeatherResponse.Location location = weather.getLocation();

        // Update time
        if (location != null && location.getLocaltime() != null) {
            String timeStr = location.getLocaltime();
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(timeStr);
                weatherUpdateTime.setText("Updated: " + outputFormat.format(date));
            } catch (Exception e) {
                weatherUpdateTime.setText("Updated: " + timeStr);
            }
            weatherUpdateTime.setVisibility(View.VISIBLE);
        }

        // Set temperature and condition
        weatherTemp.setText(String.format(Locale.getDefault(),
                "%.0f°C", current.getTempC()));
        weatherCondition.setText(current.getCondition().getText());

        // Set humidity and wind
        weatherHumidity.setText(String.format(Locale.getDefault(),
                " %d%%", current.getHumidity()));
        weatherWind.setText(String.format(Locale.getDefault(),
                " %.0f km/h", current.getWindKph()));

        // Set weather icon - using fallback only
        setWeatherIconFallback(current.getCondition().getText());
    }

    private void setWeatherIconFallback(String condition) {
        String icon = "☀️";
        String lower = condition.toLowerCase();

        if (lower.contains("rain")) icon = "🌧️";
        else if (lower.contains("cloud")) icon = "☁️";
        else if (lower.contains("storm")) icon = "⛈️";
        else if (lower.contains("snow")) icon = "🌨️";
        else if (lower.contains("fog")) icon = "🌫️";
        else if (lower.contains("wind")) icon = "💨";

        weatherIconFallback.setText(icon);
    }

    private void displayForecast(WeatherResponse.Forecast forecast) {
        if (forecast == null || forecast.getForecastday() == null) return;

        forecastContainer.removeAllViews();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        for (WeatherResponse.ForecastDay day : forecast.getForecastday()) {
            View dayView = getLayoutInflater().inflate(R.layout.item_forecast_day, forecastContainer, false);

            TextView tvDay = dayView.findViewById(R.id.forecastDay);
            TextView tvIcon = dayView.findViewById(R.id.forecastIcon);
            TextView tvTemp = dayView.findViewById(R.id.forecastTemp);
            TextView tvRain = dayView.findViewById(R.id.forecastRain);

            try {
                java.util.Date date = inputFormat.parse(day.getDate());
                tvDay.setText(outputFormat.format(date));
            } catch (Exception e) {
                tvDay.setText("--");
            }

            String condition = day.getDay().getCondition().getText().toLowerCase();
            if (condition.contains("rain")) tvIcon.setText("🌧️");
            else if (condition.contains("cloud")) tvIcon.setText("☁️");
            else tvIcon.setText("☀️");

            tvTemp.setText(String.format(Locale.getDefault(),
                    "%.0f°", day.getDay().getAvgtempC()));
            tvRain.setText(day.getDay().getDailyChanceOfRain() + "%");

            forecastContainer.addView(dayView);
        }
    }

    private void setupWeatherClick() {
        weatherCard.setOnClickListener(v -> {

            if (currentWeather != null) {

                Intent intent = new Intent(this, WeatherActivity.class);
                intent.putExtra("weather_json", new Gson().toJson(currentWeather));
                startActivity(intent);

            } else {

                Toast.makeText(
                        this,
                        "Weather data not available yet",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void showWeatherForecastDialog() {
        if (currentWeather != null) {
            WeatherForecastDialog dialog = WeatherForecastDialog.newInstance(currentWeather);
            dialog.show(getSupportFragmentManager(), "WeatherForecastDialog");
        } else {
            Toast.makeText(this, "Weather data not available yet", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFarmData();

        // Refresh user data
        if (sessionManager.isLoggedIn()) {
            loadCurrentUser();
        }
    }
}