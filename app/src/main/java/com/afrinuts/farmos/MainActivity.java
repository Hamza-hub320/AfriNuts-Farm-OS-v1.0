package com.afrinuts.farmos;

import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
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
import com.afrinuts.farmos.ui.theme.CircularProgressView;
import com.afrinuts.farmos.ui.users.UsersListActivity;
import com.afrinuts.farmos.ui.weather.WeatherActivity;
import com.afrinuts.farmos.ui.workers.WorkerDashboardActivity;
import com.afrinuts.farmos.ui.profit.ProfitDashboardActivity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;

import com.afrinuts.farmos.data.remote.WeatherResponse;
import com.afrinuts.farmos.data.repository.WeatherRepository;
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

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
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AfriNutsFarmOS";
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // Sync Components
    private SyncManager syncManager;
    private FirestoreSyncManager firestoreSyncManager;
    private SyncService syncService;
    private boolean isBound = false;
    private SyncStatusReceiver syncReceiver;
    private MenuItem syncMenuItem;
    private boolean isSyncInitialized = false;

    // UI Elements - Toolbar
    // Toolbar elements
    private TextView toolbarSyncStatus;
    private TextView toolbarUserName;
    private View syncStatusDot;
    private MaterialToolbar toolbar;
    private ImageButton btnToolbarSync;
    private ImageButton btnToolbarLogout;

    // Welcome Header
    private TextView welcomeUserName;
    private TextView locationText;

    // Stats Cards
    private TextView totalHectaresText;
    private TextView totalTreesText;

    // Progress Ring
    private CircularProgressView progressRing;
    private TextView progressPercentText;

    // Financial Snapshot
    private TextView monthlyRevenueText;
    private TextView monthlyExpensesText;
    private TextView netProfitText;
    private TextView revenueTrend;
    private TextView expenseTrend;
    private TextView viewFinancialDetails;

    // Quick Action Buttons
    private CardView btnViewBlocksCard;
    private CardView btnAddBlockCard;
    private CardView btnTasksListCard;
    private CardView btnAddTaskCard;
    private CardView btnAddExpenseCard;
    private CardView btnViewExpensesCard;
    private CardView btnAddRevenueCard;
    private CardView btnViewRevenueCard;
    private CardView btnWorkerAnalyticsCard;
    private CardView btnUsersCard;
    private CardView btnProfitDashboardCard;
    private CardView btnExpenseChartsCard;
    private CardView btnReportsCard;
    private CardView btnAnalyticsCard;
    private CardView btnSettingsCard;

    // Recent Activity
    private LinearLayout recentActivityContainer;

    // Weather fields
    private TextView weatherFeelsLike;
    private TextView weatherHumidity;
    private TextView weatherWind;
    private TextView weatherLocationDetail;
    private LinearLayout forecastContainer;
    private ImageView weatherIcon;
    private CardView weatherCard;
    private TextView weatherTemp;
    private TextView weatherCondition;
    private TextView weatherUpdateTime;
    private WeatherRepository weatherRepository;
    private WeatherResponse currentWeather;

    // Offline Banner
    private View offlineBanner;

    // Data
    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private SessionManager sessionManager;
    private UserEntity currentUser;

    // Repositories
    private BlockRepository blockRepository;
    private ExpenseRepository expenseRepository;
    private RevenueRepository revenueRepository;
    private TaskRepository taskRepository;

    // Firestore Listeners
    private ListenerRegistration blocksListener;
    private ListenerRegistration expensesListener;
    private ListenerRegistration revenuesListener;
    private ListenerRegistration tasksListener;

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

    // ========================================
    // LIFECYCLE
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        initViews();
        setupToolbar();

        database = AppDatabase.getInstance(this);

        long defaultFarmId = 1L;
        blockRepository = new BlockRepository(this, defaultFarmId);
        expenseRepository = new ExpenseRepository(this, defaultFarmId);
        revenueRepository = new RevenueRepository(this, defaultFarmId);
        taskRepository = new TaskRepository(this, defaultFarmId);

        loadCurrentUser();
        ensureFarmExists();

        setupSyncManager();
        setupRoleBasedUI();
        setupClickListeners();
        setupHoverEffects();
        setupWeather();

        loadWeatherData();
    }

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

        if (blocksListener != null) blocksListener.remove();
        if (expensesListener != null) expensesListener.remove();
        if (revenuesListener != null) revenuesListener.remove();
        if (tasksListener != null) tasksListener.remove();
        databaseExecutor.shutdown();
        if (firestoreSyncManager != null) {
            firestoreSyncManager.stopListening();
        }
    }

    // ========================================
    // UI INITIALIZATION
    // ========================================

    private void initViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        btnToolbarSync = findViewById(R.id.btnToolbarSync);
        btnToolbarLogout = findViewById(R.id.btnToolbarLogout);

        // Welcome Header
        welcomeUserName = findViewById(R.id.welcomeUserName);
        locationText = findViewById(R.id.locationText);

        // Stats Cards
        totalHectaresText = findViewById(R.id.totalHectaresText);
        totalTreesText = findViewById(R.id.totalTreesText);

        // Progress Ring
        progressRing = findViewById(R.id.progressRing);

        // Financial Snapshot
        monthlyRevenueText = findViewById(R.id.monthlyRevenueText);
        monthlyExpensesText = findViewById(R.id.monthlyExpensesText);
        netProfitText = findViewById(R.id.netProfitText);
        revenueTrend = findViewById(R.id.revenueTrend);
        expenseTrend = findViewById(R.id.expenseTrend);
        viewFinancialDetails = findViewById(R.id.viewFinancialDetails);

        // Quick Action Buttons
        btnViewBlocksCard = findViewById(R.id.btnViewBlocksCard);
        btnAddBlockCard = findViewById(R.id.btnAddBlockCard);
        btnTasksListCard = findViewById(R.id.btnTasksListCard);
        btnAddTaskCard = findViewById(R.id.btnAddTaskCard);
        btnAddExpenseCard = findViewById(R.id.btnAddExpenseCard);
        btnViewExpensesCard = findViewById(R.id.btnViewExpensesCard);
        btnAddRevenueCard = findViewById(R.id.btnAddRevenueCard);
        btnViewRevenueCard = findViewById(R.id.btnViewRevenueCard);
        btnWorkerAnalyticsCard = findViewById(R.id.btnWorkerAnalyticsCard);
        btnUsersCard = findViewById(R.id.btnUsersCard);
        btnProfitDashboardCard = findViewById(R.id.btnProfitDashboardCard);
        btnExpenseChartsCard = findViewById(R.id.btnExpenseChartsCard);
        btnReportsCard = findViewById(R.id.btnReportsCard);
        btnAnalyticsCard = findViewById(R.id.btnAnalyticsCard);
        btnSettingsCard = findViewById(R.id.btnSettingsCard);

        // Recent Activity
        recentActivityContainer = findViewById(R.id.recentActivityContainer);

        // Weather - MODERN DESIGN (ALL VIEWS)
        weatherCard = findViewById(R.id.weatherCard);
        weatherIcon = findViewById(R.id.weatherIcon);
        weatherTemp = findViewById(R.id.weatherTemp);
        weatherCondition = findViewById(R.id.weatherCondition);
        weatherFeelsLike = findViewById(R.id.weatherFeelsLike);
        weatherHumidity = findViewById(R.id.weatherHumidity);
        weatherWind = findViewById(R.id.weatherWind);
        weatherUpdateTime = findViewById(R.id.weatherUpdateTime);
        weatherLocationDetail = findViewById(R.id.weatherLocationDetail);
        forecastContainer = findViewById(R.id.forecastContainer);

        // Toolbar (new location)
        btnToolbarSync = findViewById(R.id.btnToolbarSync);
        btnToolbarLogout = findViewById(R.id.btnToolbarLogout);
        toolbarSyncStatus = findViewById(R.id.toolbarSyncStatus);
        toolbarUserName = findViewById(R.id.toolbarUserName);
        syncStatusDot = findViewById(R.id.syncStatusDot);

        // Offline Banner
        offlineBanner = findViewById(R.id.offlineBanner);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        if (btnToolbarSync != null) {
            btnToolbarSync.setOnClickListener(v -> {
                if (currentFarm != null && syncManager != null) {
                    if (!syncManager.isSyncing()) {
                        Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();
                        animateSyncButton(btnToolbarSync, true);
                        syncManager.forceSyncNow();
                    } else {
                        Toast.makeText(this, "Sync already in progress", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Sync not ready", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnToolbarLogout != null) {
            btnToolbarLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    // ========================================
    // USER & SESSION
    // ========================================

    private void loadCurrentUser() {
        databaseExecutor.execute(() -> {
            long userId = sessionManager.getUserId();
            if (userId != -1) {
                UserEntity user = database.userDao().getUserById(userId);
                runOnUiThread(() -> {
                    currentUser = user;
                    if (currentUser == null) {
                        sessionManager.logout();
                        redirectToLogin();
                    } else {
                        // Update toolbar with user name
                        String firstName = currentUser.getName() != null && !currentUser.getName().isEmpty()
                                ? currentUser.getName().split(" ")[0]
                                : "User";

                        if (toolbarUserName != null) {
                            toolbarUserName.setText("Hi, " + firstName);
                            toolbarUserName.setVisibility(View.VISIBLE);
                        }

                        if (welcomeUserName != null) {
                            welcomeUserName.setText(firstName);
                        }
                        initializeSyncListener();
                    }
                });
            }
        });
    }

    private void initializeSyncListener() {
        if (currentFarm == null) {
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
            FarmEntity existingFarm = database.farmDao().getFirstFarm();

            if (existingFarm == null) {
                Log.d(TAG, "No farm found. Creating default farm...");

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
                existingFarm = database.farmDao().getFirstFarm();
            }

            final FarmEntity farm = existingFarm;

            runOnUiThread(() -> {
                currentFarm = farm;

                long farmId = farm.getId();
                blockRepository = new BlockRepository(MainActivity.this, farmId);
                expenseRepository = new ExpenseRepository(MainActivity.this, farmId);
                revenueRepository = new RevenueRepository(MainActivity.this, farmId);
                taskRepository = new TaskRepository(MainActivity.this, farmId);

                firestoreSyncManager = FirestoreSyncManager.getInstance(MainActivity.this);
                firestoreSyncManager.startListening(String.valueOf(farmId));

                loadFarmData();

                if (currentUser != null) {
                    initializeSyncListener();
                }

                loadUsersFromFirestore();
                startAllListeners();

                if (locationText != null) {
                    locationText.setText("Odienné, Côte d'Ivoire");
                }
            });
        });
    }

    private void loadFarmData() {
        databaseExecutor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();

            if (farm != null) {
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());

                Log.d(TAG, "Loaded " + blocks.size() + " blocks from database");
                for (BlockEntity block : blocks) {
                    Log.d(TAG, "Block: " + block.getBlockName() + " - Status: " + block.getStatus());
                }

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

                double avgSurvivalRate = plantedBlocks > 0 ? totalSurvivalRate / plantedBlocks : 0;
                int plantedProgressValue = totalBlocks > 0 ? (plantedBlocks * 100 / totalBlocks) : 0;

                final int finalTotalBlocks = totalBlocks;
                final int finalPlantedBlocks = plantedBlocks;
                final int finalTotalAliveTrees = totalAliveTrees;
                final double finalAvgSurvivalRate = avgSurvivalRate;
                final int finalPlantedProgress = plantedProgressValue;
                final List<BlockEntity> finalBlocks = blocks;

                runOnUiThread(() -> {
                    allBlocks = finalBlocks;
                    updateUI(finalTotalBlocks, finalPlantedBlocks, finalTotalAliveTrees,
                            finalAvgSurvivalRate, finalPlantedProgress);

                    if (currentFarm != null && currentUser != null) {
                        setupSyncListener();
                    }
                });
            }
        });
    }

    private void updateUI(int totalBlocks, int plantedBlocks, int totalAliveTrees,
                          double avgSurvivalRate, int plantedProgressValue) {

        // Update farm stats
        if (currentFarm != null) {
            totalHectaresText.setText(String.format(Locale.getDefault(),
                    "%.0f", currentFarm.getTotalHectares()));

            int totalTrees = (int) (currentFarm.getCashewHectares() *
                    currentFarm.getTreesPerHectare());
            totalTreesText.setText(String.format(Locale.getDefault(),
                    "%,d", totalTrees));
        }

        // Update progress ring (circular progress)
        updateProgressRing();

        // Update financial snapshot (revenue, expenses, profit)
        updateFinancialSnapshot();

        // Load recent activity
        loadRecentActivity();
    }

    private void updateProgressRing() {
        if (allBlocks == null || progressRing == null) {
            Log.d(TAG, "updateProgressRing: allBlocks is null or progressRing null");
            return;
        }

        int totalBlocks = allBlocks.size();
        int plantedBlocks = 0;

        for (BlockEntity block : allBlocks) {
            if (block.isPlanted()) {
                plantedBlocks++;
                Log.d(TAG, "Planted block: " + block.getBlockName());
            }
        }

        int progressPercent = totalBlocks > 0 ? (plantedBlocks * 100 / totalBlocks) : 0;

        Log.d(TAG, "Total blocks: " + totalBlocks);
        Log.d(TAG, "Planted blocks: " + plantedBlocks);
        Log.d(TAG, "Progress percent: " + progressPercent + "%");

        progressRing.setProgress(progressPercent);
        progressRing.setMax(100);
    }

    private void updateFinancialSnapshot() {
        if (currentFarm == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        new Thread(() -> {
            double monthlyRevenue = 0;
            double monthlyExpenses = 0;

            List<RevenueEntity> revenues = database.revenueDao().getRevenuesByFarmId(currentFarm.getId());
            for (RevenueEntity revenue : revenues) {
                if (revenue.getHarvestDate() >= startOfMonth) {
                    monthlyRevenue += revenue.getTotalAmount();
                }
            }

            List<ExpenseEntity> expenses = database.expenseDao().getExpensesByFarmId(currentFarm.getId());
            for (ExpenseEntity expense : expenses) {
                if (expense.getDate() >= startOfMonth) {
                    monthlyExpenses += expense.getAmount();
                }
            }

            double netProfit = monthlyRevenue - monthlyExpenses;
            double revenueTrendPercent = calculateRevenueTrend();
            double expenseTrendPercent = calculateExpenseTrend();

            final double finalMonthlyRevenue = monthlyRevenue;
            final double finalMonthlyExpenses = monthlyExpenses;
            final double finalNetProfit = netProfit;

            runOnUiThread(() -> {
                monthlyRevenueText.setText(formatCurrency(finalMonthlyRevenue));
                monthlyExpensesText.setText(formatCurrency(finalMonthlyExpenses));
                netProfitText.setText(formatCurrency(finalNetProfit));
                netProfitText.setTextColor(finalNetProfit >= 0 ?
                        getColor(R.color.primary) : getColor(android.R.color.holo_red_dark));

                revenueTrend.setText(formatTrend(revenueTrendPercent));
                revenueTrend.setTextColor(revenueTrendPercent >= 0 ?
                        getColor(android.R.color.holo_green_dark) : getColor(android.R.color.holo_red_dark));

                expenseTrend.setText(formatTrend(expenseTrendPercent));
                expenseTrend.setTextColor(expenseTrendPercent <= 0 ?
                        getColor(android.R.color.holo_green_dark) : getColor(android.R.color.holo_red_dark));
            });
        }).start();
    }

    private double calculateRevenueTrend() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, -1);
        long startOfLastMonth = calendar.getTimeInMillis();

        double currentMonthRevenue = 0;
        double lastMonthRevenue = 0;

        List<RevenueEntity> revenues = database.revenueDao().getRevenuesByFarmId(currentFarm.getId());
        for (RevenueEntity revenue : revenues) {
            if (revenue.getHarvestDate() >= startOfMonth) {
                currentMonthRevenue += revenue.getTotalAmount();
            } else if (revenue.getHarvestDate() >= startOfLastMonth) {
                lastMonthRevenue += revenue.getTotalAmount();
            }
        }

        if (lastMonthRevenue == 0) return 0;
        return ((currentMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100;
    }

    private double calculateExpenseTrend() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, -1);
        long startOfLastMonth = calendar.getTimeInMillis();

        double currentMonthExpenses = 0;
        double lastMonthExpenses = 0;

        List<ExpenseEntity> expenses = database.expenseDao().getExpensesByFarmId(currentFarm.getId());
        for (ExpenseEntity expense : expenses) {
            if (expense.getDate() >= startOfMonth) {
                currentMonthExpenses += expense.getAmount();
            } else if (expense.getDate() >= startOfLastMonth) {
                lastMonthExpenses += expense.getAmount();
            }
        }

        if (lastMonthExpenses == 0) return 0;
        return ((currentMonthExpenses - lastMonthExpenses) / lastMonthExpenses) * 100;
    }

    private void loadRecentActivity() {
        if (currentFarm == null || recentActivityContainer == null) return;

        new Thread(() -> {
            long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
            List<TaskEntity> recentTasks = database.taskDao().getTasksByFarmId(currentFarm.getId());
            List<ExpenseEntity> recentExpenses = database.expenseDao().getExpensesByFarmId(currentFarm.getId());
            List<RevenueEntity> recentRevenues = database.revenueDao().getRevenuesByFarmId(currentFarm.getId());

            runOnUiThread(() -> {
                recentActivityContainer.removeAllViews();

                int count = 0;

                // Add tasks
                for (TaskEntity task : recentTasks) {
                    if (count >= 5) break;
                    if (task.getUpdatedAt() >= oneWeekAgo) {
                        String blockName = getBlockName(task.getBlockId());
                        String subtitle = blockName != null ? "Block " + blockName : "Farm-Wide";

                        addActivityItem(
                                task.getTitle(),
                                subtitle,
                                formatTimeAgo(task.getUpdatedAt()),
                                getTaskIcon(task.getType()),
                                task.getStatus() == TaskEntity.TaskStatus.COMPLETED
                        );
                        count++;
                    }
                }

                // Add expenses
                for (ExpenseEntity expense : recentExpenses) {
                    if (count >= 5) break;
                    if (expense.getUpdatedAt() >= oneWeekAgo) {
                        String blockName = getBlockName(expense.getBlockId());
                        String subtitle = blockName != null ? "Block " + blockName : "Farm-Wide";

                        addActivityItem(
                                expense.getCategory().getDisplayName(),
                                subtitle + " • " + formatCurrency(expense.getAmount()),
                                formatTimeAgo(expense.getDate()),
                                getExpenseIcon(expense.getCategory()),
                                false
                        );
                        count++;
                    }
                }

                // Add revenues
                for (RevenueEntity revenue : recentRevenues) {
                    if (count >= 5) break;
                    if (revenue.getUpdatedAt() >= oneWeekAgo) {
                        String blockName = getBlockName(revenue.getBlockId());
                        String subtitle = blockName != null ? "Block " + blockName : "Processing Center";

                        addActivityItem(
                                revenue.getQuality().getDisplayName() + " Grade",
                                subtitle + " • " + formatCurrency(revenue.getTotalAmount()),
                                formatTimeAgo(revenue.getHarvestDate()),
                                getRevenueIcon(revenue.getQuality()),
                                true
                        );
                        count++;
                    }
                }

                if (count == 0) {
                    addActivityItem(
                            "No Recent Activity",
                            "Add blocks, tasks, or expenses to see activity here",
                            "",
                            R.drawable.ic_note,
                            false
                    );
                }
            });
        }).start();
    }

    private String getBlockName(Long blockId) {
        if (blockId == null || allBlocks == null) return null;
        for (BlockEntity block : allBlocks) {
            if (block.getId() == blockId) {
                return block.getBlockName();
            }
        }
        return null;
    }

    private void addActivityItem(String title, String subtitle, String time, int iconRes, boolean showStatus) {
        View itemView = getLayoutInflater().inflate(R.layout.item_recent_activity, recentActivityContainer, false);

        ImageView iconView = itemView.findViewById(R.id.activityIcon);
        TextView titleView = itemView.findViewById(R.id.activityTitle);
        TextView subtitleView = itemView.findViewById(R.id.activitySubtitle);
        TextView timeView = itemView.findViewById(R.id.activityTime);
        ImageView statusView = itemView.findViewById(R.id.activityStatus);

        iconView.setImageResource(iconRes);
        iconView.setColorFilter(getColor(R.color.primary));
        titleView.setText(title);
        subtitleView.setText(subtitle);
        timeView.setText(time);

        if (showStatus) {
            statusView.setVisibility(View.VISIBLE);
            statusView.setColorFilter(getColor(R.color.primary));
        } else {
            statusView.setVisibility(View.GONE);
        }

        recentActivityContainer.addView(itemView);
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String formatCurrency(double amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.1fM XAF", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format(Locale.getDefault(), "%.0fK XAF", amount / 1_000);
        }
        return String.format(Locale.getDefault(), "%.0f XAF", amount);
    }

    private String formatTrend(double percent) {
        return String.format(Locale.getDefault(), "%+.1f%%", percent);
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
    // SYNC MANAGEMENT
    // ========================================

    private void setupSyncManager() {
        syncManager = SyncManager.getInstance(this);
    }

    private void setupSyncListener() {
        if (isSyncInitialized) {
            Log.d(TAG, "Sync already initialized, skipping");
            return;
        }

        if (currentFarm == null || syncManager == null) {
            Log.d(TAG, "Cannot setup sync listener - farm or manager null");
            return;
        }

        Log.d(TAG, "Initializing SyncManager for farm: " + currentFarm.getId());

        syncManager.init(this, currentFarm.getId(), new SyncManager.SyncListener() {
            @Override
            public void onSyncStarted() {
                runOnUiThread(() -> {
                    // Update toolbar status
                    if (toolbarSyncStatus != null) {
                        toolbarSyncStatus.setText("Syncing...");
                        toolbarSyncStatus.setVisibility(View.VISIBLE);
                    }
                    if (syncStatusDot != null) {
                        syncStatusDot.setVisibility(View.VISIBLE);
                    }
                    if (btnToolbarSync != null) {
                        animateSyncButton(btnToolbarSync, true);
                    }
                });
            }

            @Override
            public void onSyncCompleted(int syncedItems) {
                runOnUiThread(() -> {
                    // Update toolbar status
                    if (toolbarSyncStatus != null) {
                        toolbarSyncStatus.setText("Synced");
                        toolbarSyncStatus.setVisibility(View.VISIBLE);
                        // Hide after 2 seconds
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (toolbarSyncStatus != null) {
                                toolbarSyncStatus.setVisibility(View.GONE);
                            }
                        }, 2000);
                    }
                    if (syncStatusDot != null) {
                        syncStatusDot.setVisibility(View.GONE);
                    }
                    if (btnToolbarSync != null) {
                        animateSyncButton(btnToolbarSync, false);
                    }
                    if (syncedItems > 0) {
                        Toast.makeText(MainActivity.this,
                                "Synced " + syncedItems + " item" + (syncedItems > 1 ? "s" : ""),
                                Toast.LENGTH_SHORT).show();
                    }
                    loadFarmData();
                });
            }

            @Override
            public void onSyncFailed(String error) {
                runOnUiThread(() -> {
                    if (toolbarSyncStatus != null) {
                        toolbarSyncStatus.setText("Sync failed");
                        toolbarSyncStatus.setVisibility(View.VISIBLE);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (toolbarSyncStatus != null) {
                                toolbarSyncStatus.setVisibility(View.GONE);
                            }
                        }, 3000);
                    }
                    if (syncStatusDot != null) {
                        syncStatusDot.setVisibility(View.GONE);
                    }
                    if (btnToolbarSync != null) {
                        animateSyncButton(btnToolbarSync, false);
                    }
                    Toast.makeText(MainActivity.this,
                            "Sync failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onOfflineMode() {
                runOnUiThread(() -> showOfflineBanner());
            }

            @Override
            public void onOnlineMode() {
                runOnUiThread(() -> hideOfflineBanner());
            }
        });

        isSyncInitialized = true;
    }

    private void animateSyncButton(ImageButton button, boolean start) {
        if (button == null) return;

        if (start) {
            button.animate().cancel();
            button.animate()
                    .rotationBy(360)
                    .setDuration(1000)
                    .withEndAction(() -> {
                        button.setRotation(0);
                        if (syncManager != null && syncManager.isSyncing()) {
                            animateSyncButton(button, true);
                        }
                    })
                    .start();
        } else {
            button.animate().cancel();
            button.setRotation(0);
        }
    }

    private void showOfflineBanner() {
        if (offlineBanner != null) {
            offlineBanner.setVisibility(View.VISIBLE);
        }
    }

    private void hideOfflineBanner() {
        if (offlineBanner != null) {
            offlineBanner.setVisibility(View.GONE);
        }
    }

    // ========================================
    // USER SYNC FROM FIRESTORE
    // ========================================

    private void loadUsersFromFirestore() {
        if (currentFarm == null) return;

        Log.d(TAG, "Loading users from Firestore for farm: " + currentFarm.getId());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String farmId = String.valueOf(currentFarm.getId());

        db.collection("users")
                .whereEqualTo("farmId", farmId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        UserEntity user = documentToUser(doc);
                        if (user != null) {
                            UserEntity existing = database.userDao().getUserByEmail(user.getEmail());
                            if (existing == null) {
                                database.userDao().insert(user);
                            } else {
                                user.setLocalId(existing.getLocalId());
                                database.userDao().update(user);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load users from Firestore", e));
    }

    private UserEntity documentToUser(DocumentSnapshot doc) {
        try {
            String name = doc.getString("name");
            String role = doc.getString("role");
            String phone = doc.getString("phone");
            String email = doc.getString("email");
            String authMethod = doc.getString("authMethod");
            Boolean isActive = doc.getBoolean("isActive");

            if (name == null || role == null || email == null) return null;

            UserEntity user = new UserEntity(name, UserEntity.UserRole.valueOf(role),
                    phone != null ? phone : "", email, "");
            if (authMethod != null) {
                user.setAuthMethod(UserEntity.AuthMethod.valueOf(authMethod));
            }
            user.setActive(isActive != null ? isActive : true);
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to user", e);
            return null;
        }
    }

    // ========================================
    // FIRESTORE REAL-TIME LISTENERS
    // ========================================

    private void startAllListeners() {
        if (currentFarm == null) return;

        String farmId = String.valueOf(currentFarm.getId());
        Log.d(TAG, "Starting real-time listeners for farm: " + farmId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        blocksListener = db.collection("blocks")
                .whereEqualTo("farmId", farmId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Blocks listener failed", error);
                        return;
                    }
                    handleDataChanges("blocks", snapshots);
                });

        expensesListener = db.collection("expenses")
                .whereEqualTo("farmId", farmId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Expenses listener failed", error);
                        return;
                    }
                    handleDataChanges("expenses", snapshots);
                });

        revenuesListener = db.collection("revenues")
                .whereEqualTo("farmId", farmId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Revenues listener failed", error);
                        return;
                    }
                    handleDataChanges("revenues", snapshots);
                });

        tasksListener = db.collection("tasks")
                .whereEqualTo("farmId", farmId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Tasks listener failed", error);
                        return;
                    }
                    handleDataChanges("tasks", snapshots);
                });
    }

    private void handleDataChanges(String collection, QuerySnapshot snapshots) {
        if (snapshots == null) return;

        for (DocumentChange dc : snapshots.getDocumentChanges()) {
            DocumentSnapshot document = dc.getDocument();

            switch (dc.getType()) {
                case ADDED:
                    Log.d(TAG, "New " + collection + " added remotely");
                    handleDocumentAdded(collection, document);
                    break;
                case MODIFIED:
                    Log.d(TAG, collection + " modified remotely");
                    handleDocumentModified(collection, document);
                    break;
                case REMOVED:
                    Log.d(TAG, collection + " removed remotely");
                    handleDocumentRemoved(collection, document);
                    break;
            }
        }

        runOnUiThread(this::loadFarmData);
    }

    private void handleDocumentAdded(String collection, DocumentSnapshot doc) {
        databaseExecutor.execute(() -> {
            try {
                switch (collection) {
                    case "blocks":
                        BlockEntity block = documentToBlock(doc);
                        if (block != null) {
                            blockRepository.syncBlockFromFirestore(doc);
                        }
                        break;
                    case "expenses":
                        ExpenseEntity expense = documentToExpense(doc);
                        if (expense != null) {
                            expenseRepository.syncExpenseFromFirestore(doc);
                        }
                        break;
                    case "revenues":
                        RevenueEntity revenue = documentToRevenue(doc);
                        if (revenue != null) {
                            revenueRepository.syncRevenueFromFirestore(doc);
                        }
                        break;
                    case "tasks":
                        TaskEntity task = documentToTask(doc);
                        if (task != null) {
                            taskRepository.syncTaskFromFirestore(doc);
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling added document", e);
            }
        });
    }

    private void handleDocumentModified(String collection, DocumentSnapshot doc) {
        databaseExecutor.execute(() -> {
            try {
                switch (collection) {
                    case "blocks":
                        BlockEntity block = documentToBlock(doc);
                        if (block != null) {
                            blockRepository.syncBlockFromFirestore(doc);
                        }
                        break;
                    case "expenses":
                        ExpenseEntity expense = documentToExpense(doc);
                        if (expense != null) {
                            expenseRepository.syncExpenseFromFirestore(doc);
                        }
                        break;
                    case "revenues":
                        RevenueEntity revenue = documentToRevenue(doc);
                        if (revenue != null) {
                            revenueRepository.syncRevenueFromFirestore(doc);
                        }
                        break;
                    case "tasks":
                        TaskEntity task = documentToTask(doc);
                        if (task != null) {
                            taskRepository.syncTaskFromFirestore(doc);
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling modified document", e);
            }
        });
    }

    private void handleDocumentRemoved(String collection, DocumentSnapshot doc) {
        databaseExecutor.execute(() -> {
            try {
                long id = doc.getLong("id");
                switch (collection) {
                    case "blocks":
                        BlockEntity block = database.blockDao().getBlockById(id);
                        if (block != null) {
                            database.blockDao().delete(block);
                        }
                        break;
                    case "expenses":
                        ExpenseEntity expense = database.expenseDao().getExpenseById(id);
                        if (expense != null) {
                            database.expenseDao().delete(expense);
                        }
                        break;
                    case "revenues":
                        RevenueEntity revenue = database.revenueDao().getRevenueById(id);
                        if (revenue != null) {
                            database.revenueDao().delete(revenue);
                        }
                        break;
                    case "tasks":
                        TaskEntity task = database.taskDao().getTaskById(id);
                        if (task != null) {
                            database.taskDao().deleteTask(task);
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling removed document", e);
            }
        });
    }

    // ========================================
    // DOCUMENT CONVERTERS
    // ========================================

    private BlockEntity documentToBlock(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            String farmIdStr = doc.getString("farmId");
            long farmId = farmIdStr != null ? Long.parseLong(farmIdStr) : 1L;
            String blockName = doc.getString("blockName");
            double hectareSize = doc.getDouble("hectareSize");
            String status = doc.getString("status");
            Long clearedDate = doc.getLong("clearedDate");
            Long plowedDate = doc.getLong("plowedDate");
            Long plantedDate = doc.getLong("plantedDate");
            double survivalRate = doc.getDouble("survivalRate");
            long replacementCount = doc.getLong("replacementCount");
            String notes = doc.getString("notes");

            BlockEntity block = new BlockEntity(farmId, blockName, hectareSize, notes);
            block.setId(id);
            block.setStatus(BlockEntity.BlockStatus.valueOf(status));
            block.setClearedDate(clearedDate);
            block.setPlowedDate(plowedDate);
            block.setPlantedDate(plantedDate);
            block.setSurvivalRate(survivalRate);
            block.setReplacementCount((int) replacementCount);
            return block;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to block", e);
            return null;
        }
    }

    private ExpenseEntity documentToExpense(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            String farmIdStr = doc.getString("farmId");
            long farmId = farmIdStr != null ? Long.parseLong(farmIdStr) : 1L;
            Long blockId = doc.contains("blockId") ? doc.getLong("blockId") : null;
            String category = doc.getString("category");
            double amount = doc.getDouble("amount");
            long date = doc.getLong("date");
            String description = doc.getString("description");
            return new ExpenseEntity(farmId, blockId,
                    ExpenseEntity.ExpenseCategory.valueOf(category), amount, date, description);
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to expense", e);
            return null;
        }
    }

    private RevenueEntity documentToRevenue(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            String farmIdStr = doc.getString("farmId");
            long farmId = farmIdStr != null ? Long.parseLong(farmIdStr) : 1L;
            Long blockId = doc.contains("blockId") ? doc.getLong("blockId") : null;
            long harvestDate = doc.getLong("harvestDate");
            double quantityKg = doc.getDouble("quantityKg");
            double pricePerKg = doc.getDouble("pricePerKg");
            String buyer = doc.getString("buyer");
            String quality = doc.getString("quality");
            String notes = doc.getString("notes");
            return new RevenueEntity(farmId, blockId, harvestDate, quantityKg,
                    pricePerKg, buyer, RevenueEntity.QualityGrade.valueOf(quality), notes);
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to revenue", e);
            return null;
        }
    }

    private TaskEntity documentToTask(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            String farmIdStr = doc.getString("farmId");
            long farmId = farmIdStr != null ? Long.parseLong(farmIdStr) : 1L;
            Long blockId = doc.contains("blockId") ? doc.getLong("blockId") : null;
            String title = doc.getString("title");
            String description = doc.getString("description");
            String status = doc.getString("status");
            String type = doc.getString("type");
            long dueDate = doc.getLong("dueDate");
            Long completedDate = doc.contains("completedDate") ? doc.getLong("completedDate") : null;
            return new TaskEntity(farmId, blockId, title, description,
                    TaskEntity.TaskType.valueOf(type), TaskEntity.TaskStatus.valueOf(status), dueDate);
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to task", e);
            return null;
        }
    }

    // ========================================
    // CLICK LISTENERS
    // ========================================

    private void setupRoleBasedUI() {
        if (!sessionManager.isOwner() && btnUsersCard != null) {
            btnUsersCard.setVisibility(View.GONE);
        }

        if (!sessionManager.canEditData()) {
            if (btnAddBlockCard != null) btnAddBlockCard.setVisibility(View.GONE);
            if (btnAddExpenseCard != null) btnAddExpenseCard.setVisibility(View.GONE);
            if (btnAddRevenueCard != null) btnAddRevenueCard.setVisibility(View.GONE);
            if (btnAddTaskCard != null) btnAddTaskCard.setVisibility(View.GONE);
        }

        if (sessionManager.isInvestor() && btnWorkerAnalyticsCard != null) {
            btnWorkerAnalyticsCard.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // Farm Operations
        btnViewBlocksCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                startActivity(new Intent(MainActivity.this, BlocksActivity.class));
            }
        });

        btnAddBlockCard.setOnClickListener(v -> {
            if (currentFarm != null && sessionManager.canEditData()) {
                String nextBlock = getNextAvailableBlockName();
                if (nextBlock.equals("FULL")) {
                    Toast.makeText(this, "All 35 blocks created", Toast.LENGTH_LONG).show();
                    return;
                }
                BlockDialog dialog = BlockDialog.newInstance(currentFarm.getId());
                dialog.setNextBlockName(nextBlock);
                dialog.setOnBlockAddedListener(() -> loadFarmData());
                dialog.show(getSupportFragmentManager(), "BlockDialog");
            }
        });

        btnTasksListCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                startActivity(new Intent(MainActivity.this, TasksListActivity.class));
            }
        });

        btnAddTaskCard.setOnClickListener(v -> {
            if (currentFarm != null && sessionManager.canEditData()) {
                AddTaskDialog dialog = AddTaskDialog.newInstance(currentFarm.getId());
                dialog.setOnTaskAddedListener(() -> loadFarmData());
                dialog.show(getSupportFragmentManager(), "AddTaskDialog");
            }
        });

        // Financial
        btnAddExpenseCard.setOnClickListener(v -> {
            if (currentFarm != null && sessionManager.canEditData()) {
                AddExpenseDialog dialog = AddExpenseDialog.newInstance(currentFarm.getId());
                dialog.setOnExpenseAddedListener(() -> loadFarmData());
                dialog.show(getSupportFragmentManager(), "AddExpenseDialog");
            }
        });

        btnViewExpensesCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                startActivity(new Intent(MainActivity.this, ExpensesListActivity.class));
            }
        });

        btnAddRevenueCard.setOnClickListener(v -> {
            if (currentFarm != null && sessionManager.canEditData()) {
                AddRevenueDialog dialog = AddRevenueDialog.newInstance(currentFarm.getId());
                dialog.setOnRevenueAddedListener(() -> loadFarmData());
                dialog.show(getSupportFragmentManager(), "AddRevenueDialog");
            }
        });

        btnViewRevenueCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                startActivity(new Intent(MainActivity.this, RevenuesListActivity.class));
            }
        });

        // Management
        btnWorkerAnalyticsCard.setOnClickListener(v -> {
            if (currentFarm != null && !sessionManager.isInvestor()) {
                startActivity(new Intent(MainActivity.this, WorkerDashboardActivity.class));
            }
        });

        btnUsersCard.setOnClickListener(v -> {
            if (currentFarm != null && sessionManager.isOwner()) {
                startActivity(new Intent(MainActivity.this, UsersListActivity.class));
            }
        });

        btnProfitDashboardCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                startActivity(new Intent(MainActivity.this, ProfitDashboardActivity.class));
            }
        });

        btnExpenseChartsCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                startActivity(new Intent(MainActivity.this, ExpenseChartsActivity.class));
            }
        });

        // Reports & Settings
        btnReportsCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                startActivity(new Intent(MainActivity.this, ReportsActivity.class));
            }
        });

        btnAnalyticsCard.setOnClickListener(v -> {
            if (currentFarm != null) {
                startActivity(new Intent(MainActivity.this, AnalyticsDashboardActivity.class));
            }
        });

        btnSettingsCard.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        if (viewFinancialDetails != null) {
            viewFinancialDetails.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ProfitDashboardActivity.class));
            });
        }
    }

    private void setupHoverEffects() {
        View[] clickableCards = {
                btnViewBlocksCard, btnAddBlockCard, btnTasksListCard, btnAddTaskCard,
                btnAddExpenseCard, btnViewExpensesCard, btnAddRevenueCard, btnViewRevenueCard,
                btnWorkerAnalyticsCard, btnUsersCard, btnProfitDashboardCard,
                btnExpenseChartsCard, btnReportsCard, btnAnalyticsCard, btnSettingsCard,
                weatherCard
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

    private void setupWeather() {
        weatherRepository = new WeatherRepository(this);
        if (weatherCard != null) {
            weatherCard.setOnClickListener(v -> {
                if (currentWeather != null) {
                    Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
                    intent.putExtra("weather_json", new Gson().toJson(currentWeather));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Weather data not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadWeatherData() {
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
                    if (weatherCondition != null) {
                        weatherCondition.setText("Weather unavailable");
                    }
                    Log.e(TAG, "Weather error: " + error);
                });
            }
        });
    }

    private void displayWeather(WeatherResponse weather) {
        if (weather == null) return;

        currentWeather = weather;
        WeatherResponse.Current current = weather.getCurrent();
        WeatherResponse.Location location = weather.getLocation();

        // Location - with null check
        if (weatherLocationDetail != null && location != null) {
            weatherLocationDetail.setText(location.getRegion() != null ?
                    location.getRegion() : "Côte d'Ivoire");
        }

        // Update time - with null check
        if (weatherUpdateTime != null && location != null && location.getLocaltime() != null) {
            String timeStr = location.getLocaltime();
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(timeStr);
                weatherUpdateTime.setText("Updated " + outputFormat.format(date));
            } catch (Exception e) {
                weatherUpdateTime.setText("Updated");
            }
        } else if (weatherUpdateTime != null) {
            weatherUpdateTime.setText("");
        }

        // Temperature and condition - with null checks
        if (weatherTemp != null) {
            weatherTemp.setText(String.format(Locale.getDefault(), "%.0f°C", current.getTempC()));
        }
        if (weatherCondition != null) {
            weatherCondition.setText(current.getCondition().getText());
        }
        if (weatherFeelsLike != null) {
            weatherFeelsLike.setText(String.format(Locale.getDefault(), "%.0f°C", current.getFeelslikeC()));
        }

        // Humidity and wind - with null checks
        if (weatherHumidity != null) {
            weatherHumidity.setText(current.getHumidity() + "%");
        }
        if (weatherWind != null) {
            weatherWind.setText(String.format(Locale.getDefault(), "%.0f km/h", current.getWindKph()));
        }

        // Set weather icon
        if (weatherIcon != null) {
            setWeatherIcon(current.getCondition().getText());
        }

        // Load forecast if available
        if (forecastContainer != null && weather.getForecast() != null &&
                weather.getForecast().getForecastday() != null) {
            displayForecast(weather.getForecast());
        }
    }

    private void displayForecast(WeatherResponse.Forecast forecast) {
        if (forecastContainer == null || forecast.getForecastday() == null) return;

        forecastContainer.removeAllViews();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        // Get up to 3 days of forecast
        int maxDays = Math.min(3, forecast.getForecastday().size());

        for (int i = 0; i < maxDays; i++) {
            WeatherResponse.ForecastDay day = forecast.getForecastday().get(i);

            View dayView = getLayoutInflater().inflate(R.layout.item_forecast_day, forecastContainer, false);

            TextView tvDay = dayView.findViewById(R.id.forecastDay);
            ImageView ivIcon = dayView.findViewById(R.id.forecastIcon);
            TextView tvTemp = dayView.findViewById(R.id.forecastTemp);
            TextView tvRain = dayView.findViewById(R.id.forecastRain);

            // Set day name
            try {
                Date date = inputFormat.parse(day.getDate());
                tvDay.setText(outputFormat.format(date));
            } catch (Exception e) {
                tvDay.setText("--");
            }

            // Set icon based on condition
            String condition = day.getDay().getCondition().getText().toLowerCase();
            int iconRes;
            if (condition.contains("rain")) {
                iconRes = R.drawable.ic_rain;
            } else if (condition.contains("cloud")) {
                iconRes = R.drawable.ic_cloud;
            } else if (condition.contains("storm")) {
                iconRes = R.drawable.ic_storm;
            } else {
                iconRes = R.drawable.ic_wb_sunny;
            }

            ivIcon.setImageResource(iconRes);
            ivIcon.setColorFilter(getColor(R.color.accent));

            // Set temperature
            tvTemp.setText(String.format(Locale.getDefault(), "%.0f°", day.getDay().getAvgtempC()));

            // Set rain chance
            tvRain.setText(day.getDay().getDailyChanceOfRain() + "%");

            forecastContainer.addView(dayView);
        }
    }

    private void setWeatherIcon(String condition) {
        if (weatherIcon == null) return;

        String lower = condition.toLowerCase();
        int iconRes;

        if (lower.contains("rain")) {
            iconRes = R.drawable.ic_rain;
        } else if (lower.contains("cloud")) {
            iconRes = R.drawable.ic_cloud;
        } else if (lower.contains("storm")) {
            iconRes = R.drawable.ic_storm;
        } else if (lower.contains("snow")) {
            iconRes = R.drawable.ic_snow;
        } else if (lower.contains("fog")) {
            iconRes = R.drawable.ic_fog;
        } else if (lower.contains("wind")) {
            iconRes = R.drawable.ic_air;
        } else {
            iconRes = R.drawable.ic_wb_sunny;
        }

        weatherIcon.setImageResource(iconRes);
        weatherIcon.setColorFilter(getColor(R.color.accent));
    }

    // ========================================
    // UTILITY
    // ========================================

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

    private int getTaskIcon(TaskEntity.TaskType type) {
        switch (type) {
            case CLEARING: return R.drawable.ic_tractor;
            case PLOWING: return R.drawable.ic_grain;
            case PLANTING: return R.drawable.ic_grass;
            case REPLACEMENT: return R.drawable.ic_sync;
            case FERTILIZING: return R.drawable.ic_fertilizer;
            case PRUNING: return R.drawable.ic_cut;
            case WEEDING: return R.drawable.ic_grass;
            case HARVEST: return R.drawable.ic_download;
            case IRRIGATION: return R.drawable.ic_water_drop;
            case MAINTENANCE: return R.drawable.ic_construction;
            default: return R.drawable.ic_note;
        }
    }

    private int getExpenseIcon(ExpenseEntity.ExpenseCategory category) {
        switch (category) {
            case LAND_CLEARING: return R.drawable.ic_construction;
            case PLOWING: return R.drawable.ic_grain;
            case SEEDLINGS: return R.drawable.ic_grass;
            case LABOR: return R.drawable.ic_people;
            case SECURITY: return R.drawable.ic_security;
            case FENCING: return R.drawable.ic_fence;
            case FERTILIZER: return R.drawable.ic_fertilizer;
            case IRRIGATION: return R.drawable.ic_water_drop;
            case EQUIPMENT: return R.drawable.ic_construction;
            case MAINTENANCE: return R.drawable.ic_construction;
            default: return R.drawable.ic_money_off;
        }
    }

    private int getRevenueIcon(RevenueEntity.QualityGrade grade) {
        switch (grade) {
            case PREMIUM: return R.drawable.ic_star;
            case STANDARD: return R.drawable.ic_check_circle;
            case PROCESSING: return R.drawable.ic_factory;
            default: return R.drawable.ic_download;
        }
    }

    private String formatTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60 * 1000) {
            return "Just now";
        } else if (diff < 60 * 60 * 1000) {
            long minutes = diff / (60 * 1000);
            return minutes + " min ago";
        } else if (diff < 24 * 60 * 60 * 1000) {
            long hours = diff / (60 * 60 * 1000);
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (diff < 7 * 24 * 60 * 60 * 1000) {
            long days = diff / (24 * 60 * 60 * 1000);
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}