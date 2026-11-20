package com.afrinuts.farmos.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.compliance.ComplianceHubActivity;
import com.afrinuts.farmos.ui.farm.FarmHubActivity;
import com.afrinuts.farmos.ui.finance.FinanceHubActivity;

public abstract class BaseHubActivity extends BaseActivity {

    // Bottom Navigation Views
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

    private CardView bottomNavCard;

    // Scroll tracking
    private int lastScrollY = 0;
    private boolean isNavbarVisible = true;
    private static final int HIDE_THRESHOLD = 20; // Minimum scroll distance to trigger hide

    // Current selected tab
    protected int currentNavItem = -1;

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

    /**
     * Call this method to attach scroll listener to a RecyclerView
     */
    protected void attachScrollListenerToRecyclerView(RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                handleScroll(dy);
            }
        });
    }

    /**
     * Call this method to attach scroll listener to a ViewPager2
     */
    protected void attachScrollListenerToViewPager(ViewPager2 viewPager) {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        });

        // Also listen for nested scrolling
        viewPager.getChildAt(0).setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int dy = scrollY - oldScrollY;
                handleScroll(dy);
            }
        });
    }

    /**
     * Handle scroll direction to show/hide navbar
     */
    private void handleScroll(int dy) {
        if (dy > HIDE_THRESHOLD && isNavbarVisible) {
            // Scrolling down - hide navbar
            hideNavbar();
        } else if (dy < -HIDE_THRESHOLD && !isNavbarVisible) {
            // Scrolling up - show navbar
            showNavbar();
        }
    }

    /**
     * Hide the bottom navigation bar with animation
     */
    private void hideNavbar() {
        if (!isNavbarVisible || bottomNavCard == null) return;

        isNavbarVisible = false;
        bottomNavCard.animate()
                .translationY(bottomNavCard.getHeight() + 50)
                .setDuration(250)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        bottomNavCard.setVisibility(View.INVISIBLE);
                    }
                })
                .start();
    }

    /**
     * Show the bottom navigation bar with animation
     */
    private void showNavbar() {
        if (isNavbarVisible || bottomNavCard == null) return;

        bottomNavCard.setVisibility(View.VISIBLE);
        bottomNavCard.animate()
                .translationY(0)
                .setDuration(250)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        isNavbarVisible = true;
    }

    /**
     * Reset navbar visibility (call this when refreshing content)
     */
    protected void resetNavbarVisibility() {
        if (!isNavbarVisible && bottomNavCard != null) {
            bottomNavCard.setVisibility(View.VISIBLE);
            bottomNavCard.setTranslationY(0);
            isNavbarVisible = true;
        }
    }

    private void initBottomNavigation() {
        bottomNavCard = findViewById(R.id.bottomNavCard);
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

        // Initially visible
        bottomNavCard.setVisibility(View.VISIBLE);
        bottomNavCard.setTranslationY(0);
        isNavbarVisible = true;
        lastScrollY = 0;
    }

    private void setupBottomNavigation() {
        currentNavItem = getCurrentNavItem();
        updateNavSelection(currentNavItem);

        navHome.setOnClickListener(v -> navigateTo(MainActivity.class, 0));
        navFarm.setOnClickListener(v -> navigateTo(FarmHubActivity.class, 1));
        navFinance.setOnClickListener(v -> navigateTo(FinanceHubActivity.class, 2));
        navAnalytics.setOnClickListener(v -> navigateTo(AnalyticsHubActivity.class, 3));
        navCompliance.setOnClickListener(v -> navigateTo(ComplianceHubActivity.class, 4));
    }

    private void navigateTo(Class<?> targetActivity, int targetNavItem) {
        if (currentNavItem == targetNavItem) return;
        if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(targetNavItem, sessionManager.getUserRole())) {
            android.widget.Toast.makeText(this, "This section is not available for your role", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

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
        navComplianceIcon.setColorFilter(inactiveColor);

        navHomeText.setTextColor(inactiveColor);
        navFarmText.setTextColor(inactiveColor);
        navFinanceText.setTextColor(inactiveColor);
        navAnalyticsText.setTextColor(inactiveColor);
        navComplianceText.setTextColor(inactiveColor);

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
            case 4:
                navComplianceIcon.setColorFilter(activeColor);
                navComplianceText.setTextColor(activeColor);
                break;
        }
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}