package com.afrinuts.farmos.ui.farm;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.blocks.BlocksFragment;
import com.afrinuts.farmos.ui.tasks.TasksFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class FarmHubActivity extends BaseHubActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;
    private FarmPagerAdapter pagerAdapter;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_farm_hub;
    }

    @Override
    protected int getCurrentNavItem() {
        return 1; // Farm tab is position 1
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
        fabAdd = findViewById(R.id.fabAdd);
    }

    private void setupViewPager() {
        pagerAdapter = new FarmPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Blocks");
                tab.setIcon(R.drawable.ic_view_list);
            } else {
                tab.setText("Tasks");
                tab.setIcon(R.drawable.ic_task);
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
        if (position == 0) {
            // Blocks tab
            fabAdd.setImageResource(R.drawable.ic_park);
            fabAdd.setOnClickListener(v -> {
                BlocksFragment fragment = (BlocksFragment) pagerAdapter.getFragment(position);
                if (fragment != null) {
                    fragment.showAddBlockDialog();
                } else {
                    Toast.makeText(this, "Error opening block dialog", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Tasks tab
            fabAdd.setImageResource(R.drawable.ic_check_circle);
            fabAdd.setOnClickListener(v -> {
                TasksFragment fragment = (TasksFragment) pagerAdapter.getFragment(position);
                if (fragment != null) {
                    fragment.showAddTaskDialog();
                } else {
                    Toast.makeText(this, "Error opening task dialog", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupFab() {
        // Initial state (Blocks tab)
        fabAdd.setImageResource(R.drawable.ic_add);
        fabAdd.setOnClickListener(v -> {
            BlocksFragment fragment = (BlocksFragment) pagerAdapter.getFragment(0);
            if (fragment != null) {
                fragment.showAddBlockDialog();
            }
        });
    }

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    // ViewPager Adapter
    private static class FarmPagerAdapter extends FragmentStateAdapter {

        private final BlocksFragment blocksFragment;
        private final TasksFragment tasksFragment;

        public FarmPagerAdapter(FarmHubActivity activity) {
            super(activity);
            blocksFragment = new BlocksFragment();
            tasksFragment = new TasksFragment();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return blocksFragment;
            } else {
                return tasksFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        public Fragment getFragment(int position) {
            if (position == 0) {
                return blocksFragment;
            } else {
                return tasksFragment;
            }
        }
    }
}