package com.afrinuts.farmos.ui.farm;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.blocks.BlocksFragment;
import com.afrinuts.farmos.ui.tasks.TasksFragment;
import com.afrinuts.farmos.ui.workers.WorkerDashboardFragment;
import com.afrinuts.farmos.ui.suppliers.SuppliersFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class FarmHubActivity extends BaseHubActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;
    private FarmPagerAdapter pagerAdapter;

    // Track which tabs are visible based on role
    private List<TabItem> visibleTabs = new ArrayList<>();
    private int currentPosition = 0;

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
    }

    /**
     * Determine which tabs to show based on user role
     */
    private void determineVisibleTabs() {
        visibleTabs.clear();

        // Owners and Managers see all tabs
        if (sessionManager.isOwner() || sessionManager.isManager()) {
            visibleTabs.add(new TabItem("Blocks", R.drawable.ic_view_list, TabType.BLOCKS));
            visibleTabs.add(new TabItem("Tasks", R.drawable.ic_task, TabType.TASKS));
            visibleTabs.add(new TabItem("Workers", R.drawable.ic_people, TabType.WORKERS));
            visibleTabs.add(new TabItem("Suppliers", R.drawable.ic_people, TabType.SUPPLIERS));
        }
        // Workers only see Tasks tab
        else if (sessionManager.isWorker()) {
            visibleTabs.add(new TabItem("Tasks", R.drawable.ic_task, TabType.TASKS));
        }
        // Field Officers only see Suppliers tab
        else if (sessionManager.isFieldOfficer()) {
            visibleTabs.add(new TabItem("Suppliers", R.drawable.ic_people, TabType.SUPPLIERS));
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fabAdd = findViewById(R.id.fabAdd);

        // Initially hide FAB until we know which tab is active
        fabAdd.hide();
    }

    private void setupViewPager() {
        pagerAdapter = new FarmPagerAdapter(this, visibleTabs);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            TabItem tabItem = visibleTabs.get(position);
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
        if (position >= visibleTabs.size()) {
            fabAdd.hide();
            return;
        }

        TabItem tabItem = visibleTabs.get(position);

        // Clear any existing click listeners
        fabAdd.setOnClickListener(null);

        // Set FAB based on tab type
        if (tabItem.type == TabType.BLOCKS && (sessionManager.isOwner() || sessionManager.isManager())) {
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
        else if (tabItem.type == TabType.TASKS) {
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
        else if (tabItem.type == TabType.SUPPLIERS && (sessionManager.isOwner() || sessionManager.isManager() || sessionManager.isFieldOfficer())) {
            fabAdd.setImageResource(R.drawable.ic_add);
            fabAdd.setOnClickListener(v -> {
                Fragment fragment = pagerAdapter.getFragment(position);
                if (fragment instanceof SuppliersFragment) {
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
        // Owners, Managers, and Workers can access Farm Hub
        // Investors are blocked
        return sessionManager.isOwner() || sessionManager.isManager() || sessionManager.isWorker()
                || sessionManager.isFieldOfficer();
    }

    @Override
    protected String getAccessDeniedMessage() {
        return "Farm operations are only accessible to Owners, Managers, Workers, and Field Officers";
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