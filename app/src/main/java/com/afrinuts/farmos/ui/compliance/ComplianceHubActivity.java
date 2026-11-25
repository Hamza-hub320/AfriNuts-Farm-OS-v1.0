package com.afrinuts.farmos.ui.compliance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.farm.FarmHubActivity;
import com.afrinuts.farmos.ui.finance.FinanceHubActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.ui.settings.SettingsActivity;
import com.afrinuts.farmos.ui.users.UsersListActivity;
import com.afrinuts.farmos.ui.sync.SyncCenterActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.firebase.auth.FirebaseAuth;

public class ComplianceHubActivity extends BaseHubActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;
    private CompliancePagerAdapter pagerAdapter;

    // Navigation items
    private LinearLayout navHome;
    private LinearLayout navFarm;
    private LinearLayout navFinance;
    private LinearLayout navAnalytics;
    private LinearLayout navCompliance;
    private ImageView navHomeIcon;
    private ImageView navFarmIcon;
    private ImageView navFinanceIcon;
    private ImageView navAnalyticsIcon;
    private ImageView navComplianceIcon;
    private TextView navHomeText;
    private TextView navFarmText;
    private TextView navFinanceText;
    private TextView navAnalyticsText;
    private TextView navComplianceText;

    // Track tabs
    private List<TabItem> tabs = new ArrayList<>();
    private int currentPosition = 0;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_compliance_hub;
    }

    @Override
    protected int getCurrentNavItem() {
        return 4; // Compliance is index 4
    }

    @Override
    protected void onHubCreate(Bundle savedInstanceState) {
        setupTabs();
        initViews();
        setupViewPager();
        setupFab();
        setupBottomNavigation();
    }

    private void setupTabs() {
        tabs.clear();
        for (String key : com.afrinuts.farmos.utils.AccessMap.hubTabs(4, sessionManager.getUserRole())) {
            switch (key) {
                case "PROGRAMMES": tabs.add(new TabItem("Programs", R.drawable.ic_certified, TabType.PROGRAMMES)); break;
                case "CHECKS": tabs.add(new TabItem("Checks", R.drawable.ic_audit, TabType.CHECKS)); break;
                case "DOCUMENTS": tabs.add(new TabItem("Docs", R.drawable.ic_document, TabType.DOCUMENTS)); break;
                case "DUE_EXPIRING": tabs.add(new TabItem("Due", R.drawable.ic_progress_clock, TabType.DUE_EXPIRING)); break;
                default: throw new IllegalStateException("Unknown Compliance tab: " + key);
            }
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fabAdd = findViewById(R.id.fabAdd);
        View profile = findViewById(R.id.complianceProfileButton);
        if (profile != null) profile.setOnClickListener(v -> showAccountMenu(profile));
        TextView complianceDate = findViewById(R.id.complianceDateText);
        if (complianceDate != null) {
            complianceDate.setText(new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date()));
        }

        // Bottom navigation items
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

        fabAdd.hide();
    }

    private void showAccountMenu(View anchor) {
        PopupWindow popup = new PopupWindow(this);
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), 0xFFE9E1CF);
        menu.setBackground(bg);
        if (sessionManager.canManageUsers()) addMenuRow(menu, popup, "Team", R.drawable.ic_person_add, () ->
                startActivity(new Intent(this, UsersListActivity.class)));
        addMenuRow(menu, popup, "Settings", R.drawable.ic_settings, () -> startActivity(new Intent(this, SettingsActivity.class)));
        if (sessionManager.isOwner() || sessionManager.isCEO() || sessionManager.isManager())
            addMenuRow(menu, popup, "Sync Center", R.drawable.ic_sync, () ->
                    startActivity(new Intent(this, SyncCenterActivity.class)));
        View divider = new View(this);
        divider.setBackgroundColor(0xFFE9E1CF);
        menu.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        addMenuRow(menu, popup, "Logout", R.drawable.ic_logout, () -> new MaterialAlertDialogBuilder(this)
                .setTitle("Logout").setMessage("Are you sure you want to logout?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.logout();
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }).show());
        popup.setContentView(menu);
        popup.setWidth(dp(196));
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setElevation(dp(8));
        popup.showAsDropDown(anchor, -dp(152), dp(8));
    }

    private void addMenuRow(LinearLayout menu, PopupWindow popup, String title, int iconRes, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(12), 0);
        row.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0xFFEFEAE2),
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT), null));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter("Logout".equals(title) ? 0xFFB3402A : 0xFF1C1B1A);
        row.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView text = new TextView(this);
        text.setText(title);
        text.setTextColor("Logout".equals(title) ? 0xFFB3402A : 0xFF1C1B1A);
        text.setTextSize(15);
        text.setTypeface(Typeface.create(ResourcesCompat.getFont(this, R.font.manrope_medium), Typeface.NORMAL));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMarginStart(dp(12));
        row.addView(text, textParams);
        menu.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        row.setOnClickListener(v -> { popup.dismiss(); action.run(); });
    }

    private void setupViewPager() {
        pagerAdapter = new CompliancePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            TabItem tabItem = tabs.get(position);
            tab.setText(tabItem.title);
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updateFabForTab(position);
            }
        });
    }

    private void updateFabForTab(int position) {
        if (position >= tabs.size()) {
            fabAdd.hide();
            return;
        }

        TabItem tabItem = tabs.get(position);

        // Keep the Docs FAB 16dp above the bottom navigation pill.
        android.view.ViewGroup.LayoutParams rawParams = fabAdd.getLayoutParams();
        if (rawParams instanceof android.widget.FrameLayout.LayoutParams) {
            android.widget.FrameLayout.LayoutParams fabParams =
                    (android.widget.FrameLayout.LayoutParams) rawParams;
            fabParams.bottomMargin = dp(tabItem.type == TabType.DOCUMENTS ? 92 : 126);
            fabAdd.setLayoutParams(fabParams);
        }

        // Clear any existing listeners to prevent duplicates
        fabAdd.setOnClickListener(null);
        fabAdd.setImageResource(R.drawable.ic_add);

        // Set click listener based on tab type
        switch (tabItem.type) {
            case PROGRAMMES:
                if (!com.afrinuts.farmos.utils.AccessMap.canUseFab("ADD_CERTIFICATION", sessionManager.getUserRole())) { fabAdd.hide(); break; }
                fabAdd.setOnClickListener(v -> {
                    Fragment fragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + viewPager.getCurrentItem());
                    if (fragment instanceof CertificationListFragment) {
                        ((CertificationListFragment) fragment).showAddCertificationDialog();
                    } else {
                        Toast.makeText(this, "Please switch to Certifications tab", Toast.LENGTH_SHORT).show();
                    }
                });
                fabAdd.show();
                break;

            case CHECKS:
                if (!com.afrinuts.farmos.utils.AccessMap.canUseFab("ADD_AUDIT", sessionManager.getUserRole())) { fabAdd.hide(); break; }
                fabAdd.setOnClickListener(v -> showCheckChooser());
                fabAdd.show();
                break;

            case DOCUMENTS:
                if (!com.afrinuts.farmos.utils.AccessMap.canUseFab("ADD_DOCUMENT", sessionManager.getUserRole())) { fabAdd.hide(); break; }
                fabAdd.setOnClickListener(v -> {
                    Fragment fragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + viewPager.getCurrentItem());
                    if (fragment instanceof DocumentListFragment) {
                        ((DocumentListFragment) fragment).showAddDocumentDialog();
                    }
                });
                fabAdd.show();
                break;

            default:
                fabAdd.hide();
                break;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showCheckChooser() {
        new CheckChooserDialog().show(getSupportFragmentManager(), "AddCheckChooserDialog");
    }

    void showCheckChoice(int which) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (!(fragment instanceof ChecksFragment)) return;
        ChecksFragment checks = (ChecksFragment) fragment;
        if (which == 0) checks.showAddAuditDialog();
        else if (which == 1) checks.showAddInspectionDialog();
        else if (which == 2) checks.showAddVerificationDialog();
    }

    private void setupFab() {
        if (tabs.isEmpty()) {
            fabAdd.hide();
            return;
        }

        viewPager.post(() -> {
            currentPosition = viewPager.getCurrentItem();
            updateFabForTab(currentPosition);
        });
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(0, sessionManager.getUserRole())) {
                Toast.makeText(this, "This section is not available for your role", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, com.afrinuts.farmos.MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navFarm.setOnClickListener(v -> {
            if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(1, sessionManager.getUserRole())) {
                Toast.makeText(this, "This section is not available for your role", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FarmHubActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navFinance.setOnClickListener(v -> {
            if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(2, sessionManager.getUserRole())) {
                Toast.makeText(this, "This section is not available for your role", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FinanceHubActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navAnalytics.setOnClickListener(v -> {
            if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(3, sessionManager.getUserRole())) {
                Toast.makeText(this, "This section is not available for your role", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, AnalyticsHubActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navCompliance.setOnClickListener(v -> {
            // Already on Compliance, refresh
            finish();
            startActivity(getIntent());
        });

        highlightNavItem(4);
    }

    private void highlightNavItem(int selectedIndex) {
        updateNavSelection(selectedIndex);
    }

    private void resetNavItem(ImageView icon, TextView text, int color) {
        icon.setColorFilter(color);
        text.setTextColor(color);
        text.setAlpha(0.6f);
    }

    private void setActiveNavItem(ImageView icon, TextView text, int color) {
        icon.setColorFilter(color);
        text.setTextColor(color);
        text.setAlpha(1.0f);
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected String getAccessDeniedMessage() {
        return "Compliance is only accessible to Owners, Managers and CEO";
    }

    // Tab types
    private enum TabType { PROGRAMMES, CHECKS, DOCUMENTS, DUE_EXPIRING }

    // Tab item class
    private static class TabItem {
        String title;
        int iconRes;
        TabType type;

        TabItem(String title, int iconRes, TabType type) {
            this.title = title;
            this.iconRes = iconRes;
            this.type = type;
        }
    }

    // ViewPager Adapter
    private class CompliancePagerAdapter extends FragmentStateAdapter {

        public CompliancePagerAdapter(@NonNull ComplianceHubActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            TabItem tab = tabs.get(position);
            switch (tab.type) {
                case PROGRAMMES:
                    return new CertificationListFragment();
                case CHECKS:
                    return new ChecksFragment();
                case DOCUMENTS:
                    return new DocumentListFragment();
                case DUE_EXPIRING:
                    return new DueExpiringFragment();
                default:
                    return new CertificationListFragment();
            }
        }

        @Override
        public int getItemCount() {
            return tabs.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
}
