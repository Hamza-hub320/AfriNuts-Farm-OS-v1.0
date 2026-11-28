package com.afrinuts.farmos;

import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.ui.farm.FarmHubActivity;
import com.afrinuts.farmos.ui.finance.FinanceHubActivity;

import com.afrinuts.farmos.data.repository.BlockRepository;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QuerySnapshot;
import com.afrinuts.farmos.ui.compliance.ComplianceHubActivity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import java.text.SimpleDateFormat;

import android.content.Intent;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.afrinuts.farmos.data.sync.SyncService;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.ui.sync.SyncStatusReceiver;
import android.os.Bundle;
import android.util.Log;
import android.widget.PopupWindow;
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
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.ui.theme.HomeChartView;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.utils.SessionManager;
import com.afrinuts.farmos.ui.notifications.NotificationBell;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private static final int SUPPLIER_GOAL = 1000;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // Sync Components
    private SyncManager syncManager;
    private FirestoreSyncManager firestoreSyncManager;
    private SyncService syncService;
    private boolean isBound = false;
    private SyncStatusReceiver syncReceiver;
    private boolean isSyncInitialized = false;

    private static ExecutorService backgroundExecutor;

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
    private LinearLayout navCompliance;
    private ImageView navComplianceIcon;
    private TextView navComplianceText;

    // UI Elements - Toolbar
    private View btnProfile;
    private TextView toolbarSyncStatus;
    private TextView toolbarUserName;
    private View syncStatusDot;
    private ImageButton btnToolbarSync;
    private ImageButton homeNotificationButton;
    private TextView homeNotificationBadge;

    // Financial Snapshot
    private TextView monthlyRevenueText;
    private TextView monthlyExpensesText;
    private TextView netProfitText;
    private TextView revenueTrend;
    private TextView expenseTrend;

    // Recent Activity
    private LinearLayout recentActivityContainer;

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

    // Navigation state
    // Change from 3 to 4
    private int currentNavItem = 0; // 0=Home, 1=Farm, 2=Finance, 3=Analytics, 4=Compliance

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFarmData();

        if (sessionManager.isLoggedIn()) {
            loadCurrentUser();
        }
        NotificationBell.refresh(this, homeNotificationBadge);
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
        navCompliance = findViewById(R.id.navCompliance);
        navHomeIcon = findViewById(R.id.navHomeIcon);
        navFarmIcon = findViewById(R.id.navFarmIcon);
        navFinanceIcon = findViewById(R.id.navFinanceIcon);
        navAnalyticsIcon = findViewById(R.id.navAnalyticsIcon);
        navComplianceIcon = findViewById(R.id.navComplianceIcon);
        navHomeText = findViewById(R.id.navHomeText);
        navFarmText = findViewById(R.id.navFarmText);
        navFinanceText = findViewById(R.id.navFinanceText);
        navAnalyticsText = findViewById(R.id.navAnalyticsText);
        navComplianceText = findViewById(R.id.navComplianceText);



        // Toolbar
        btnToolbarSync = findViewById(R.id.btnToolbarSync);
        homeNotificationButton = findViewById(R.id.homeNotificationButton);
        btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile instanceof TextView) ((TextView) btnProfile).setText(new UserEntity(sessionManager.getUserName(), null, null, null, null).getInitials());
        toolbarSyncStatus = findViewById(R.id.toolbarSyncStatus);
        syncStatusDot = findViewById(R.id.syncStatusDot);

        // Welcome Header
        TextView overviewDateText = findViewById(R.id.overviewDateText);
        if (overviewDateText != null) {
            overviewDateText.setText(new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date()) + " · Odienné");
        }

        // Financial Snapshot

        // Recent Activity

        // Offline Banner
        offlineBanner = findViewById(R.id.offlineBanner);
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> navigateToHome());
        navFarm.setOnClickListener(v -> navigateToFarm());
        navFinance.setOnClickListener(v -> navigateToFinance());
        navAnalytics.setOnClickListener(v -> navigateToAnalytics());
        navCompliance.setOnClickListener(v -> navigateToCompliance());

        // Set default selected state
        setNavItemSelected(0);
    }

    private void navigateToCompliance() {
        if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(4, sessionManager.getUserRole())) {
            android.widget.Toast.makeText(this, com.afrinuts.farmos.utils.AccessMap.hubDeniedMessage(4), android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (currentNavItem == 4) return;
        currentNavItem = 4;
        setNavItemSelected(4);

        Intent intent = new Intent(MainActivity.this, ComplianceHubActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void navigateToHome() {
        if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(0, sessionManager.getUserRole())) {
            Toast.makeText(this, com.afrinuts.farmos.utils.AccessMap.hubDeniedMessage(0), Toast.LENGTH_LONG).show();
            return;
        }
        if (currentNavItem == 0) return;
        currentNavItem = 0;
        setNavItemSelected(0);
        // Show home content (already visible)
        // Refresh data
        loadFarmData();
    }

    private void navigateToFarm() {
        if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(1, sessionManager.getUserRole())) {
            android.widget.Toast.makeText(this, com.afrinuts.farmos.utils.AccessMap.hubDeniedMessage(1), android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (currentNavItem == 1) return;
        currentNavItem = 1;
        setNavItemSelected(1);
        startActivity(new Intent(MainActivity.this, FarmHubActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToFinance() {
        if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(2, sessionManager.getUserRole())) {
            android.widget.Toast.makeText(this, com.afrinuts.farmos.utils.AccessMap.hubDeniedMessage(2), android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (currentNavItem == 2) return;
        currentNavItem = 2;
        setNavItemSelected(2);
        startActivity(new Intent(MainActivity.this, FinanceHubActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToAnalytics() {
        if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(3, sessionManager.getUserRole())) {
            android.widget.Toast.makeText(this, com.afrinuts.farmos.utils.AccessMap.hubDeniedMessage(3), android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (currentNavItem == 3) return;
        currentNavItem = 3;
        setNavItemSelected(3);
        startActivity(new Intent(MainActivity.this, AnalyticsHubActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setNavItemSelected(int position) {
        int inactiveColor = Color.rgb(201, 194, 180);
        int activeColor = ContextCompat.getColor(this, android.R.color.white);
        setHomeNavState(navHome, navHomeIcon, navHomeText, position == 0, inactiveColor, activeColor);
        setHomeNavState(navFarm, navFarmIcon, navFarmText, position == 1, inactiveColor, activeColor);
        setHomeNavState(navFinance, navFinanceIcon, navFinanceText, position == 2, inactiveColor, activeColor);
        setHomeNavState(navAnalytics, navAnalyticsIcon, navAnalyticsText, position == 3, inactiveColor, activeColor);
        setHomeNavState(navCompliance, navComplianceIcon, navComplianceText, position == 4, inactiveColor, activeColor);
    }

    private void setHomeNavState(LinearLayout item, ImageView icon, TextView label, boolean active,
                                 int inactiveColor, int activeColor) {
        item.setOrientation(active ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        item.setGravity(android.view.Gravity.CENTER);
        item.setPadding(active ? dp(16) : 0, 0, active ? dp(16) : 0, 0);
        item.setBackgroundResource(active ? R.drawable.bg_nav_active_pill : 0);
        LinearLayout.LayoutParams fixedItem = (LinearLayout.LayoutParams) item.getLayoutParams();
        fixedItem.height = dp(48);
        item.setLayoutParams(fixedItem);
        ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
        iconParams.width = dp(20);
        iconParams.height = dp(20);
        icon.setLayoutParams(iconParams);
        label.setTextSize(14);
        label.setTypeface(Typeface.create(ResourcesCompat.getFont(this, R.font.manrope_extrabold), Typeface.NORMAL));
        label.setVisibility(active ? View.VISIBLE : View.GONE);
        label.setTextColor(active ? activeColor : inactiveColor);
        label.setAlpha(1f);
        LinearLayout.LayoutParams labelParams = (LinearLayout.LayoutParams) label.getLayoutParams();
        labelParams.topMargin = active ? 0 : 4;
        labelParams.setMarginStart(active ? dp(7) : 0);
        label.setLayoutParams(labelParams);
        icon.setColorFilter(active ? activeColor : inactiveColor);
        ViewGroup.LayoutParams raw = item.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams itemParams = (LinearLayout.LayoutParams) raw;
            itemParams.width = 0;
            itemParams.weight = active ? 1.8f : 0.8f;
            item.setLayoutParams(itemParams);
        }
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void setupToolbar() {
        homeNotificationBadge = NotificationBell.wire(this, homeNotificationButton);
        NotificationBell.refresh(this, homeNotificationBadge);
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

    }

    private void setupProfileButton() {
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> showProfileMenu());
        }
    }

    private void showProfileMenu() {
        if (btnProfile == null) return;
        PopupWindow popup = new PopupWindow(this);
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(20));
        background.setStroke(dp(1), 0xFFE9E1CF);
        menu.setBackground(background);
        menu.setElevation(dp(8));

        if (sessionManager.canManageUsers()) {
            addAccountMenuItem(menu, popup, "Team", R.drawable.ic_person_add, 0xFF1C1B1A, false, () -> {
                if (currentFarm != null) {
                    startActivity(new Intent(MainActivity.this, com.afrinuts.farmos.ui.users.UsersListActivity.class));
                }
            });
        }
        addAccountMenuItem(menu, popup, "Settings", R.drawable.ic_settings, 0xFF1C1B1A, false,
                () -> startActivity(new Intent(MainActivity.this, com.afrinuts.farmos.ui.settings.SettingsActivity.class)));
        if (sessionManager.isOwner() || sessionManager.isCEO() || sessionManager.isManager())
            addAccountMenuItem(menu, popup, "Sync Center", R.drawable.ic_sync, 0xFF1C1B1A, false,
                    () -> startActivity(new Intent(MainActivity.this, com.afrinuts.farmos.ui.sync.SyncCenterActivity.class)));
        View divider = new View(this);
        divider.setBackgroundColor(0xFFE9E1CF);
        menu.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        addAccountMenuItem(menu, popup, "Logout", R.drawable.ic_logout, 0xFFB3402A, true,
                this::showLogoutDialog);

        popup.setContentView(menu);
        popup.setWidth(dp(196));
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setElevation(dp(8));
        popup.showAsDropDown(btnProfile, -dp(152), dp(8));
    }

    private void addAccountMenuItem(LinearLayout menu, PopupWindow popup, String title, int iconRes,
                                    int textColor, boolean bold, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), 0, dp(12), 0);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0xFFEFEAE2),
                new ColorDrawable(Color.TRANSPARENT), null));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(textColor);
        row.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(textColor);
        label.setTextSize(15);
        label.setTypeface(Typeface.create(ResourcesCompat.getFont(this, bold ? R.font.manrope : R.font.manrope_medium),
                bold ? Typeface.BOLD : Typeface.NORMAL));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMarginStart(dp(12));
        row.addView(label, labelParams);

        menu.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        row.setOnClickListener(v -> {
            popup.dismiss();
            action.run();
        });
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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
            String bio = doc.getString("bio");
            String company = doc.getString("company");
            String title = doc.getString("title");
            Long lastLoginAt = doc.getLong("lastLoginAt");
            String lastLoginIp = doc.getString("lastLoginIp");
            String lastLoginDevice = doc.getString("lastLoginDevice");
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
            user.setBio(bio);
            user.setCompany(company);
            user.setTitle(title);
            if (lastLoginAt != null) user.setLastLoginAt(lastLoginAt);
            user.setLastLoginIp(lastLoginIp);
            user.setLastLoginDevice(lastLoginDevice);

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
                runProductTypeBackfillBeforeListening(farmId, () ->
                        runSupplierActivationRepairBeforeListening(farmId, () -> {
                            firestoreSyncManager.startListening(String.valueOf(farmId), sessionManager.isOwner());

                            loadFarmData();

                            if (currentUser != null) {
                                initializeSyncListener();
                            }

                            if (sessionManager.isOwner()) {
                                loadUsersFromFirestore();
                            }
                        }));

            });
        });
    }

    private void runProductTypeBackfillBeforeListening(long farmId, Runnable onComplete) {
        databaseExecutor.execute(() -> {
            android.content.SharedPreferences repairs =
                    getSharedPreferences("farm_os_repairs", MODE_PRIVATE);
            String doneKey = "product_type_backfill_v1_done";
            if (repairs.getBoolean(doneKey, false)) {
                runOnUiThread(onComplete);
                return;
            }

            syncManager.setSyncCenterContext(farmId, null);
            String[] batchNumbers = {
                    "AFN-COOP-CONV-260710-4A76",
                    "AFN-COOP-ORG-260710-C97E",
                    "AFN-FARM-ORG-260710-34D1",
                    "AFN-INDP-CONV-261108-DAF0D5",
                    "AFN-INDP-CONV-261111-0BE9A9",
                    "AFN-INDP-CONV-261121-1FB0B8",
                    "AFN-INDP-CONV-261127-E3A3E2",
                    "AFN-INDP-ORG-260710-8DD0"
            };

            try {
                for (String batchNumber : batchNumbers) {
                    try {
                        QualityBatchEntity batch = database.qualityBatchDao().getBatchByNumber(batchNumber);
                        if (batch == null) {
                            Log.i(TAG, "Backfill: batch " + batchNumber + " not found on this device");
                            continue;
                        }
                        String productType = batch.getProductType();
                        if (productType == null || productType.trim().isEmpty()) {
                            batch.setProductType("RCN");
                            batch.setUpdatedAt(System.currentTimeMillis());
                            database.qualityBatchDao().update(batch);
                            syncManager.queueForSync("quality_batch", batch.getId(),
                                    com.afrinuts.farmos.data.local.entity.SyncEntity.SyncAction.UPDATE);
                            Log.i(TAG, "Backfill: repaired batch " + batchNumber);
                        } else {
                            Log.i(TAG, "Backfill: batch " + batchNumber
                                    + " already set to " + productType);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Backfill: failed for batch " + batchNumber, e);
                    }
                }
            } finally {
                repairs.edit().putBoolean(doneKey, true).apply();
                syncManager.triggerAutoSync();
                runOnUiThread(onComplete);
            }
        });
    }

    private void runSupplierActivationRepairBeforeListening(long farmId, Runnable onComplete) {
        databaseExecutor.execute(() -> {
            syncManager.setSyncCenterContext(farmId, null);
            SupplierRepository supplierRepository = new SupplierRepository(this, farmId);
            QualityBatchRepository batchRepository = new QualityBatchRepository(this, farmId);
            List<SupplierEntity> suppliersToActivate = new ArrayList<>();
            try {
                for (SupplierEntity supplier : supplierRepository.getSuppliersSync()) {
                    if (supplier == null || supplier.getStatus() != SupplierEntity.SupplierStatus.PENDING) {
                        continue;
                    }
                    List<QualityBatchEntity> batches =
                            batchRepository.getBatchesBySupplierIdSync(supplier.getId());
                    if (batches != null && !batches.isEmpty()) {
                        suppliersToActivate.add(supplier);
                    } else {
                        Log.i(TAG, "Activation repair: " + supplier.getName() + " ("
                                + supplier.getId() + ") left Pending — no deliveries");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Activation repair: failed while checking suppliers", e);
                runOnUiThread(onComplete);
                return;
            }

            activateNextPendingSupplier(supplierRepository, suppliersToActivate, 0,
                    onComplete);
        });
    }

    private void activateNextPendingSupplier(SupplierRepository supplierRepository,
            List<SupplierEntity> suppliers, int index, Runnable onComplete) {
        if (index >= suppliers.size()) {
            runOnUiThread(onComplete);
            return;
        }

        SupplierEntity supplier = suppliers.get(index);
        supplier.setStatus(SupplierEntity.SupplierStatus.ACTIVE);
        supplier.setActive(true);
        supplier.setUpdatedAt(System.currentTimeMillis());
        supplierRepository.updateSupplier(supplier, new SupplierRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.i(TAG, "Activation repair: " + supplier.getName() + " set Active");
                activateNextPendingSupplier(supplierRepository, suppliers, index + 1, onComplete);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Activation repair: failed for " + supplier.getName()
                        + " (" + supplier.getId() + "): " + error);
                activateNextPendingSupplier(supplierRepository, suppliers, index + 1, onComplete);
            }
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
                        loadHomeQualityMetrics(farm.getId());

                        final FarmEntity finalFarm = farm;
                        final List<BlockEntity> finalBlocks = blocks;
                        runOnUiThread(() -> {
                            currentFarm = finalFarm;
                            allBlocks = finalBlocks;
                            updateHomeBlockHeatmap(finalBlocks);
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

    private void updateHomeBlockHeatmap(List<BlockEntity> blocks) {
        TextView averageSurvival = findViewById(R.id.homeAverageBlockSurvival);
        LinearLayout grid = findViewById(R.id.homeBlockHeatmapGrid);
        LinearLayout legend = findViewById(R.id.homeBlockHeatmapLegend);
        if (averageSurvival == null || grid == null || legend == null) return;

        int plantedCount = 0;
        double survivalTotal = 0;
        if (blocks != null) {
            for (BlockEntity block : blocks) {
                if (block != null && block.isPlanted()) {
                    plantedCount++;
                    survivalTotal += block.getSurvivalRate();
                }
            }
        }
        averageSurvival.setText(plantedCount == 0
                ? "—%"
                : String.format(Locale.getDefault(), "%.0f%%", survivalTotal / plantedCount));

        float density = getResources().getDisplayMetrics().density;
        int configuredWidthDp = getResources().getConfiguration().screenWidthDp;
        boolean tablet = configuredWidthDp >= 600;
        int gridWidthDp = Math.min(tablet ? 420 : 344, Math.max(36, configuredWidthDp - 80));
        int cellSize = Math.max(1, Math.min(Math.round((tablet ? 52 : 42) * density),
                (int) ((gridWidthDp - 36f) * density / 7f)));
        int gap = Math.round((tablet ? 8 : 6) * density);
        grid.removeAllViews();

        BlockEntity[] blocksByGridPosition = new BlockEntity[35];
        if (blocks != null) {
            for (BlockEntity block : blocks) {
                if (block == null || block.getBlockName() == null) continue;
                String name = block.getBlockName().trim().toUpperCase(Locale.ROOT);
                if (name.length() < 2) continue;
                char row = name.charAt(0);
                int column;
                try {
                    column = Integer.parseInt(name.substring(1));
                } catch (NumberFormatException ignored) {
                    continue;
                }
                int rowIndex = row - 'A';
                if (rowIndex >= 0 && rowIndex < 5 && column >= 1 && column <= 7) {
                    blocksByGridPosition[rowIndex * 7 + column - 1] = block;
                }
            }
        }

        for (int rowIndex = 0; rowIndex < 5; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, cellSize);
            if (rowIndex > 0) rowParams.topMargin = gap;
            grid.addView(row, rowParams);

            for (int column = 0; column < 7; column++) {
                int index = rowIndex * 7 + column;
                String blockName = String.valueOf((char) ('A' + rowIndex)) + (column + 1);
                BlockEntity block = blocksByGridPosition[index];
                BlockEntity.BlockStatus status = block == null || block.getStatus() == null
                        ? BlockEntity.BlockStatus.NOT_CLEARED : block.getStatus();
                TextView cell = new TextView(this);
                cell.setText(blockName);
                cell.setTextColor(Color.WHITE);
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, tablet ? 13 : 11);
                cell.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_extrabold));
                cell.setGravity(android.view.Gravity.CENTER);
                cell.setIncludeFontPadding(false);
                cell.setBackground(createHeatmapCellBackground(status));
                cell.setContentDescription(blockName + " · " + status.getDisplayName());
                if (block != null) {
                    cell.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this,
                                com.afrinuts.farmos.ui.farm.FarmHubActivity.class);
                        intent.putExtra(com.afrinuts.farmos.ui.farm.FarmHubActivity.EXTRA_OPEN_BLOCK_ID,
                                block.getId());
                        startActivity(intent);
                    });
                } else {
                    cell.setClickable(false);
                    cell.setFocusable(false);
                }
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(cellSize, cellSize);
                if (column > 0) cellParams.leftMargin = gap;
                row.addView(cell, cellParams);
            }
        }

        legend.removeAllViews();
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            BlockEntity.BlockStatus[] tabletLegendOrder = {
                    BlockEntity.BlockStatus.PLANTED,
                    BlockEntity.BlockStatus.NOT_CLEARED,
                    BlockEntity.BlockStatus.CLEARED,
                    BlockEntity.BlockStatus.PLOWED,
                    BlockEntity.BlockStatus.HARVESTED
            };
            for (BlockEntity.BlockStatus status : tabletLegendOrder) {
                addHeatmapLegendRow(legend, new BlockEntity.BlockStatus[]{status}, density);
            }
        } else {
            addHeatmapLegendRow(legend, new BlockEntity.BlockStatus[]{
                    BlockEntity.BlockStatus.PLANTED,
                    BlockEntity.BlockStatus.NOT_CLEARED,
                    BlockEntity.BlockStatus.CLEARED
            }, density);
            addHeatmapLegendRow(legend, new BlockEntity.BlockStatus[]{
                    BlockEntity.BlockStatus.PLOWED,
                    BlockEntity.BlockStatus.HARVESTED
            }, density);
        }
    }

    private android.graphics.drawable.GradientDrawable createHeatmapCellBackground(
            BlockEntity.BlockStatus status) {
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(getHeatmapStatusColor(status));
        background.setCornerRadius(8 * getResources().getDisplayMetrics().density);
        return background;
    }

    private int getHeatmapStatusColor(BlockEntity.BlockStatus status) {
        if (status == null) status = BlockEntity.BlockStatus.HARVESTED;
        switch (status) {
            case NOT_CLEARED:
                return ContextCompat.getColor(this, android.R.color.holo_red_dark);
            case CLEARED:
                return ContextCompat.getColor(this, android.R.color.holo_orange_dark);
            case PLOWED:
                return ContextCompat.getColor(this, android.R.color.holo_blue_dark);
            case PLANTED:
                return ContextCompat.getColor(this, android.R.color.holo_green_dark);
            default:
                return ContextCompat.getColor(this, R.color.primary);
        }
    }

    private void addHeatmapLegendRow(LinearLayout legend, BlockEntity.BlockStatus[] statuses,
                                    float density) {
        boolean tablet = getResources().getConfiguration().screenWidthDp >= 600;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (legend.getChildCount() > 0) rowParams.topMargin = Math.round(8 * density);
        legend.addView(row, rowParams);

        for (int i = 0; i < statuses.length; i++) {
            BlockEntity.BlockStatus status = statuses[i];
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) itemParams.leftMargin = Math.round(14 * density);
            row.addView(item, itemParams);

            View swatch = new View(this);
            android.graphics.drawable.GradientDrawable swatchBackground =
                    new android.graphics.drawable.GradientDrawable();
            swatchBackground.setColor(getHeatmapStatusColor(status));
            swatchBackground.setCornerRadius(3 * density);
            swatch.setBackground(swatchBackground);
            item.addView(swatch, new LinearLayout.LayoutParams(
                    Math.round((tablet ? 12 : 10) * density),
                    Math.round((tablet ? 12 : 10) * density)));

            TextView label = new TextView(this);
            label.setText(status.getDisplayName());
            label.setTextColor(Color.parseColor("#3D3A35"));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, tablet ? 14 : 12);
            label.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
            label.setIncludeFontPadding(false);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelParams.leftMargin = Math.round(6 * density);
            item.addView(label, labelParams);
        }
    }

    private void loadHomeQualityMetrics(long farmId) {
        if (databaseExecutor == null || databaseExecutor.isShutdown()) return;
        try {
            databaseExecutor.execute(() -> {
                try {
                    Calendar now = Calendar.getInstance();
                    Calendar start = (Calendar) now.clone();
                    start.set(Calendar.DAY_OF_YEAR, 1);
                    start.set(Calendar.HOUR_OF_DAY, 0);
                    start.set(Calendar.MINUTE, 0);
                    start.set(Calendar.SECOND, 0);
                    start.set(Calendar.MILLISECOND, 0);
                    long startOfYear = start.getTimeInMillis();
                    Calendar end = (Calendar) start.clone();
                    end.add(Calendar.YEAR, 1);
                    long endOfYear = end.getTimeInMillis();
                    List<QualityBatchEntity> batches = database.qualityBatchDao().getBatchesByFarmId(farmId);
                    List<SupplierEntity> suppliers = database.supplierDao().getSuppliersByFarmId(farmId);
                    double rcnKg = 0;
                    float[] monthlyKg = new float[12];
                    int premium = 0, standard = 0, reject = 0;
                    for (QualityBatchEntity batch : batches) {
                        if (batch == null || !batch.isRcn()) continue;
                        long date = batch.getProcessingDate();
                        if (date < startOfYear || date >= endOfYear) continue;
                        double kg = Math.max(0, batch.getWeightKg());
                        rcnKg += kg;
                        Calendar batchDate = Calendar.getInstance();
                        batchDate.setTimeInMillis(date);
                        int month = batchDate.get(Calendar.MONTH);
                        monthlyKg[month] += (float) kg;
                        QualityBatchEntity.QualityGrade grade = batch.getQualityGrade();
                        if (grade == QualityBatchEntity.QualityGrade.PREMIUM) premium++;
                        else if (grade == QualityBatchEntity.QualityGrade.STANDARD) standard++;
                        else if (grade == QualityBatchEntity.QualityGrade.REJECT) reject++;
                    }
                    int graded = premium + standard + reject;
                    int onboarded = 0;
                    for (SupplierEntity supplier : suppliers) {
                        if (supplier.getStatus() == SupplierEntity.SupplierStatus.ACTIVE) onboarded++;
                    }
                    final double finalRcnKg = rcnKg;
                    final float[] finalMonthlyKg = monthlyKg;
                    final int finalPremium = premium, finalStandard = standard, finalReject = reject;
                    final int finalGraded = graded, finalOnboarded = onboarded;
                    final int finalSupplierTotal = SUPPLIER_GOAL;
                    runOnUiThread(() -> {
                        TextView rcn = findViewById(R.id.homeRcnNumber);
                        TextView premiumPercent = findViewById(R.id.homePremiumGradePercent);
                        TextView standardPercent = findViewById(R.id.homeStandardGradePercent);
                        TextView rejectPercent = findViewById(R.id.homeRejectGradePercent);
                        TextView gradedCount = findViewById(R.id.homeGradedCount);
                        TextView supplierOnboarded = findViewById(R.id.homeSupplierOnboarded);
                        TextView supplierTotal = findViewById(R.id.homeSupplierTotal);
                        TextView supplierRemaining = findViewById(R.id.homeSupplierRemaining);
                        TextView supplierProgressText = findViewById(R.id.homeSupplierProgressText);
                        ProgressBar supplierProgress = findViewById(R.id.homeSupplierProgress);
                        HomeChartView area = findViewById(R.id.homeRcnChart);
                        HomeChartView pie = findViewById(R.id.homeGradePie);
                        HomeChartView bars = findViewById(R.id.homeGradeBars);
                        if (rcn == null || premiumPercent == null || standardPercent == null || rejectPercent == null
                                || gradedCount == null || supplierOnboarded == null || supplierTotal == null
                                || supplierRemaining == null || supplierProgressText == null || supplierProgress == null
                                || area == null || pie == null || bars == null) return;
                        rcn.setText(String.format(Locale.getDefault(), "%,.0f", finalRcnKg));
                        area.setMode(HomeChartView.Mode.AREA);
                        area.setValues(finalMonthlyKg);
                        pie.setMode(HomeChartView.Mode.PIE);
                        pie.setValues(new float[]{finalPremium, finalStandard, finalReject});
                        bars.setMode(HomeChartView.Mode.BARS);
                        bars.setValues(new float[]{finalPremium, finalStandard, finalReject});
                        int[] gradePercentages = gradePercentages(finalPremium, finalStandard, finalReject);
                        premiumPercent.setText(gradePercentages[0] + "%");
                        standardPercent.setText(gradePercentages[1] + "%");
                        rejectPercent.setText(gradePercentages[2] + "%");
                        gradedCount.setText(String.valueOf(finalGraded));
                        supplierOnboarded.setText(String.valueOf(finalOnboarded));
                        supplierTotal.setText(String.format(Locale.getDefault(), "%,d", finalSupplierTotal));
                        int toGo = Math.max(finalSupplierTotal - finalOnboarded, 0);
                        if (getResources().getConfiguration().screenWidthDp >= 600) {
                            supplierRemaining.setText(toGo + " still to onboard");
                        } else {
                            supplierRemaining.setText(toGo + " to go");
                        }
                        int percent = finalSupplierTotal == 0 ? 0 : Math.round(finalOnboarded * 100f / finalSupplierTotal);
                        supplierProgress.setProgress(percent);
                        supplierProgressText.setText(percent + "%");
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading Home quality metrics", e);
                }
            });
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "Home quality metrics load rejected", e);
        }
    }

    private static int[] gradePercentages(int premium, int standard, int reject) {
        int[] counts = {Math.max(0, premium), Math.max(0, standard), Math.max(0, reject)};
        int total = counts[0] + counts[1] + counts[2];
        if (total == 0) return new int[]{0, 0, 0};

        int[] percentages = new int[3];
        float[] remainders = new float[3];
        int assigned = 0;
        for (int i = 0; i < counts.length; i++) {
            float exact = counts[i] * 100f / total;
            percentages[i] = (int) exact;
            remainders[i] = exact - percentages[i];
            assigned += percentages[i];
        }
        while (assigned < 100) {
            int next = 0;
            for (int i = 1; i < remainders.length; i++) {
                if (remainders[i] > remainders[next]) next = i;
            }
            percentages[next]++;
            remainders[next] = -1;
            assigned++;
        }
        return percentages;
    }

    private void updateFinancialSnapshot() {
        if (currentFarm == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        // Also get start of year for comparison
        Calendar yearCalendar = Calendar.getInstance();
        yearCalendar.set(Calendar.DAY_OF_YEAR, 1);
        yearCalendar.set(Calendar.HOUR_OF_DAY, 0);
        yearCalendar.set(Calendar.MINUTE, 0);
        yearCalendar.set(Calendar.SECOND, 0);
        long startOfYear = yearCalendar.getTimeInMillis();

        new Thread(() -> {
            double monthlyRevenue = 0;
            double monthlyExpenses = 0;
            double yearlyRevenue = 0;
            double yearlyExpenses = 0;
            double allTimeRevenue = 0;
            double allTimeExpenses = 0;

            // Calculate revenues
            List<RevenueEntity> revenues = database.revenueDao().getRevenuesByFarmId(currentFarm.getId());
            for (RevenueEntity revenue : revenues) {
                allTimeRevenue += revenue.getTotalAmount();

                long revenueDate = revenue.getCreatedAt();
                if (revenueDate >= startOfYear) {
                    yearlyRevenue += revenue.getTotalAmount();
                }
                if (revenueDate >= startOfMonth) {
                    monthlyRevenue += revenue.getTotalAmount();
                }
            }

            // Calculate expenses
            List<ExpenseEntity> expenses = database.expenseDao().getExpensesByFarmId(currentFarm.getId());
            for (ExpenseEntity expense : expenses) {
                long expenseDate = expense.getDate();
                allTimeExpenses += expense.getAmount();

                if (expenseDate >= startOfYear) {
                    yearlyExpenses += expense.getAmount();
                }
                if (expenseDate >= startOfMonth) {
                    monthlyExpenses += expense.getAmount();
                }
            }

            // Calculate trends (compare this month to previous month)
            double monthlyProfit = monthlyRevenue - monthlyExpenses;
            double yearlyProfit = yearlyRevenue - yearlyExpenses;
            double allTimeProfit = allTimeRevenue - allTimeExpenses;

            double revenueTrendPercent = calculateRevenueTrend();
            double expenseTrendPercent = calculateExpenseTrend();

            // Make them effectively final by creating local copies
            final double finalMonthlyRevenue = monthlyRevenue;
            final double finalMonthlyExpenses = monthlyExpenses;
            final double finalYearlyRevenue = yearlyRevenue;
            final double finalYearlyExpenses = yearlyExpenses;
            final double finalYearlyProfit = yearlyProfit;
            final double finalNetProfit = allTimeProfit;

            runOnUiThread(() -> {
                // Update the financial snapshot with YEARLY data (not monthly)
                monthlyRevenueText.setText(formatCurrency(finalYearlyRevenue));
                monthlyExpensesText.setText(formatCurrency(finalYearlyExpenses));
                netProfitText.setText(formatCurrency(finalYearlyProfit));
                netProfitText.setTextColor(finalYearlyProfit >= 0 ?
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

        // Start of current year
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long thisYearStart = calendar.getTimeInMillis();

        // Start of last year
        calendar.add(Calendar.YEAR, -1);
        long lastYearStart = calendar.getTimeInMillis();

        double lastYearRevenue = 0;
        double thisYearRevenue = 0;

        List<RevenueEntity> revenues =
                database.revenueDao().getRevenuesByFarmId(currentFarm.getId());

        for (RevenueEntity revenue : revenues) {
            long revenueDate = revenue.getCreatedAt();

            if (revenueDate >= lastYearStart && revenueDate < thisYearStart) {
                lastYearRevenue += revenue.getTotalAmount();
            } else if (revenueDate >= thisYearStart) {
                thisYearRevenue += revenue.getTotalAmount();
            }
        }

        if (lastYearRevenue > 0) {
            return ((thisYearRevenue - lastYearRevenue) / lastYearRevenue) * 100;
        } else {
            return thisYearRevenue > 0 ? 100 : 0;
        }
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
                        String subtitle = (revenue.getBatchNumber() != null && !revenue.getBatchNumber().isEmpty())
                                ? "Batch " + revenue.getBatchNumber()
                                : "Batch sale";

                        addActivityItem(
                                revenue.getQuality() != null
                                        ? revenue.getQuality().getDisplayName() + " Grade"
                                        : "Sale",
                                subtitle + " • " + formatCurrency(revenue.getTotalAmount()),
                                formatTimeAgo(revenue.getCreatedAt()),
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

        // Check if activity is finishing or executor is shutdown
        if (isFinishing() || isDestroyed() || databaseExecutor == null || databaseExecutor.isShutdown()) {
            Log.d(TAG, "Activity is finishing or executor shutdown, skipping user sync");
            return;
        }

        Log.d(TAG, "Loading users from Firestore for farm: " + currentFarm.getId());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String farmId = String.valueOf(currentFarm.getId());

        db.collection("users")
                .whereEqualTo("farmId", farmId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check again before processing
                    if (isFinishing() || isDestroyed() || databaseExecutor == null || databaseExecutor.isShutdown()) {
                        Log.d(TAG, "Activity destroyed during Firestore callback, skipping");
                        return;
                    }

                    List<UserEntity> firestoreUsers = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        UserEntity user = documentToUser(doc);
                        if (user != null) {
                            firestoreUsers.add(user);
                        }
                    }

                    Log.d(TAG, "Found " + firestoreUsers.size() + " users in Firestore");

                    // Check again before submitting to executor
                    if (isFinishing() || isDestroyed() || databaseExecutor == null || databaseExecutor.isShutdown()) {
                        Log.d(TAG, "Activity destroyed before executor submission, skipping");
                        return;
                    }

                    // Move database operations to background thread
                    databaseExecutor.execute(() -> {
                        // Check again inside the background thread
                        if (isFinishing() || isDestroyed()) {
                            Log.d(TAG, "Activity destroyed during background execution, skipping");
                            return;
                        }

                        for (UserEntity user : firestoreUsers) {
                            try {
                                UserEntity existing = database.userDao().getUserByEmail(user.getEmail());

                                if (existing == null) {
                                    long id = database.userDao().insert(user);
                                    user.setLocalId(id);
                                    Log.d(TAG, "✅ Inserted new user: " + user.getEmail());
                                } else {
                                    user.setLocalId(existing.getLocalId());
                                    database.userDao().update(user);
                                    Log.d(TAG, "✅ Updated existing user: " + user.getEmail());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing user: " + user.getEmail(), e);
                            }
                        }

                        // Update UI if activity is still alive
                        if (!isFinishing() && !isDestroyed()) {
                            runOnUiThread(() -> {
                                Log.d(TAG, "✅ User sync completed from Firestore");
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Log.e(TAG, "❌ Failed to load users from Firestore", e);
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

            Long batchId = doc.contains("batchId") ? doc.getLong("batchId") : null;
            Long supplierId = doc.contains("supplierId") ? doc.getLong("supplierId") : null;

            // 40→41: `harvestDate` was the user-picked sale date. Prefer it when
            // present (v40 Firestore docs still carry it); fall back to createdAt
            // for any doc already migrated or newly written.
            Long harvestDateObj = doc.getLong("harvestDate");
            Long createdAtObj = doc.getLong("createdAt");
            long saleDate = harvestDateObj != null
                    ? harvestDateObj
                    : (createdAtObj != null ? createdAtObj : System.currentTimeMillis());

            double quantityKg = doc.getDouble("quantityKg") != null ? doc.getDouble("quantityKg") : 0;
            double pricePerKg = doc.getDouble("pricePerKg") != null ? doc.getDouble("pricePerKg") : 0;
            String buyer = doc.getString("buyer");
            String quality = doc.getString("quality");
            String notes = doc.getString("notes");

            RevenueEntity.QualityGrade grade;
            try {
                grade = RevenueEntity.QualityGrade.valueOf(quality);
            } catch (IllegalArgumentException | NullPointerException e) {
                grade = RevenueEntity.QualityGrade.STANDARD;
            }

            RevenueEntity revenue = new RevenueEntity(
                    farmId, batchId, supplierId, saleDate,
                    quantityKg, pricePerKg, buyer, grade, notes);
            revenue.setId(id);

            // Preserve denormalized fields if the doc carries them
            if (doc.contains("batchNumber")) revenue.setBatchNumber(doc.getString("batchNumber"));
            if (doc.contains("supplierName")) revenue.setSupplierName(doc.getString("supplierName"));

            return revenue;
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
    // UTILITY
    // ========================================

    private void showLogoutDialog() {
        com.afrinuts.farmos.ui.LogoutConfirmationDialog.show(this, () -> {
                    // Clear everything before logout
                    sessionManager.logout();

                    // Also sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // Clear any pending sync
                    if (syncManager != null) {
                        syncManager.shutdown();
                    }

                    redirectToLogin();
                });
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
