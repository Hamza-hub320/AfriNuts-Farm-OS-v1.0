package com.afrinuts.farmos.ui.compliance;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.farm.FarmHubActivity;
import com.afrinuts.farmos.ui.finance.FinanceHubActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

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
        tabs.add(new TabItem("Certifications", R.drawable.ic_certified, TabType.CERTIFICATIONS));
        tabs.add(new TabItem("Audits", R.drawable.ic_audit, TabType.AUDITS));
        tabs.add(new TabItem("Compliance inspections", R.drawable.ic_inspection, TabType.INSPECTIONS));
        tabs.add(new TabItem("Verifications", R.drawable.ic_verified, TabType.VERIFICATIONS));
        tabs.add(new TabItem("Documents", R.drawable.ic_document, TabType.DOCUMENTS));
        tabs.add(new TabItem("Due & expiring", R.drawable.ic_progress_clock, TabType.DUE_EXPIRING));
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fabAdd = findViewById(R.id.fabAdd);

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

    private void setupViewPager() {
        pagerAdapter = new CompliancePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            TabItem tabItem = tabs.get(position);
            tab.setText(tabItem.title);
            tab.setIcon(tabItem.iconRes);
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

        // Clear any existing listeners to prevent duplicates
        fabAdd.setOnClickListener(null);
        fabAdd.setImageResource(R.drawable.ic_add);

        // Set click listener based on tab type
        switch (tabItem.type) {
            case CERTIFICATIONS:
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

            case AUDITS:
                fabAdd.setOnClickListener(v -> {
                    Fragment fragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + viewPager.getCurrentItem());
                    if (fragment instanceof AuditListFragment) {
                        ((AuditListFragment) fragment).showAddAuditDialog();
                    }
                });
                fabAdd.show();
                break;

            case INSPECTIONS:
                fabAdd.setOnClickListener(v -> {
                    Fragment fragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + viewPager.getCurrentItem());
                    if (fragment instanceof InspectionListFragment) {
                        ((InspectionListFragment) fragment).showAddInspectionDialog();
                    }
                });
                fabAdd.show();
                break;

            case VERIFICATIONS:
                fabAdd.setOnClickListener(v -> {
                    Fragment fragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + viewPager.getCurrentItem());
                    if (fragment instanceof VerificationListFragment) {
                        ((VerificationListFragment) fragment).showAddVerificationDialog();
                    }
                });
                fabAdd.show();
                break;

            case DOCUMENTS:
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
            Intent intent = new Intent(this, com.afrinuts.farmos.MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navFarm.setOnClickListener(v -> {
            Intent intent = new Intent(this, FarmHubActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navFinance.setOnClickListener(v -> {
            Intent intent = new Intent(this, FinanceHubActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navAnalytics.setOnClickListener(v -> {
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
        int inactiveColor = getThemeColor(R.attr.navbarIconColor);
        int activeColor = ContextCompat.getColor(this, R.color.primary);

        resetNavItem(navHomeIcon, navHomeText, inactiveColor);
        resetNavItem(navFarmIcon, navFarmText, inactiveColor);
        resetNavItem(navFinanceIcon, navFinanceText, inactiveColor);
        resetNavItem(navAnalyticsIcon, navAnalyticsText, inactiveColor);
        resetNavItem(navComplianceIcon, navComplianceText, inactiveColor);

        switch (selectedIndex) {
            case 0:
                setActiveNavItem(navHomeIcon, navHomeText, activeColor);
                break;
            case 1:
                setActiveNavItem(navFarmIcon, navFarmText, activeColor);
                break;
            case 2:
                setActiveNavItem(navFinanceIcon, navFinanceText, activeColor);
                break;
            case 3:
                setActiveNavItem(navAnalyticsIcon, navAnalyticsText, activeColor);
                break;
            case 4:
                setActiveNavItem(navComplianceIcon, navComplianceText, activeColor);
                break;
        }
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
        return sessionManager.isOwner() || sessionManager.isManager() || sessionManager.isCEO();
    }

    @Override
    protected String getAccessDeniedMessage() {
        return "Compliance is only accessible to Owners, Managers and CEO";
    }

    // Tab types
    private enum TabType {
        CERTIFICATIONS,
        AUDITS,
        INSPECTIONS,
        VERIFICATIONS,
        DOCUMENTS,
        DUE_EXPIRING
    }

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
                case CERTIFICATIONS:
                    return new CertificationListFragment();
                case AUDITS:
                    return new AuditListFragment();
                case INSPECTIONS:
                    return new InspectionListFragment();
                case VERIFICATIONS:
                    return new VerificationListFragment();
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
