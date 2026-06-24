package com.afrinuts.farmos.ui.analytics;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.expenses.ExpenseChartsFragment;
import com.afrinuts.farmos.ui.quality.QualityMetricsFragment;
import com.afrinuts.farmos.ui.quality.QualityBatchDialog;
import com.afrinuts.farmos.ui.reports.ReportsFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AnalyticsHubActivity extends BaseHubActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAction;
    private AnalyticsPagerAdapter pagerAdapter;
    private QualityMetricsFragment qualityMetricsFragment;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_analytics_hub;
    }

    @Override
    protected int getCurrentNavItem() {
        return 3;
    }

    @Override
    protected void onHubCreate(Bundle savedInstanceState) {
        initViews();
        setupViewPager();
        setupFab();
    }

    // ✅ Implement the abstract method from BaseActivity
    @Override
    protected boolean checkRoleAccess() {
        // All roles can access analytics (investors can view too)
        return true;
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
                    tab.setText("Quality");
                    tab.setIcon(R.drawable.ic_quality);
                    break;
                case 1:
                    tab.setText("Charts");
                    tab.setIcon(R.drawable.ic_insert_chart);
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

        // Get reference to QualityMetricsFragment after adapter is ready
        qualityMetricsFragment = (QualityMetricsFragment) pagerAdapter.getFragment(0);
    }

    private void updateFabForTab(int position) {
        if (position == 0) {
            // Quality tab - show FAB
            fabAction.show();
            fabAction.setImageResource(R.drawable.ic_add);
            fabAction.setOnClickListener(v -> showAddQualityDialog());
        } else {
            // Other tabs - hide FAB
            fabAction.hide();
        }
    }

    private void setupFab() {
        // Initially hidden, will be shown in updateFabForTab
        fabAction.hide();
    }

    private void showAddQualityDialog() {
        if (qualityMetricsFragment == null) {
            Toast.makeText(this, "Quality fragment not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get farm ID from the fragment
        long farmId = qualityMetricsFragment.getFarmId();
        if (farmId <= 0) {
            Toast.makeText(this, "Farm not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        QualityBatchDialog dialog = QualityBatchDialog.newInstance(farmId);
        dialog.setOnBatchAddedListener(() -> {
            // Refresh the quality metrics fragment
            if (qualityMetricsFragment != null) {
                qualityMetricsFragment.refreshData();
            }
            Toast.makeText(this, "Quality batch added!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "QualityBatchDialog");
    }

    // ViewPager Adapter
    private static class AnalyticsPagerAdapter extends FragmentStateAdapter {

        private final QualityMetricsFragment metricsFragment;
        private final ExpenseChartsFragment chartsFragment;
        private final ReportsFragment reportsFragment;

        public AnalyticsPagerAdapter(AnalyticsHubActivity activity) {
            super(activity);
            metricsFragment = new QualityMetricsFragment();
            chartsFragment = new ExpenseChartsFragment();
            reportsFragment = new ReportsFragment();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return metricsFragment;
                case 1:
                    return chartsFragment;
                case 2:
                    return reportsFragment;
                default:
                    return metricsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        public Fragment getFragment(int position) {
            switch (position) {
                case 0:
                    return metricsFragment;
                case 1:
                    return chartsFragment;
                case 2:
                    return reportsFragment;
                default:
                    return metricsFragment;
            }
        }
    }
}