package com.afrinuts.farmos;

import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.ui.analytics.AnalyticsDashboardActivity;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.ui.blocks.BlocksActivity;
import com.afrinuts.farmos.ui.expenses.ExpensesListActivity;
import com.afrinuts.farmos.ui.farm.FarmHubActivity;
import com.afrinuts.farmos.ui.finance.FinanceHubActivity;
import com.afrinuts.farmos.ui.profit.ProfitDashboardActivity;
import com.afrinuts.farmos.ui.revenue.RevenuesListActivity;
import com.afrinuts.farmos.ui.tasks.TasksListActivity;
import com.afrinuts.farmos.ui.theme.CircularProgressView;
import com.afrinuts.farmos.ui.weather.WeatherActivity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;

import com.afrinuts.farmos.data.remote.WeatherResponse;
import com.afrinuts.farmos.data.repository.WeatherRepository;
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.view.MotionEvent;
import java.text.SimpleDateFormat;

import android.content.Intent;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.snackbar.Snackbar;
import com.afrinuts.farmos.data.sync.SyncService;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.ui.sync.SyncStatusReceiver;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.afrinuts.farmos.utils.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AfriNutsFarmOS";
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // Sync Components
    private SyncManager syncManager;
    private FirestoreSyncManager firestoreSyncManager;
    private SyncService syncService;
    private boolean isBound = false;
    private SyncStatusReceiver syncReceiver;
    private boolean isSyncInitialized = false;

    // UI Elements - Bottom Navigation
    private LinearLayout navHome;
    private LinearLayout navFarm;
    private LinearLayout navFinance;
    private LinearLayout navAnalytics;
    private ImageView navHomeIcon;
    private ImageView navFarmIcon;
    private ImageView navFinanceIcon;
    private ImageView navAnalyticsIcon;
    private TextView navHomeText;
    private TextView navFarmText;
    private TextView navFinanceText;
    private TextView navAnalyticsText;

    // UI Elements - Toolbar
    private ImageButton btnProfile;
    private TextView toolbarSyncStatus;
    private TextView toolbarUserName;
    private View syncStatusDot;
    private ImageButton btnToolbarSync;
    private ImageButton btnToolbarLogout;

    // Welcome Header
    private TextView welcomeUserName;
    private TextView locationText;

    private Handler greetingHandler;
    private Runnable greetingUpdater;

    // UI Elements
    private TextView greetingText;

    // Stats Cards
    private TextView totalHectaresText;
    private TextView totalTreesText;

    // Progress Ring
    private CircularProgressView progressRing;

    // Farm Summary Views
    private TextView blocksPlantedText;
    private TextView treesAliveText;
    private ProgressBar blocksProgress;
    private ProgressBar treesProgress;
    private TextView blocksPercentText;
    private TextView treesPercentText;

    // Financial Snapshot
    private TextView monthlyRevenueText;
    private TextView monthlyExpensesText;
    private TextView netProfitText;
    private TextView revenueTrend;
    private TextView expenseTrend;
    private TextView viewFinancialDetails;

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

    // Navigation state
    private int currentNavItem = 0; // 0=Home, 1=Farm, 2=Finance, 3=Analytics

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
        setContentView(R.layout.activity_main);

        // Initialize session manager
        sessionManager = new SessionManager(this);

        Log.d("MainActivity", "isLoggedIn: " + sessionManager.isLoggedIn());
        Log.d("MainActivity", "savedInstanceState: " + savedInstanceState);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            Log.d("MainActivity", "Not logged in, redirecting to Login");
            redirectToLogin();
            return;
        }


        initViews();
        setupBottomNavigation();
        setupToolbar();
        setupProfileButton();

        database = AppDatabase.getInstance(this);

        long defaultFarmId = 1L;
        blockRepository = new BlockRepository(this, defaultFarmId);
        expenseRepository = new ExpenseRepository(this, defaultFarmId);
        revenueRepository = new RevenueRepository(this, defaultFarmId);
        taskRepository = new TaskRepository(this, defaultFarmId);

        loadCurrentUser();
        ensureFarmExists();

        setupSyncManager();
        setupWeather();

        loadWeatherData();

        // Setup Financial Snapshot Click Listener
        setupFinancialSnapshotClick();

        startGreetingUpdater();
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

        // Shutdown executor gracefully
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
            try {
                // Wait up to 1 second for tasks to complete
                if (!databaseExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    databaseExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                databaseExecutor.shutdownNow();
            }
        }

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
    }

    // ========================================
    // UI INITIALIZATION
    // ========================================

    private void initViews() {
        // Bottom Navigation
        navHome = findViewById(R.id.navHome);
        navFarm = findViewById(R.id.navFarm);
        navFinance = findViewById(R.id.navFinance);
        navAnalytics = findViewById(R.id.navAnalytics);
        navHomeIcon = findViewById(R.id.navHomeIcon);
        navFarmIcon = findViewById(R.id.navFarmIcon);
        navFinanceIcon = findViewById(R.id.navFinanceIcon);
        navAnalyticsIcon = findViewById(R.id.navAnalyticsIcon);
        navHomeText = findViewById(R.id.navHomeText);
        navFarmText = findViewById(R.id.navFarmText);
        navFinanceText = findViewById(R.id.navFinanceText);
        navAnalyticsText = findViewById(R.id.navAnalyticsText);

        // Toolbar
        btnToolbarSync = findViewById(R.id.btnToolbarSync);
        btnToolbarLogout = findViewById(R.id.btnToolbarLogout);
        btnProfile = findViewById(R.id.btnProfile);
        toolbarSyncStatus = findViewById(R.id.toolbarSyncStatus);
        toolbarUserName = findViewById(R.id.toolbarUserName);
        syncStatusDot = findViewById(R.id.syncStatusDot);

        // Welcome Header
        welcomeUserName = findViewById(R.id.welcomeUserName);
        locationText = findViewById(R.id.locationText);
        greetingText = findViewById(R.id.greetingText);

        // Stats Cards
        totalHectaresText = findViewById(R.id.totalHectaresText);
        totalTreesText = findViewById(R.id.totalTreesText);

        // Progress Ring
        progressRing = findViewById(R.id.progressRing);

        // Farm Summary
        blocksPlantedText = findViewById(R.id.blocksPlantedText);
        treesAliveText = findViewById(R.id.treesAliveText);
        blocksProgress = findViewById(R.id.blocksProgress);
        treesProgress = findViewById(R.id.treesProgress);
        blocksPercentText = findViewById(R.id.blocksPercentText);
        treesPercentText = findViewById(R.id.treesPercentText);

        // Financial Snapshot
        monthlyRevenueText = findViewById(R.id.monthlyRevenueText);
        monthlyExpensesText = findViewById(R.id.monthlyExpensesText);
        netProfitText = findViewById(R.id.netProfitText);
        revenueTrend = findViewById(R.id.revenueTrend);
        expenseTrend = findViewById(R.id.expenseTrend);
        viewFinancialDetails = findViewById(R.id.viewFinancialDetails);

        // Recent Activity
        recentActivityContainer = findViewById(R.id.recentActivityContainer);

        // Weather
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

        // Offline Banner
        offlineBanner = findViewById(R.id.offlineBanner);
    }

    private void setupFinancialSnapshotClick() {
        CardView financialCard = findViewById(R.id.financialCard);
        if (financialCard != null) {
            financialCard.setOnClickListener(v -> {
                if (currentFarm != null) {
                    startActivity(new Intent(MainActivity.this, ProfitDashboardActivity.class));
                    // Optional: Add transition animation
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    Toast.makeText(this, "Farm not configured", Toast.LENGTH_SHORT).show();
                }
            });

            // Add hover effect for better UX
            financialCard.setOnTouchListener((v, event) -> {
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

        // Also handle the "View Details" text separately if needed
        if (viewFinancialDetails != null) {
            viewFinancialDetails.setOnClickListener(v -> {
                if (currentFarm != null) {
                    startActivity(new Intent(MainActivity.this, ProfitDashboardActivity.class));
                }
            });
        }
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> navigateToHome());
        navFarm.setOnClickListener(v -> navigateToFarm());
        navFinance.setOnClickListener(v -> navigateToFinance());
        navAnalytics.setOnClickListener(v -> navigateToAnalytics());

        // Set default selected state
        setNavItemSelected(0);
    }

    private void navigateToHome() {
        if (currentNavItem == 0) return;
        currentNavItem = 0;
        setNavItemSelected(0);
        // Show home content (already visible)
        // Refresh data
        loadFarmData();
    }

    private void navigateToFarm() {
        if (currentNavItem == 1) return;
        currentNavItem = 1;
        setNavItemSelected(1);
        startActivity(new Intent(MainActivity.this, FarmHubActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToFinance() {
        if (currentNavItem == 2) return;
        currentNavItem = 2;
        setNavItemSelected(2);
        startActivity(new Intent(MainActivity.this, FinanceHubActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToAnalytics() {
        if (currentNavItem == 3) return;
        currentNavItem = 3;
        setNavItemSelected(3);
        startActivity(new Intent(MainActivity.this, AnalyticsHubActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setNavItemSelected(int position) {
        // Get colors - WHITE for inactive, PRIMARY for active
        int inactiveColor = getThemeColor(R.attr.navbarIconColor);
        int activeColor = ContextCompat.getColor(this, R.color.primary);

        // Reset all icons
        navHomeIcon.setColorFilter(inactiveColor);
        navFarmIcon.setColorFilter(inactiveColor);
        navFinanceIcon.setColorFilter(inactiveColor);
        navAnalyticsIcon.setColorFilter(inactiveColor);

        // Reset all text colors
        navHomeText.setTextColor(inactiveColor);
        navFarmText.setTextColor(inactiveColor);
        navFinanceText.setTextColor(inactiveColor);
        navAnalyticsText.setTextColor(inactiveColor);

        // Set selected item
        switch (position) {
            case 0:
                navHomeIcon.setColorFilter(activeColor);
                navHomeText.setTextColor(activeColor);
                break;
            case 1:
                navFarmIcon.setColorFilter(activeColor);
                navFarmText.setTextColor(activeColor);
                break;
            case 2:
                navFinanceIcon.setColorFilter(activeColor);
                navFinanceText.setTextColor(activeColor);
                break;
            case 3:
                navAnalyticsIcon.setColorFilter(activeColor);
                navAnalyticsText.setTextColor(activeColor);
                break;
        }
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void setupToolbar() {
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

    private void setupProfileButton() {
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> showProfileMenu());
        }
    }

    private void showProfileMenu() {
        String[] options;
        if (sessionManager.isOwner()) {
            options = new String[]{"Team", "Settings"};
        } else {
            options = new String[]{"Settings"};
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Account")
                .setItems(options, (dialog, which) -> {
                    if (options[which].equals("Team")) {
                        if (currentFarm != null) {
                            Intent intent = new Intent(MainActivity.this, com.afrinuts.farmos.ui.users.UsersListActivity.class);
                            startActivity(intent);
                        }
                    } else if (options[which].equals("Settings")) {
                        startActivity(new Intent(MainActivity.this, com.afrinuts.farmos.ui.settings.SettingsActivity.class));
                    }
                })
                .show();
    }

    // ========================================
    // USER & SESSION
    // ========================================

    private void loadCurrentUser() {
        // First check if session is valid
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "Session not logged in, redirecting to login");
            redirectToLogin();
            return;
        }

        long userId = sessionManager.getUserId();
        String userUid = sessionManager.getUserUid();

        Log.d(TAG, "Loading user - ID: " + userId + ", UID: " + userUid);

        // Validate user ID
        if (userId <= 0) {
            Log.e(TAG, "Invalid user ID: " + userId);
            sessionManager.logout();
            redirectToLogin();
            return;
        }

        databaseExecutor.execute(() -> {
            UserEntity user = database.userDao().getUserById(userId);
            runOnUiThread(() -> {
                if (user == null) {
                    Log.e(TAG, "User ID " + userId + " not found in local database");

                    // Only try to recover if we have a valid UID
                    if (userUid != null && !userUid.isEmpty() && !"null".equals(userUid)) {
                        fetchUserFromFirestore(userUid);
                    } else {
                        Log.e(TAG, "No valid UID for recovery, logging out");
                        sessionManager.logout();
                        redirectToLogin();
                    }
                } else {
                    currentUser = user;

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

                    // ✅ ADD THIS: Initialize greeting
                    TextView greetingText = findViewById(R.id.greetingText);
                    if (greetingText != null) {
                        greetingText.setText(getTimeBasedGreeting());
                    }

                    // Only initialize sync if user is active
                    if (currentUser.isActive()) {
                        initializeSyncListener();
                    } else {
                        Log.e(TAG, "User is inactive, logging out");
                        sessionManager.logout();
                        redirectToLogin();
                    }
                }
            });
        });
    }

    private String getTimeBasedGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        if (hourOfDay >= 5 && hourOfDay < 12) {
            return "Good morning";
        } else if (hourOfDay >= 12 && hourOfDay < 17) {
            return "Good afternoon";
        } else if (hourOfDay >= 17 && hourOfDay < 21) {
            return "Good evening";
        } else {
            return "Good night";
        }
    }

    private void startGreetingUpdater() {
        greetingHandler = new Handler(Looper.getMainLooper());
        greetingUpdater = new Runnable() {
            @Override
            public void run() {
                TextView greetingText = findViewById(R.id.greetingText);
                if (greetingText != null) {
                    greetingText.setText(getTimeBasedGreeting());
                }
                // Update every minute (60000 ms) or you can use 3600000 for hourly
                greetingHandler.postDelayed(this, 60000);
            }
        };
        greetingHandler.post(greetingUpdater);
    }

    private void stopGreetingUpdater() {
        if (greetingHandler != null && greetingUpdater != null) {
            greetingHandler.removeCallbacks(greetingUpdater);
            greetingHandler = null;
            greetingUpdater = null;
        }
    }

    private void fetchUserFromFirestore(String userUid) {
        Log.d(TAG, "Attempting to recover user from Firestore with UID: " + userUid);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isActive = documentSnapshot.getBoolean("isActive");
                        if (isActive != null && !isActive) {
                            Log.e(TAG, "User is inactive in Firestore");
                            sessionManager.logout();
                            redirectToLogin();
                            return;
                        }

                        UserEntity user = documentToUser(documentSnapshot);
                        if (user != null) {
                            // Save to local database
                            databaseExecutor.execute(() -> {
                                // Check if user already exists to avoid duplicates
                                UserEntity existing = database.userDao().getUserByUid(user.getUid());
                                long id;
                                if (existing != null) {
                                    // Update existing
                                    user.setLocalId(existing.getLocalId());
                                    database.userDao().update(user);
                                    id = existing.getLocalId();
                                    Log.d(TAG, "Updated existing user from Firestore: " + user.getName());
                                } else {
                                    // Insert new
                                    id = database.userDao().insert(user);
                                    user.setLocalId(id);
                                    Log.d(TAG, "Created new user from Firestore: " + user.getName());
                                }

                                final long finalId = id;
                                runOnUiThread(() -> {
                                    currentUser = user;

                                    // Update session with correct local ID
                                    sessionManager.logout(); // Clear old session
                                    sessionManager.createLoginSession(user);

                                    String firstName = user.getName().split(" ")[0];
                                    if (toolbarUserName != null) {
                                        toolbarUserName.setText("Hi, " + firstName);
                                    }
                                    if (welcomeUserName != null) {
                                        welcomeUserName.setText(firstName);
                                    }

                                    Toast.makeText(this, "Welcome back, " + firstName, Toast.LENGTH_SHORT).show();
                                    initializeSyncListener();
                                });
                            });
                        } else {
                            Log.e(TAG, "Failed to convert user from Firestore");
                            sessionManager.logout();
                            redirectToLogin();
                        }
                    } else {
                        Log.e(TAG, "User not found in Firestore");
                        sessionManager.logout();
                        redirectToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user from Firestore", e);
                    sessionManager.logout();
                    redirectToLogin();
                });
    }

    private UserEntity documentToUser(DocumentSnapshot doc) {
        try {
            String uid = doc.getId();
            String name = doc.getString("name");
            String roleStr = doc.getString("role");
            String phone = doc.getString("phone");
            String email = doc.getString("email");
            String authMethodStr = doc.getString("authMethod");
            Boolean isActive = doc.getBoolean("isActive");
            Long createdAt = doc.getLong("createdAt");
            Long localId = doc.getLong("localId");

            // ✅ IMPORTANT: Get password hash from Firestore
            String passwordHash = doc.getString("passwordHash");

            UserEntity.UserRole role;
            try {
                role = UserEntity.UserRole.valueOf(roleStr);
            } catch (Exception e) {
                Log.e(TAG, "Invalid role: " + roleStr);
                return null;
            }

            Object farmIdObj = doc.get("farmId");
            long farmId = 1;
            if (farmIdObj instanceof Long) {
                farmId = (Long) farmIdObj;
            } else if (farmIdObj instanceof String) {
                try {
                    farmId = Long.parseLong((String) farmIdObj);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid farmId format");
                }
            }

            UserEntity user;

            if (UserEntity.AuthMethod.PASSWORD.name().equals(authMethodStr)) {
                // ✅ For password users, use the Firebase Auth constructor
                user = new UserEntity(
                        uid,
                        name,
                        role,
                        phone,
                        email,
                        passwordHash != null ? passwordHash : "", // Use password hash from Firestore
                        farmId
                );
            } else {
                // For PIN users
                user = new UserEntity(
                        name,
                        role,
                        phone,
                        "", // pinHash will be set separately
                        farmId
                );
                user.setUid(uid);
            }

            user.setAuthMethod(UserEntity.AuthMethod.valueOf(authMethodStr));
            user.setActive(isActive != null ? isActive : true);
            if (createdAt != null) user.setCreatedAt(createdAt);
            if (localId != null && localId > 0) user.setLocalId(localId);

            // ✅ For password users, ensure password hash is set
            if (UserEntity.AuthMethod.PASSWORD.name().equals(authMethodStr) && passwordHash != null) {
                user.setPasswordHash(passwordHash);
            }

            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to user", e);
            return null;
        }
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
        // ✅ Check if executor is still alive
        if (databaseExecutor == null || databaseExecutor.isShutdown() || databaseExecutor.isTerminated()) {
            Log.w(TAG, "loadFarmData called after executor shutdown");
            return;
        }

        try {
            databaseExecutor.execute(() -> {
                try {
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
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading farm data", e);
                }
            });
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "loadFarmData rejected - executor already shut down", e);
        }
    }

    private void updateUI(int totalBlocks, int plantedBlocks, int totalAliveTrees,
                          double avgSurvivalRate, int plantedProgressValue) {

        // Farm details (only for views that exist in new layout)
        if (currentFarm != null) {
            // Update Farm Summary Card
            int blocksPercent = totalBlocks > 0 ? (plantedBlocks * 100 / totalBlocks) : 0;
            int totalTrees = (int)(currentFarm.getCashewHectares() * currentFarm.getTreesPerHectare());
            int treesPercent = totalTrees > 0 ? (totalAliveTrees * 100 / totalTrees) : 0;

            blocksPlantedText.setText(String.format(Locale.getDefault(),
                    "%d/%d", plantedBlocks, totalBlocks));
            treesAliveText.setText(String.format(Locale.getDefault(),
                    "%d/%d", totalAliveTrees, totalTrees));

            blocksProgress.setProgress(blocksPercent);
            treesProgress.setProgress(treesPercent);

            blocksPercentText.setText(blocksPercent + "%");
            treesPercentText.setText(treesPercent + "%");

            // Update stats cards
            totalHectaresText.setText(String.format(Locale.getDefault(),
                    "%.0f", currentFarm.getTotalHectares()));

            totalTreesText.setText(String.format(Locale.getDefault(),
                    "%,d", totalTrees));

            // Update welcome header
            welcomeUserName.setText(sessionManager.getUserName() != null ?
                    sessionManager.getUserName() : "Farmer");

            // Update progress ring (circular progress)
            updateProgressRing();

            // Update financial snapshot (revenue, expenses, profit)
            updateFinancialSnapshot();

            // Load recent activity
            loadRecentActivity();
        }
    }

    private void updateProgressRing() {
        if (allBlocks == null || progressRing == null) return;

        int totalBlocks = allBlocks.size();
        int plantedBlocks = 0;

        for (BlockEntity block : allBlocks) {
            if (block.isPlanted()) {
                plantedBlocks++;
            }
        }

        int progressPercent = totalBlocks > 0 ? (plantedBlocks * 100 / totalBlocks) : 0;
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
                    if (count >= 3) break;
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
                    if (count >= 3) break;
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
                    if (count >= 3) break;
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
                    if (toolbarSyncStatus != null) {
                        toolbarSyncStatus.setText("Synced");
                        toolbarSyncStatus.setVisibility(View.VISIBLE);
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

        if (weatherLocationDetail != null && location != null) {
            weatherLocationDetail.setText(location.getRegion() != null ?
                    location.getRegion() : "Côte d'Ivoire");
        }

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
                    // Clear everything before logout
                    sessionManager.logout();

                    // Also sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // Clear any pending sync
                    if (syncManager != null) {
                        syncManager.shutdown();
                    }

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