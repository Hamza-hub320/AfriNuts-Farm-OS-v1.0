package com.afrinuts.farmos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.farm.FarmHubActivity;
import com.afrinuts.farmos.ui.finance.FinanceHubActivity;

public abstract class BaseHubActivity extends BaseActivity {

    // Bottom Navigation Views
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
    private CardView bottomNavCard;

    // Current selected tab (to be set by child activities)
    protected int currentNavItem = -1; // 0=Home, 1=Farm, 2=Finance, 3=Analytics

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());
        initBottomNavigation();
        setupBottomNavigation();
        onHubCreate(savedInstanceState);
    }

    protected abstract int getLayoutResourceId();
    protected abstract void onHubCreate(Bundle savedInstanceState);
    protected abstract int getCurrentNavItem();

    private void initBottomNavigation() {
        bottomNavCard = findViewById(R.id.bottomNavCard);
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
    }

    private void setupBottomNavigation() {
        currentNavItem = getCurrentNavItem();
        updateNavSelection(currentNavItem);

        navHome.setOnClickListener(v -> navigateTo(MainActivity.class, 0));
        navFarm.setOnClickListener(v -> navigateTo(FarmHubActivity.class, 1));
        navFinance.setOnClickListener(v -> navigateTo(FinanceHubActivity.class, 2));
        navAnalytics.setOnClickListener(v -> navigateTo(AnalyticsHubActivity.class, 3));
    }

    private void navigateTo(Class<?> targetActivity, int targetNavItem) {
        if (currentNavItem == targetNavItem) return;

        Intent intent = new Intent(this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    protected void updateNavSelection(int selectedPosition) {
        int inactiveColor = getThemeColor(R.attr.navbarIconColor);
        int activeColor = ContextCompat.getColor(this, R.color.primary);

        // Reset all icons and text colors
        navHomeIcon.setColorFilter(inactiveColor);
        navFarmIcon.setColorFilter(inactiveColor);
        navFinanceIcon.setColorFilter(inactiveColor);
        navAnalyticsIcon.setColorFilter(inactiveColor);

        navHomeText.setTextColor(inactiveColor);
        navFarmText.setTextColor(inactiveColor);
        navFinanceText.setTextColor(inactiveColor);
        navAnalyticsText.setTextColor(inactiveColor);

        // Set selected item
        switch (selectedPosition) {
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
}