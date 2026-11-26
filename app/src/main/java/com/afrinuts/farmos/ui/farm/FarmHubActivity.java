package com.afrinuts.farmos.ui.farm;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.blocks.BlocksFragment;
import com.afrinuts.farmos.ui.tasks.TasksFragment;
import com.afrinuts.farmos.ui.workers.WorkerDashboardFragment;
import com.afrinuts.farmos.ui.suppliers.SuppliersFragment;
import com.afrinuts.farmos.data.remote.WeatherResponse;
import com.afrinuts.farmos.data.repository.WeatherRepository;
import com.afrinuts.farmos.ui.settings.SettingsActivity;
import com.afrinuts.farmos.ui.users.UsersListActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FarmHubActivity extends BaseHubActivity implements
        com.afrinuts.farmos.ui.blocks.BlocksFragment.BlockSelectionListener,
        com.afrinuts.farmos.ui.blocks.BlockDetailFragment.Listener {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;
    private FarmPagerAdapter pagerAdapter;

    // Track which tabs are visible based on role
    private List<TabItem> visibleTabs = new ArrayList<>();
    private int currentPosition = 0;
    private android.widget.FrameLayout detailContainer;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_farm_hub;
    }

    @Override
    protected int getCurrentNavItem() {
        return 1;
    }

    @Override
    protected void onHubCreate(Bundle savedInstanceState) {
        determineVisibleTabs();
        initViews();
        setupViewPager();
        setupFab();
        setupFarmHeader();
    }

    private void setupFarmHeader() {
        TextView date = findViewById(R.id.farmDate);
        TextView weatherChip = findViewById(R.id.farmWeatherChip);
        View profile = findViewById(R.id.farmProfileButton);
        if (profile instanceof TextView) ((TextView) profile).setText(new com.afrinuts.farmos.data.local.entity.UserEntity(sessionManager.getUserName(), null, null, null, null).getInitials());
        if (date != null) {
            date.setText(new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date()) + " · Odienné");
        }
        if (profile != null) {
            profile.setOnClickListener(v -> showFarmAccountMenu(profile));
        }
        if (detailContainer != null && weatherChip != null) {
            android.graphics.drawable.Drawable weatherIcon = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_wb_sunny);
            if (weatherIcon != null) {
                int iconSize = dp(19);
                weatherIcon.setBounds(0, 0, iconSize, iconSize);
                androidx.core.graphics.drawable.DrawableCompat.setTint(weatherIcon, 0xFFCC8A18);
                weatherChip.setCompoundDrawables(weatherIcon, null, null, null);
                weatherChip.setCompoundDrawablePadding(dp(8));
            }
        }
        if (weatherChip == null) return;

        WeatherRepository repository = new WeatherRepository(this);
        repository.getCurrentWeather(new WeatherRepository.WeatherCallback() {
            @Override public void onSuccess(WeatherResponse weather) {
                if (weather == null || weather.getCurrent() == null) return;
                runOnUiThread(() -> {
                    String condition = weather.getCurrent().getCondition() == null ? "" : weather.getCurrent().getCondition().getText();
                    weatherChip.setText(String.format(Locale.getDefault(), "%.0f° · %s", weather.getCurrent().getTempC(), condition));
                    weatherChip.setOnClickListener(v -> {
                        Intent intent = new Intent(FarmHubActivity.this, com.afrinuts.farmos.ui.weather.WeatherActivity.class);
                        intent.putExtra("weather_json", new Gson().toJson(weather));
                        startActivity(intent);
                    });
                });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    weatherChip.setText("Weather");
                    weatherChip.setOnClickListener(v -> startActivity(new Intent(FarmHubActivity.this,
                            com.afrinuts.farmos.ui.weather.WeatherActivity.class)));
                });
            }
        });
    }

    private void showFarmAccountMenu(View anchor) {
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
                    startActivity(new Intent(this, com.afrinuts.farmos.ui.sync.SyncCenterActivity.class)));
        View divider = new View(this);
        divider.setBackgroundColor(0xFFE9E1CF);
        menu.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        addMenuRow(menu, popup, "Logout", R.drawable.ic_logout, () -> com.afrinuts.farmos.ui.LogoutConfirmationDialog.show(this, () -> {
                    sessionManager.logout();
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }));
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
        icon.setColorFilter(title.equals("Logout") ? 0xFFB3402A : 0xFF1C1B1A);
        row.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView text = new TextView(this);
        text.setText(title);
        text.setTextColor(title.equals("Logout") ? 0xFFB3402A : 0xFF1C1B1A);
        text.setTextSize(15);
        text.setTypeface(Typeface.create(ResourcesCompat.getFont(this, R.font.manrope_medium), Typeface.NORMAL));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMarginStart(dp(12));
        row.addView(text, textParams);
        menu.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        row.setOnClickListener(v -> { popup.dismiss(); action.run(); });
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    /**
     * Determine which tabs to show based on user role
     */
    private void determineVisibleTabs() {
        visibleTabs.clear();

        for (String key : com.afrinuts.farmos.utils.AccessMap.farmTabs(sessionManager.getUserRole())) {
            switch (key) {
                case "BLOCKS": visibleTabs.add(new TabItem("Blocks", R.drawable.ic_view_list, TabType.BLOCKS)); break;
                case "SUPPLIERS": visibleTabs.add(new TabItem("Suppliers", R.drawable.ic_people, TabType.SUPPLIERS)); break;
                case "TASKS": visibleTabs.add(new TabItem("Tasks", R.drawable.ic_task, TabType.TASKS)); break;
                case "WORKERS": visibleTabs.add(new TabItem("Workers", R.drawable.ic_people, TabType.WORKERS)); break;
                default: throw new IllegalStateException("Unknown Farm tab: " + key);
            }
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fabAdd = findViewById(R.id.fabAdd);
        detailContainer = findViewById(R.id.detailContainer);

        // Initially hide FAB until we know which tab is active
        fabAdd.hide();
    }

    private void setupViewPager() {
        pagerAdapter = new FarmPagerAdapter(this, visibleTabs);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            TabItem tabItem = visibleTabs.get(position);
            tab.setText(tabItem.title);
            TextView label = new TextView(this);
            label.setTextAppearance(R.style.TextAppearance_FinanceTab);
            label.setText(tabItem.title);
            label.setGravity(Gravity.CENTER);
            label.setIncludeFontPadding(false);
            label.setTextColor(position == viewPager.getCurrentItem() ? Color.WHITE : 0xFF59544C);
            label.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tab.setCustomView(label);
        }).attach();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { setFarmTabLabelColor(tab, true); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { setFarmTabLabelColor(tab, false); }
            @Override public void onTabReselected(TabLayout.Tab tab) { setFarmTabLabelColor(tab, true); }
        });
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) setFarmTabLabelColor(tab, tab.isSelected());
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updateFabForTab(position);
                if (detailContainer != null && visibleTabs.get(position).type != TabType.BLOCKS) {
                    showDetailPlaceholder();
                }
            }
        });
    }

    @Override public boolean onBlockSelected(com.afrinuts.farmos.data.local.entity.BlockEntity block) {
        if (detailContainer == null || block == null) return false;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContainer, com.afrinuts.farmos.ui.blocks.BlockDetailFragment.newInstance(block.getId()), "tablet-block-detail")
                .commit();
        return true;
    }

    @Override public void onBlockDeleted(long blockId) {
        showDetailPlaceholder();
        Toast.makeText(this, "Block deleted", Toast.LENGTH_SHORT).show();
    }

    private void showDetailPlaceholder() {
        if (detailContainer == null) return;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailContainer, new DetailPlaceholderFragment())
                .commit();
    }

    public static class DetailPlaceholderFragment extends Fragment {
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                android.view.ViewGroup container, Bundle state) {
            TextView empty = new TextView(requireContext());
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(32), dp(32), dp(32), dp(32));
            empty.setText("Choose a supplier, block, or task to view its details.");
            empty.setTextColor(0xFF59544C);
            empty.setTextSize(18);
            return empty;
        }
        private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    }

    private void setFarmTabLabelColor(TabLayout.Tab tab, boolean selected) {
        View customView = tab == null ? null : tab.getCustomView();
        if (customView instanceof TextView) {
            ((TextView) customView).setTextColor(selected ? Color.WHITE : 0xFF59544C);
        }
    }

    private void updateFabForTab(int position) {
        if (position >= visibleTabs.size()) {
            fabAdd.hide();
            return;
        }

        TabItem tabItem = visibleTabs.get(position);

        // Clear any existing click listeners
        fabAdd.setOnClickListener(null);

        // Set FAB based on tab type
        if (tabItem.type == TabType.BLOCKS && com.afrinuts.farmos.utils.AccessMap.canUseFab("ADD_BLOCK", sessionManager.getUserRole())) {
            fabAdd.setImageResource(R.drawable.ic_add);
            fabAdd.setOnClickListener(v -> {
                Fragment fragment = pagerAdapter.getFragment(position);
                if (fragment instanceof BlocksFragment) {
                    ((BlocksFragment) fragment).showAddBlockDialog();
                } else {
                    Toast.makeText(this, "Error: Fragment not ready", Toast.LENGTH_SHORT).show();
                }
            });
            fabAdd.show();
        }
        else if (tabItem.type == TabType.TASKS && com.afrinuts.farmos.utils.AccessMap.canUseFab("ADD_TASK", sessionManager.getUserRole())) {
            fabAdd.setImageResource(R.drawable.ic_add);
            fabAdd.setOnClickListener(v -> {
                Fragment fragment = pagerAdapter.getFragment(position);
                if (fragment instanceof TasksFragment) {
                    ((TasksFragment) fragment).showAddTaskDialog();
                } else {
                    Toast.makeText(this, "Error: Fragment not ready", Toast.LENGTH_SHORT).show();
                }
            });
            fabAdd.show();
        }
        else if (tabItem.type == TabType.SUPPLIERS && com.afrinuts.farmos.utils.AccessMap.canUseFab("ADD_SUPPLIER", sessionManager.getUserRole())) {
            fabAdd.setImageResource(R.drawable.ic_add);
            fabAdd.setOnClickListener(v -> {
                Fragment fragment = pagerAdapter.getFragment(position);
                if (fragment instanceof SuppliersFragment && fragment.isAdded()
                        && fragment.getView() != null) {
                    ((SuppliersFragment) fragment).showAddSupplierDialog();
                }
                else {
                    Toast.makeText(this, "Error: Fragment not ready", Toast.LENGTH_SHORT).show();
                }
            });
            fabAdd.show();
        }
        else {
            fabAdd.hide();
        }
    }

    private void setupFab() {
        if (visibleTabs.isEmpty()) {
            fabAdd.hide();
            return;
        }

        // Set initial state based on first tab
        // Delay to ensure fragment is ready
        viewPager.post(() -> {
            currentPosition = viewPager.getCurrentItem();
            updateFabForTab(currentPosition);
        });
    }

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected String getAccessDeniedMessage() {
        return "Farm operations are only accessible to Owners, Managers, CEO, Workers, and Field Officers";
    }

    // Tab types
    private enum TabType {
        BLOCKS, TASKS, WORKERS, SUPPLIERS
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
    private static class FarmPagerAdapter extends FragmentStateAdapter {

        private final List<TabItem> tabs;
        private final BlocksFragment blocksFragment;
        private final TasksFragment tasksFragment;
        private final WorkerDashboardFragment workerFragment;
        private final SuppliersFragment suppliersFragment; // Add SuppliersFragment

        public FarmPagerAdapter(FarmHubActivity activity, List<TabItem> tabs) {
            super(activity);
            this.tabs = tabs;

            // Create fragments
            this.blocksFragment = new BlocksFragment();
            this.tasksFragment = new TasksFragment();
            this.workerFragment = new WorkerDashboardFragment();
            this.suppliersFragment = new SuppliersFragment(); // Initialize SuppliersFragment
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            TabItem tab = tabs.get(position);
            switch (tab.type) {
                case BLOCKS:
                    return blocksFragment;
                case TASKS:
                    return tasksFragment;
                case WORKERS:
                    return workerFragment;
                case SUPPLIERS:
                    return suppliersFragment;
                default:
                    return tasksFragment;
            }
        }

        @Override
        public int getItemCount() {
            return tabs.size();
        }

        public Fragment getFragment(int position) {
            TabItem tab = tabs.get(position);
            switch (tab.type) {
                case BLOCKS:
                    return blocksFragment;
                case TASKS:
                    return tasksFragment;
                case WORKERS:
                    return workerFragment;
                case SUPPLIERS:
                    return suppliersFragment;
                default:
                    return tasksFragment;
            }
        }
    }
}
