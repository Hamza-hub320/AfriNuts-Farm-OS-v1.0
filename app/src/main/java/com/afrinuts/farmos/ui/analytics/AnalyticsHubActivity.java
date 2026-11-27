package com.afrinuts.farmos.ui.analytics;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.quality.QualityMetricsFragment;
import com.afrinuts.farmos.ui.quality.QualityBatchDetailFragment;
import com.afrinuts.farmos.ui.quality.QualityBatchDialog;
import com.afrinuts.farmos.ui.reports.ReportsFragment;
import com.afrinuts.farmos.ui.reports.ReportDetailFragment;
import com.afrinuts.farmos.ui.traceability.CreateMasterTraceabilityDialog;
import com.afrinuts.farmos.ui.traceability.MasterTraceabilityFragment;
import com.afrinuts.farmos.ui.traceability.QRScannerActivity;
import com.afrinuts.farmos.ui.traceability.TraceabilityFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AnalyticsHubActivity extends BaseHubActivity implements QualityMetricsFragment.OnBatchSelectedListener, TraceabilityFragment.OnTraceabilitySelectedListener, MasterTraceabilityFragment.OnMasterSelectedListener, ReportsFragment.OnReportSelectedListener {

    public static final String EXTRA_OPEN_BATCH_ID = "analytics_open_quality_batch_id";

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAction;
    private AnalyticsPagerAdapter pagerAdapter;
    private QualityMetricsFragment qualityMetricsFragment;
    private TraceabilityFragment traceabilityFragment;
    private MasterTraceabilityFragment masterTraceabilityFragment;
    private ReportsFragment reportsFragment;

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
        long openBatchId = getIntent().getLongExtra(EXTRA_OPEN_BATCH_ID, -1);
        if (openBatchId >= 0) {
            int qualityPosition = -1;
            for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
                if ("QUALITY".equals(pagerAdapter.getTabKey(i))) { qualityPosition = i; break; }
            }
            if (qualityPosition >= 0) {
                viewPager.setCurrentItem(qualityPosition, false);
                long batchId = openBatchId;
                viewPager.post(() -> {
                    if (qualityMetricsFragment != null) qualityMetricsFragment.openBatchById(batchId);
                    else onBatchSelected(batchId);
                });
            }
        }
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
        TextView date = findViewById(R.id.analyticsDateText);
        if (date != null) {
            date.setText(new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date()));
        }
        View profile = findViewById(R.id.analyticsProfileButton);
        if (profile instanceof TextView) ((TextView) profile).setText(new com.afrinuts.farmos.data.local.entity.UserEntity(sessionManager.getUserName(), null, null, null, null).getInitials());
        if (profile != null) profile.setOnClickListener(v -> showAccountMenu(profile));
    }

    private void setupViewPager() {
        pagerAdapter = new AnalyticsPagerAdapter(this,
                com.afrinuts.farmos.utils.AccessMap.hubTabs(3, sessionManager.getUserRole()));
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String key = pagerAdapter.getTabKey(position);
            switch (key) {
                case "QUALITY": tab.setText("Quality"); break;
                case "TRACEABILITY": tab.setText(getResources().getConfiguration().smallestScreenWidthDp >= 600 ? "Traceability" : "Trace"); break;
                case "BULK": tab.setText("Bulk"); break;
                case "REPORTS": tab.setText("Reports"); break;
            }
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateFabForTab(position);
                // Other Analytics tabs can restore their last detail while
                // becoming active. Reapply the Reports selection after that
                // lifecycle work so the right pane always matches its list.
                if ("REPORTS".equals(pagerAdapter.getTabKey(position))) {
                    viewPager.postDelayed(() -> {
                        if (reportsFragment != null) {
                            onReportSelected(reportsFragment.getSelectedReportType());
                        }
                    }, 350);
                }
            }
        });

        for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
            switch (pagerAdapter.getTabKey(i)) {
                case "QUALITY": qualityMetricsFragment = (QualityMetricsFragment) pagerAdapter.getFragment(i); break;
                case "TRACEABILITY": traceabilityFragment = (TraceabilityFragment) pagerAdapter.getFragment(i); break;
                case "BULK": masterTraceabilityFragment = (MasterTraceabilityFragment) pagerAdapter.getFragment(i); break;
                case "REPORTS": reportsFragment = (ReportsFragment) pagerAdapter.getFragment(i); break;
            }
        }
    }

    @Override
    public void onReportSelected(String reportType) {
        View detailPane = findViewById(R.id.detailContainer);
        if (detailPane == null) return;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContainer);
        if (current instanceof ReportDetailFragment) {
            ((ReportDetailFragment) current).setReportType(reportType);
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContainer, ReportDetailFragment.newInstance(reportType), "analytics_report_detail")
                .commit();
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
            fabAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.accent)));
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
            fabAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.accent)));
            fabAction.setImageResource(R.drawable.ic_qr_code);
            fabAction.setOnLongClickListener(null);
            fabAction.setOnClickListener(v -> {
                if (traceabilityFragment != null) {
                    traceabilityFragment.showTraceabilityOptions();
                }
            });
        } else if ("BULK".equals(pagerAdapter.getTabKey(position))) {
            fabAction.show();
            fabAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.accent)));
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
        updateFabForTab(viewPager.getCurrentItem());
    }

    @Override
    public void onBatchSelected(long batchId) {
        View detailPane = findViewById(R.id.detailContainer);
        if (detailPane == null) return;
        if (batchId < 0) {
            Fragment currentDetail = getSupportFragmentManager().findFragmentById(R.id.detailContainer);
            if (currentDetail != null) getSupportFragmentManager().beginTransaction().remove(currentDetail).commit();
            return;
        }
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContainer);
        if (current instanceof QualityBatchDetailFragment
                && current.getArguments() != null
                && current.getArguments().getLong("batch_id", -1) == batchId) return;
        Fragment detail = QualityBatchDetailFragment.newInstance(batchId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContainer, detail, "quality_batch_detail_" + batchId)
                .commit();
    }

    @Override
    public void onTraceabilitySelected(long traceabilityId) {
        View detailPane = findViewById(R.id.detailContainer);
        if (detailPane == null) return;
        if (traceabilityId < 0) {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContainer);
            if (current != null) getSupportFragmentManager().beginTransaction().remove(current).commit();
            return;
        }
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContainer);
        if (current instanceof com.afrinuts.farmos.ui.traceability.TraceabilityDetailFragment
                && current.getArguments() != null
                && current.getArguments().getLong("traceability_id", -1) == traceabilityId) return;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContainer,
                        com.afrinuts.farmos.ui.traceability.TraceabilityDetailFragment.newInstance(traceabilityId),
                        "traceability_detail_" + traceabilityId)
                .commit();
    }

    @Override
    public void onMasterSelected(long masterId) {
        View detailPane = findViewById(R.id.detailContainer);
        if (detailPane == null) return;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.detailContainer);
        if (masterId < 0) {
            if (current != null) getSupportFragmentManager().beginTransaction().remove(current).commit();
            return;
        }
        if (current instanceof com.afrinuts.farmos.ui.traceability.MasterTraceabilityDetailFragment
                && current.getArguments() != null
                && current.getArguments().getLong("master_id", -1) == masterId) return;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContainer,
                        com.afrinuts.farmos.ui.traceability.MasterTraceabilityDetailFragment.newInstance(masterId),
                        "master_traceability_detail_" + masterId)
                .commit();
    }

    public void onMasterDeleted() {
        onMasterSelected(-1);
        if (masterTraceabilityFragment != null) masterTraceabilityFragment.refreshData();
    }

    public void openQualityBatch(long batchId) {
        if (viewPager == null || pagerAdapter == null) return;
        for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
            if ("QUALITY".equals(pagerAdapter.getTabKey(i))) {
                viewPager.setCurrentItem(i, false);
                long id = batchId;
                viewPager.post(() -> {
                    if (qualityMetricsFragment != null) qualityMetricsFragment.openBatchById(id);
                    else onBatchSelected(id);
                });
                return;
            }
        }
    }

    public void openTraceabilityBatch(long traceabilityId) {
        if (viewPager == null || pagerAdapter == null) return;
        for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
            if ("TRACEABILITY".equals(pagerAdapter.getTabKey(i))) {
                traceabilityFragment = (TraceabilityFragment) pagerAdapter.getFragment(i);
                viewPager.setCurrentItem(i, false);
                viewPager.post(() -> traceabilityFragment.openTraceabilityById(traceabilityId));
                return;
            }
        }
    }

    public void refreshTraceabilityList() {
        if (traceabilityFragment != null) traceabilityFragment.refreshData();
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
        if (sessionManager.canManageUsers()) {
            addMenuRow(menu, popup, "Team", R.drawable.ic_person_add, () ->
                    startActivity(new Intent(this, com.afrinuts.farmos.ui.users.UsersListActivity.class)));
        }
        addMenuRow(menu, popup, "Settings", R.drawable.ic_settings, () ->
                startActivity(new Intent(this, com.afrinuts.farmos.ui.settings.SettingsActivity.class)));
        if (sessionManager.isOwner() || sessionManager.isCEO() || sessionManager.isManager())
            addMenuRow(menu, popup, "Sync Center", R.drawable.ic_sync, () ->
                    startActivity(new Intent(this, com.afrinuts.farmos.ui.sync.SyncCenterActivity.class)));
        View divider = new View(this);
        divider.setBackgroundColor(0xFFE9E1CF);
        menu.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        addMenuRow(menu, popup, "Logout", R.drawable.ic_logout, () -> com.afrinuts.farmos.ui.LogoutConfirmationDialog.show(this, () -> {
                    sessionManager.logout();
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, com.afrinuts.farmos.ui.auth.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }));
        popup.setContentView(menu);
        popup.setWidth(dp(196));
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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
                android.content.res.ColorStateList.valueOf(0xFFEFEAE2), new ColorDrawable(Color.TRANSPARENT), null));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter("Logout".equals(title) ? 0xFFB3402A : 0xFF1C1B1A);
        row.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView text = new TextView(this);
        text.setText(title);
        text.setTextColor("Logout".equals(title) ? 0xFFB3402A : 0xFF1C1B1A);
        text.setTextSize(15);
        text.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_medium));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMarginStart(dp(12));
        row.addView(text, params);
        menu.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        row.setOnClickListener(v -> { popup.dismiss(); action.run(); });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public void showCreateMasterDialog() {
        CreateMasterTraceabilityDialog dialog = CreateMasterTraceabilityDialog.newInstance();
        dialog.setOnMasterTraceabilityCreatedListener(master -> {
            Toast.makeText(this, "Master created: " + master.getMasterBatchNumber(), Toast.LENGTH_LONG).show();
            if (masterTraceabilityFragment != null) masterTraceabilityFragment.refreshData();
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
