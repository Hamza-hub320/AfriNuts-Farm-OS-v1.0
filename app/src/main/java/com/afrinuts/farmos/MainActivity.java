package com.afrinuts.farmos;

import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.ui.analytics.AnalyticsDashboardActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.ui.expenses.AddExpenseDialog;
import com.afrinuts.farmos.ui.expenses.ExpensesListActivity;
import com.afrinuts.farmos.ui.expenses.ExpenseChartsActivity;
import com.afrinuts.farmos.ui.reports.ReportsActivity;
import com.afrinuts.farmos.ui.revenue.AddRevenueDialog;
import com.afrinuts.farmos.ui.revenue.RevenuesListActivity;
import com.afrinuts.farmos.ui.settings.SettingsActivity;
import com.afrinuts.farmos.ui.tasks.TasksListActivity;
import com.afrinuts.farmos.ui.tasks.AddTaskDialog;
import com.afrinuts.farmos.ui.users.UsersListActivity;
import com.afrinuts.farmos.ui.weather.WeatherActivity;
import com.afrinuts.farmos.ui.workers.WorkerDashboardActivity;
import com.afrinuts.farmos.ui.profit.ProfitDashboardActivity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;

import com.afrinuts.farmos.data.remote.WeatherResponse;
import com.afrinuts.farmos.data.repository.WeatherRepository;

import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.view.MotionEvent;
import java.text.SimpleDateFormat;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.Context;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.snackbar.Snackbar;
import com.afrinuts.farmos.data.sync.SyncService;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.ui.sync.SyncStatusReceiver;
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
import com.afrinuts.farmos.utils.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AfriNutsFarmOS";
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // UI Elements
    private SyncManager syncManager;
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

    private SyncService syncService;
    private boolean isBound = false;
    private SyncStatusReceiver syncReceiver;
    private MenuItem syncMenuItem;

    private View offlineBanner;
    private TextView tvSyncStatus;

    private CardView btnSettingsCard;
    private boolean isOnline = true;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SyncService.LocalBinder binder = (SyncService.LocalBinder) service;
            syncService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

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

        // Load current user in background
        loadCurrentUser();

        // FIRST: Ensure farm exists (runs in background and will trigger farm loading)
        ensureFarmExists();

        // Setup sync manager (doesn't require farm yet)
        setupSyncManager();

        // Setup role-based UI (UI only, no db calls)
        setupRoleBasedUI();

        // Setup click listeners
        setupClickListeners();

        // Setup animations and effects
        setupAnimations();
        setupHoverEffects();

        // Setup custom toolbar with sync button
        setupCustomToolbar();

        // Load weather data
        weatherRepository = new WeatherRepository(this);
        loadWeatherData();
    }

    // ========================================
    // USER & SESSION MANAGEMENT
    // ========================================

    private void loadCurrentUser() {
        databaseExecutor.execute(() -> {
            long userId = sessionManager.getUserId();
            if (userId != -1) {
                UserEntity user = database.userDao().getUserById(userId);
                runOnUiThread(() -> {
                    currentUser = user;
                    if (currentUser == null) {
                        // User not found in database, logout
                        sessionManager.logout();
                        redirectToLogin();
                    } else {
                        // Initialize sync listener after user is loaded
                        initializeSyncListener();
                    }
                });
            }
        });
    }

    private void initializeSyncListener() {
        if (currentFarm == null) {
            // Wait for farm to be loaded
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (currentFarm != null) {
                    setupSyncListener();
                }
            }, 500);
        } else {
            setupSyncListener();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ========================================
    // FARM MANAGEMENT
    // ========================================

    private void ensureFarmExists() {
        databaseExecutor.execute(() -> {
            // Check if farm already exists
            FarmEntity existingFarm = database.farmDao().getFirstFarm();

            if (existingFarm == null) {
                Log.d(TAG, "No farm found. Creating default farm...");

                // Create Odienné farm
                FarmEntity odienneFarm = new FarmEntity(
                        "AfriNuts Odienné",
                        "Odienné, Côte d'Ivoire",
                        35.0,
                        35.0,
                        100,
                        2024
                );

                long id = database.farmDao().insert(odienneFarm);
                Log.d(TAG, "Created farm with ID: " + id);

                // Reload farm after insert
                existingFarm = database.farmDao().getFirstFarm();
            } else {
                Log.d(TAG, "Farm already exists with ID: " + existingFarm.getId());
            }

            final FarmEntity farm = existingFarm;

            runOnUiThread(() -> {
                currentFarm = farm;

                // Now that farm exists, load farm data
                loadFarmData();

                // Initialize sync listener if user already loaded
                if (currentUser != null) {
                    initializeSyncListener();
                }

                // AUTO-SYNC: Load users from Firestore automatically
                loadUsersFromFirestore();
            });
        });
    }

    private void loadFarmData() {
        databaseExecutor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();

            if (farm != null) {
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());

                int totalBlocks = blocks.size();
                int plantedBlocks = 0;
                int totalAliveTrees = 0;
                double totalSurvivalRate = 0;

                for (BlockEntity block : blocks) {
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

                final int finalTotalBlocks = totalBlocks;
                final int finalPlantedBlocks = plantedBlocks;
                final int finalTotalAliveTrees = totalAliveTrees;
                final double finalAvgSurvivalRate = avgSurvivalRate;
                final int finalPlantedProgress = plantedProgressValue;
                final FarmEntity finalFarm = farm;
                final List<BlockEntity> finalBlocks = blocks;

                runOnUiThread(() -> {
                    currentFarm = finalFarm;
                    allBlocks = finalBlocks;
                    updateUI(
                            finalTotalBlocks,
                            finalPlantedBlocks,
                            finalTotalAliveTrees,
                            finalAvgSurvivalRate,
                            finalPlantedProgress
                    );

                    if (currentFarm != null && currentUser != null) {
                        setupSyncListener();
                    }
                });
            }
        });
    }

    private void updateUI(int totalBlocks, int plantedBlocks, int totalAliveTrees,
                          double avgSurvivalRate, int plantedProgressValue) {

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

        blocksCountText.setText(String.valueOf(totalBlocks));
        plantedBlocksText.setText(String.format(Locale.getDefault(),
                "%d/%d", plantedBlocks, totalBlocks));

        plantedProgress.setProgress(plantedProgressValue);

        survivalRateText.setText(String.format(Locale.getDefault(),
                "%.0f%%", avgSurvivalRate));
    }

    private String getNextAvailableBlockName() {
        if (allBlocks == null) return "A1";

        char[] rows = {'A', 'B', 'C', 'D', 'E'};
        int columns = 7;

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

    // ========================================
    // USER SYNC FROM FIRESTORE
    // ========================================

    private void loadUsersFromFirestore() {
        // Safety check - farm must be loaded
        if (currentFarm == null) {
            Log.e(TAG, "Cannot load users: farm is null");
            return;
        }

        Log.d(TAG, "Loading users from Firestore for farm: " + currentFarm.getId());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("farmId", currentFarm.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserEntity> firestoreUsers = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        UserEntity user = documentToUser(doc);
                        if (user != null) {
                            firestoreUsers.add(user);
                        }
                    }

                    // Save to local database
                    databaseExecutor.execute(() -> {
                        int addedCount = 0;
                        int updatedCount = 0;

                        for (UserEntity user : firestoreUsers) {
                            UserEntity existing = database.userDao().getUserByEmail(user.getEmail());
                            if (existing == null) {
                                database.userDao().insert(user);
                                addedCount++;
                            } else {
                                user.setId(existing.getId());
                                database.userDao().update(user);
                                updatedCount++;
                            }
                        }

                        final int finalAddedCount = addedCount;
                        final int finalUpdatedCount = updatedCount;

                        runOnUiThread(() -> {
                            Log.d(TAG, "Synced users from Firestore: " +
                                    finalAddedCount + " added, " + finalUpdatedCount + " updated");
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load users from Firestore", e);
                });
    }

    private UserEntity documentToUser(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            String name = doc.getString("name");
            String role = doc.getString("role");
            String phone = doc.getString("phone");
            String email = doc.getString("email");
            String authMethod = doc.getString("authMethod");
            boolean isActive = doc.getBoolean("isActive");
            long createdAt = doc.getLong("createdAt");

            UserEntity user = new UserEntity(name, UserEntity.UserRole.valueOf(role),
                    phone, email, "");
            user.setId(id);
            user.setAuthMethod(UserEntity.AuthMethod.valueOf(authMethod));
            user.setActive(isActive);
            user.setCreatedAt(createdAt);
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document", e);
            return null;
        }
    }

    // ========================================
    // SYNC MANAGEMENT
    // ========================================

    private void setupSyncManager() {
        syncManager = SyncManager.getInstance(this);
    }

    private void setupSyncListener() {
        Log.d(TAG, "setupSyncListener called");
        Log.d(TAG, "currentFarm: " + (currentFarm != null ? currentFarm.getId() : "null"));
        Log.d(TAG, "syncManager: " + (syncManager != null ? "not null" : "null"));

        if (currentFarm == null || syncManager == null) {
            Log.d(TAG, "Cannot setup sync listener - farm or manager null");
            return;
        }

        syncManager.init(this, String.valueOf(currentFarm.getId()), new SyncManager.SyncListener() {
            @Override
            public void onSyncStarted() {
                Log.d(TAG, "Sync started");
                runOnUiThread(() -> {
                    ImageButton btnSync = findViewById(R.id.btnToolbarSync);
                    if (btnSync != null) {
                        animateSyncButton(btnSync, true);
                    }
                });
            }

            @Override
            public void onSyncCompleted(int syncedItems) {
                runOnUiThread(() -> {
                    ImageButton btnSync = findViewById(R.id.btnToolbarSync);
                    if (btnSync != null) {
                        animateSyncButton(btnSync, false);
                    }

                    if (syncedItems > 0) {
                        Toast.makeText(MainActivity.this,
                                "Synced " + syncedItems + " item" + (syncedItems > 1 ? "s" : ""),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "All up to date",
                                Toast.LENGTH_SHORT).show();
                    }
                    loadFarmData();
                });
            }

            @Override
            public void onSyncFailed(String error) {
                Log.e(TAG, "Sync failed: " + error);
                runOnUiThread(() -> {
                    ImageButton btnSync = findViewById(R.id.btnToolbarSync);
                    if (btnSync != null) {
                        animateSyncButton(btnSync, false);
                    }
                    Toast.makeText(MainActivity.this,
                            "Sync failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onOfflineMode() {
                Log.d(TAG, "Offline mode");
                runOnUiThread(() -> {
                    isOnline = false;
                    showOfflineBanner();
                });
            }

            @Override
            public void onOnlineMode() {
                Log.d(TAG, "Online mode");
                runOnUiThread(() -> {
                    isOnline = true;
                    hideOfflineBanner();
                });
            }
        });
    }

    private void animateSyncButton(ImageButton button, boolean start) {
        if (button == null) {
            Log.e(TAG, "animateSyncButton: button is null!");
            return;
        }

        Log.d(TAG, "animateSyncButton: start=" + start + ", button=" + button.getId());

        if (start) {
            // Cancel any existing animations first
            button.animate().cancel();

            // Start new rotation animation
            button.animate()
                    .rotationBy(360)
                    .setDuration(1000)
                    .withStartAction(() -> {
                        Log.d(TAG, "Animation started");
                        button.setEnabled(false); // Disable while animating
                    })
                    .withEndAction(() -> {
                        Log.d(TAG, "Animation ended, isSyncing=" +
                                (syncManager != null ? syncManager.isSyncing() : "null"));

                        button.setRotation(0);
                        button.setEnabled(true); // Re-enable

                        if (syncManager != null && syncManager.isSyncing()) {
                            Log.d(TAG, "Continuing animation - still syncing");
                            animateSyncButton(button, true);
                        }
                    })
                    .start();
        } else {
            Log.d(TAG, "Stopping animation");
            button.animate().cancel();
            button.setRotation(0);
            button.setEnabled(true);
            button.setAlpha(1.0f); // Ensure fully visible
        }
    }

    private void setupCustomToolbar() {
        ImageButton btnSync = findViewById(R.id.btnToolbarSync);
        ImageButton btnLogout = findViewById(R.id.btnToolbarLogout);

        if (btnSync == null) {
            Log.e(TAG, "btnToolbarSync not found in layout!");
            return;
        }

        Log.d(TAG, "Sync button found, setting click listener");

        btnSync.setOnClickListener(v -> {
            Log.d(TAG, "Sync button clicked");

            // Visual feedback that button was clicked
            v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    }).start();

            if (currentFarm != null && syncManager != null) {
                Log.d(TAG, "Farm ID: " + currentFarm.getId());

                if (!syncManager.isSyncing()) {
                    Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();

                    // Force animation to start immediately
                    ImageButton syncButton = (ImageButton) v;
                    animateSyncButton(syncButton, true);

                    syncManager.forceSyncNow();
                } else {
                    Toast.makeText(this, "Sync already in progress", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "Cannot sync - farm: " + currentFarm + ", manager: " + syncManager);
                Toast.makeText(this, "Sync not ready", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showOfflineBanner() {
        if (offlineBanner == null) {
            offlineBanner = findViewById(R.id.offlineBanner);
        }
        if (offlineBanner != null) {
            offlineBanner.setVisibility(View.VISIBLE);
        }
    }

    private void hideOfflineBanner() {
        if (offlineBanner == null) {
            offlineBanner = findViewById(R.id.offlineBanner);
        }
        if (offlineBanner != null) {
            offlineBanner.setVisibility(View.GONE);
        }
    }

    // ========================================
    // UI INITIALIZATION
    // ========================================

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        btnSettingsCard = findViewById(R.id.btnSettingsCard);

        weatherContent = findViewById(R.id.weatherContent);
        weatherLoadingText = findViewById(R.id.weatherLoadingText);
        weatherUpdateTime = findViewById(R.id.weatherUpdateTime);
        weatherIconFallback = findViewById(R.id.weatherIconFallback);
        weatherIcon = findViewById(R.id.weatherIcon);
        weatherTemp = findViewById(R.id.weatherTemp);
        weatherCondition = findViewById(R.id.weatherCondition);
        weatherHumidity = findViewById(R.id.weatherHumidity);
        weatherWind = findViewById(R.id.weatherWind);
        forecastContainer = findViewById(R.id.forecastContainer);
        btnRefreshWeather = findViewById(R.id.btnRefreshWeather);
        weatherCard = findViewById(R.id.weatherCard);
    }

    private void setupRoleBasedUI() {
        if (!sessionManager.isOwner()) {
            btnUsersCard.setVisibility(View.GONE);
        }

        if (!sessionManager.canEditData()) {
            btnAddBlockCard.setVisibility(View.GONE);
            btnAddExpenseCard.setVisibility(View.GONE);
            btnAddRevenueCard.setVisibility(View.GONE);
            btnAddTaskCard.setVisibility(View.GONE);
        }

        if (sessionManager.isInvestor()) {
            btnWorkerAnalyticsCard.setVisibility(View.GONE);
        }
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

        btnSettingsCard.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
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

    // ========================================
    // WEATHER
    // ========================================

    private void loadWeatherData() {
        weatherLoadingText.setVisibility(View.VISIBLE);
        weatherContent.setVisibility(View.GONE);
        btnRefreshWeather.setVisibility(View.GONE);

        weatherRepository.getWeatherForecast(new WeatherRepository.WeatherCallback() {
            @Override
            public void onSuccess(WeatherResponse weather) {
                runOnUiThread(() -> {
                    currentWeather = weather;
                    displayWeather(weather);
                });
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

        WeatherResponse.Current current = weather.getCurrent();
        WeatherResponse.Location location = weather.getLocation();

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

        weatherTemp.setText(String.format(Locale.getDefault(),
                "%.0f°C", current.getTempC()));
        weatherCondition.setText(current.getCondition().getText());

        weatherHumidity.setText(String.format(Locale.getDefault(),
                " %d%%", current.getHumidity()));
        weatherWind.setText(String.format(Locale.getDefault(),
                " %.0f km/h", current.getWindKph()));

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

    private void setupWeatherClick() {
        weatherCard.setOnClickListener(v -> {
            if (currentWeather != null) {
                Intent intent = new Intent(this, WeatherActivity.class);
                intent.putExtra("weather_json", new Gson().toJson(currentWeather));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Weather data not available yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================
    // ACTIVITY LIFECYCLE
    // ========================================

    @Override
    protected void onResume() {
        super.onResume();
        loadFarmData();

        if (sessionManager.isLoggedIn()) {
            loadCurrentUser();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
        }
        if (syncReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(syncReceiver);
        }
        if (syncManager != null) {
            syncManager.shutdown();
        }
        if (weatherRepository != null) {
            weatherRepository.shutdown();
        }
        databaseExecutor.shutdown();
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

    private void saveBlock(BlockEntity block) {
        if (database == null || syncManager == null) return;

        databaseExecutor.execute(() -> {
            if (block.getId() == 0) {
                long id = database.blockDao().insert(block);
                block.setId((int) id);
            } else {
                database.blockDao().update(block);
            }

            syncManager.queueForSync(
                    "block",
                    block.getId(),
                    block.getId() == 0 ?
                            SyncEntity.SyncAction.CREATE :
                            SyncEntity.SyncAction.UPDATE
            );
        });
    }
}