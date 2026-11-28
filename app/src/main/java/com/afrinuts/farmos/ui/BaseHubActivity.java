package com.afrinuts.farmos.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.compliance.ComplianceHubActivity;
import com.afrinuts.farmos.ui.farm.FarmHubActivity;
import com.afrinuts.farmos.ui.finance.FinanceHubActivity;
import com.afrinuts.farmos.ui.notifications.NotificationBell;

public abstract class BaseHubActivity extends BaseActivity {
    private android.widget.TextView notificationBadge;

    protected void wireNotificationBell(int viewId) {
        android.widget.ImageButton bell = findViewById(viewId);
        notificationBadge = NotificationBell.wire(this, bell);
        NotificationBell.refresh(this, notificationBadge);
    }

    @Override protected void onResume() {
        super.onResume();
        NotificationBell.refresh(this, notificationBadge);
    }

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

        if (navHome != null) navHome.setOnClickListener(v -> navigateTo(MainActivity.class, 0));
        if (navFarm != null) navFarm.setOnClickListener(v -> navigateTo(FarmHubActivity.class, 1));
        if (navFinance != null) navFinance.setOnClickListener(v -> navigateTo(FinanceHubActivity.class, 2));
        if (navAnalytics != null) navAnalytics.setOnClickListener(v -> navigateTo(AnalyticsHubActivity.class, 3));
        if (navCompliance != null) navCompliance.setOnClickListener(v -> navigateTo(ComplianceHubActivity.class, 4));
    }

    private void navigateTo(Class<?> targetActivity, int targetNavItem) {
        if (currentNavItem == targetNavItem) return;
        if (!com.afrinuts.farmos.utils.AccessMap.canOpenHub(targetNavItem, sessionManager.getUserRole())) {
            android.widget.Toast.makeText(this, com.afrinuts.farmos.utils.AccessMap.hubDeniedMessage(targetNavItem), android.widget.Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    protected void updateNavSelection(int selectedPosition) {
        setNavItemState(navHome, navHomeIcon, navHomeText, selectedPosition == 0);
        setNavItemState(navFarm, navFarmIcon, navFarmText, selectedPosition == 1);
        setNavItemState(navFinance, navFinanceIcon, navFinanceText, selectedPosition == 2);
        setNavItemState(navAnalytics, navAnalyticsIcon, navAnalyticsText, selectedPosition == 3);
        setNavItemState(navCompliance, navComplianceIcon, navComplianceText, selectedPosition == 4);
    }

    private void setNavItemState(LinearLayout item, ImageView icon, TextView label, boolean active) {
        // Some secondary screens, such as Weather, intentionally expose only a subset of hub tabs.
        if (item == null || icon == null || label == null) return;
        int inactive = 0xFFC9C2B4;
        item.setOrientation(active ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        item.setGravity(android.view.Gravity.CENTER);
        int horizontalPadding = active ? dp(16) : 0;
        item.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        item.setBackgroundResource(active ? R.drawable.bg_nav_active_pill : 0);
        label.setVisibility(active ? View.VISIBLE : View.GONE);
        label.setTextSize(14);
        label.setTypeface(Typeface.create(ResourcesCompat.getFont(this, R.font.manrope_extrabold), Typeface.NORMAL));
        label.setTextColor(active ? android.graphics.Color.WHITE : inactive);
        label.setAlpha(1f);
        LinearLayout.LayoutParams labelParams = (LinearLayout.LayoutParams) label.getLayoutParams();
        labelParams.topMargin = 0;
        labelParams.setMarginStart(active ? dp(7) : 0);
        label.setLayoutParams(labelParams);
        ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
        iconParams.width = dp(20);
        iconParams.height = dp(20);
        icon.setLayoutParams(iconParams);
        icon.setColorFilter(active ? android.graphics.Color.WHITE : inactive);
        ViewGroup.LayoutParams raw = item.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams itemParams = (LinearLayout.LayoutParams) raw;
            itemParams.width = 0;
            itemParams.height = dp(48);
            itemParams.weight = active ? 1.8f : 0.8f;
            item.setLayoutParams(itemParams);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

}
