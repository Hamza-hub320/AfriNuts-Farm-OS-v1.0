package com.afrinuts.farmos.ui.finance;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.expenses.ExpensesFragment;
import com.afrinuts.farmos.ui.revenue.RevenueFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class FinanceHubActivity extends BaseHubActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;
    private FinancePagerAdapter pagerAdapter;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_finance_hub;
    }

    @Override
    protected int getCurrentNavItem() {
        return 2; // Finance tab is position 2
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
        pagerAdapter = new FinancePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Expenses");
                tab.setIcon(R.drawable.ic_money_off);
            } else if (position == 1) {
                tab.setText("Revenue");
                tab.setIcon(R.drawable.ic_attach_money);
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
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setImageResource(R.drawable.ic_add_business);
            fabAdd.setOnClickListener(v -> {
                ExpensesFragment fragment = (ExpensesFragment) pagerAdapter.getFragment(position);
                if (fragment != null) {
                    fragment.showAddExpenseDialog();
                } else {
                    Toast.makeText(this, "Error opening expense dialog", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (position == 1) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setImageResource(R.drawable.ic_add_business);
            fabAdd.setOnClickListener(v -> {
                RevenueFragment fragment = (RevenueFragment) pagerAdapter.getFragment(position);
                if (fragment != null) {
                    fragment.showAddRevenueDialog();
                } else {
                    Toast.makeText(this, "Error opening revenue dialog", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupFab() {
        // Initial state (Expenses tab)
        fabAdd.setVisibility(View.VISIBLE);
        fabAdd.setImageResource(R.drawable.ic_add);
        fabAdd.setOnClickListener(v -> {
            ExpensesFragment fragment = (ExpensesFragment) pagerAdapter.getFragment(0);
            if (fragment != null) {
                fragment.showAddExpenseDialog();
            }
        });
    }

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected String getAccessDeniedMessage() {
        return "Expenses are only accessible to Owners, Managers and CEO";
    }

    // ViewPager Adapter
    private static class FinancePagerAdapter extends FragmentStateAdapter {

        private final ExpensesFragment expensesFragment;
        private final RevenueFragment revenueFragment;

        public FinancePagerAdapter(FinanceHubActivity activity) {
            super(activity);
            expensesFragment = new ExpensesFragment();
            revenueFragment = new RevenueFragment();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return expensesFragment;
            } else {
                return revenueFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        public Fragment getFragment(int position) {
            if (position == 0) {
                return expensesFragment;
            } else {
                return revenueFragment;
            }
        }
    }
}
