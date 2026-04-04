package com.afrinuts.farmos.ui.analytics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.reports.ReportsActivity;
import com.afrinuts.farmos.ui.workers.WorkerDashboardFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AnalyticsHubActivity extends BaseHubActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAction;
    private AnalyticsPagerAdapter pagerAdapter;

    // Fragments
    private AnalyticsDashboardFragment analyticsFragment;
    private WorkerDashboardFragment workerFragment;
    private ReportsFragment reportsFragment;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_analytics_hub;
    }

    @Override
    protected int getCurrentNavItem() {
        return 3; // Analytics tab is position 3
    }

    @Override
    protected void onHubCreate(Bundle savedInstanceState) {
        initViews();
        setupViewPager();
        setupFab();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fabAction = findViewById(R.id.fabAction);
    }

    private void setupViewPager() {
        pagerAdapter = new AnalyticsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Predictions");
                    tab.setIcon(R.drawable.ic_trending_up);
                    break;
                case 1:
                    tab.setText("Workers");
                    tab.setIcon(R.drawable.ic_people);
                    break;
                case 2:
                    tab.setText("Reports");
                    tab.setIcon(R.drawable.ic_pdf);
                    break;
            }
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateFabForTab(position);
            }
        });
    }

    private void updateFabForTab(int position) {
        // No FAB for any tab
        fabAction.hide();
    }

    private void setupFab() {
        fabAction.hide();
    }

    @Override
    protected boolean checkRoleAccess() {
        return true;
    }

    // ViewPager Adapter
    private static class AnalyticsPagerAdapter extends FragmentStateAdapter {

        private final AnalyticsDashboardFragment analyticsFragment;
        private final WorkerDashboardFragment workerFragment;
        private final ReportsFragment reportsFragment;

        public AnalyticsPagerAdapter(AnalyticsHubActivity activity) {
            super(activity);
            analyticsFragment = new AnalyticsDashboardFragment();
            workerFragment = new WorkerDashboardFragment();
            reportsFragment = new ReportsFragment();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return analyticsFragment;
                case 1:
                    return workerFragment;
                case 2:
                    return reportsFragment;
                default:
                    return analyticsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        public Fragment getFragment(int position) {
            switch (position) {
                case 0:
                    return analyticsFragment;
                case 1:
                    return workerFragment;
                case 2:
                    return reportsFragment;
                default:
                    return analyticsFragment;
            }
        }
    }
}