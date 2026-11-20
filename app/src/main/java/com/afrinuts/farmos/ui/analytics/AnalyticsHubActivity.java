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
import com.afrinuts.farmos.ui.quality.QualityMetricsFragment;
import com.afrinuts.farmos.ui.quality.QualityBatchDialog;
import com.afrinuts.farmos.ui.reports.ReportsFragment;
import com.afrinuts.farmos.ui.traceability.CreateMasterTraceabilityDialog;
import com.afrinuts.farmos.ui.traceability.MasterTraceabilityFragment;
import com.afrinuts.farmos.ui.traceability.QRScannerActivity;
import com.afrinuts.farmos.ui.traceability.TraceabilityFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AnalyticsHubActivity extends BaseHubActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAction;
    private AnalyticsPagerAdapter pagerAdapter;
    private QualityMetricsFragment qualityMetricsFragment;
    private TraceabilityFragment traceabilityFragment;
    private MasterTraceabilityFragment masterTraceabilityFragment;

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

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fabAction = findViewById(R.id.fabAction);
    }

    private void setupViewPager() {
        pagerAdapter = new AnalyticsPagerAdapter(this,
                com.afrinuts.farmos.utils.AccessMap.hubTabs(3, sessionManager.getUserRole()));
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String key = pagerAdapter.getTabKey(position);
            switch (key) {
                case "QUALITY": tab.setText("Quality"); tab.setIcon(R.drawable.ic_quality); break;
                case "TRACEABILITY": tab.setText("Traceability"); tab.setIcon(R.drawable.ic_qr_code); break;
                case "BULK": tab.setText("Bulk"); tab.setIcon(R.drawable.ic_batch); break;
                case "REPORTS": tab.setText("Reports"); tab.setIcon(R.drawable.ic_pdf); break;
            }
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateFabForTab(position);
            }
        });

        qualityMetricsFragment = (QualityMetricsFragment) pagerAdapter.getFragment(0);
        traceabilityFragment = (TraceabilityFragment) pagerAdapter.getFragment(1);
        masterTraceabilityFragment = (MasterTraceabilityFragment) pagerAdapter.getFragment(2);
    }

    private void updateFabForTab(int position) {
        if (!com.afrinuts.farmos.utils.AccessMap.canUseFab("ANALYTICS_EDIT", sessionManager.getUserRole())) {
            fabAction.hide();
            fabAction.setOnClickListener(null);
            fabAction.setOnLongClickListener(null);
            return;
        }
        if ("QUALITY".equals(pagerAdapter.getTabKey(position))) {
            // Quality tab — kernel batch on tap, RCN intake on long-press
            fabAction.show();
            fabAction.setImageResource(R.drawable.ic_add);

            fabAction.setOnClickListener(v -> {
                if (qualityMetricsFragment != null && qualityMetricsFragment.getFarmId() > 0) {
                    QualityBatchDialog dialog = QualityBatchDialog.newInstance(
                            qualityMetricsFragment.getFarmId());
                    dialog.setOnBatchAddedListener(() -> {
                        if (qualityMetricsFragment != null) {
                            qualityMetricsFragment.refreshData();
                        }
                    });
                    dialog.show(getSupportFragmentManager(), "AddQualityBatch");
                } else {
                    Toast.makeText(this, "Loading farm data...", Toast.LENGTH_SHORT).show();
                }
            });

            fabAction.setOnLongClickListener(v -> {
                if (qualityMetricsFragment != null) {
                    qualityMetricsFragment.openRcnIntakeDialog();
                }
                return true;
            });

        } else if ("TRACEABILITY".equals(pagerAdapter.getTabKey(position))) {
            fabAction.show();
            fabAction.setImageResource(R.drawable.ic_qr_code);
            fabAction.setOnLongClickListener(null);
            fabAction.setOnClickListener(v -> {
                if (traceabilityFragment != null) {
                    traceabilityFragment.showTraceabilityOptions();
                }
            });
        } else if ("BULK".equals(pagerAdapter.getTabKey(position))) {
            fabAction.show();
            fabAction.setImageResource(R.drawable.ic_add);
            fabAction.setOnLongClickListener(null);
            fabAction.setOnClickListener(v -> showCreateMasterDialog());
        } else {
            fabAction.hide();
            fabAction.setOnClickListener(null);
            fabAction.setOnLongClickListener(null);
        }
    }

    private void setupFab() {
        fabAction.hide();
    }

    private void showCreateMasterDialog() {
        CreateMasterTraceabilityDialog dialog = CreateMasterTraceabilityDialog.newInstance();
        dialog.setOnMasterTraceabilityCreatedListener(master -> {
            Toast.makeText(this, "Master created: " + master.getMasterBatchNumber(), Toast.LENGTH_LONG).show();
            if (masterTraceabilityFragment != null) {
                // Refresh the master list
                // You'll need to add a refreshData() method to MasterTraceabilityFragment
            }
        });
        dialog.show(getSupportFragmentManager(), "CreateMasterTraceabilityDialog");
    }

    // ViewPager Adapter
    private static class AnalyticsPagerAdapter extends FragmentStateAdapter {

        private final QualityMetricsFragment metricsFragment;
        private final ReportsFragment reportsFragment;
        private final TraceabilityFragment traceabilityFragment;
        private final MasterTraceabilityFragment masterTraceabilityFragment;
        private final String[] tabKeys;

        public AnalyticsPagerAdapter(AnalyticsHubActivity activity, String[] tabKeys) {
            super(activity);
            this.tabKeys = tabKeys;
            metricsFragment = new QualityMetricsFragment();
            reportsFragment = new ReportsFragment();
            traceabilityFragment = new TraceabilityFragment();
            masterTraceabilityFragment = new MasterTraceabilityFragment();
        }

        String getTabKey(int position) { return tabKeys[position]; }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (tabKeys[position]) {
                case "QUALITY": return metricsFragment;
                case "TRACEABILITY": return traceabilityFragment;
                case "BULK": return masterTraceabilityFragment;
                case "REPORTS": return reportsFragment;
                default: return metricsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return tabKeys.length;
        }

        public Fragment getFragment(int position) {
            switch (tabKeys[position]) {
                case "QUALITY": return metricsFragment;
                case "TRACEABILITY": return traceabilityFragment;
                case "BULK": return masterTraceabilityFragment;
                case "REPORTS": return reportsFragment;
                default: return metricsFragment;
            }
        }
    }
}