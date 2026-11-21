package com.afrinuts.farmos.ui.finance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.core.content.res.ResourcesCompat;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.afrinuts.farmos.ui.expenses.ExpensesFragment;
import com.afrinuts.farmos.ui.revenue.RevenueFragment;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.ui.settings.SettingsActivity;
import com.afrinuts.farmos.ui.users.UsersListActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

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
        TextView date = findViewById(R.id.financeDateText);
        if (date != null) date.setText(new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date()));
        View profile = findViewById(R.id.financeProfileButton);
        if (profile != null) profile.setOnClickListener(v -> showAccountMenu(profile));
    }

    private void setupViewPager() {
        pagerAdapter = new FinancePagerAdapter(this,
                com.afrinuts.farmos.utils.AccessMap.hubTabs(2, sessionManager.getUserRole()));
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String key = pagerAdapter.getTabKey(position);
            if ("REVENUE".equals(key)) {
                tab.setText("Revenue");
            } else if ("EXPENSES".equals(key)) {
                tab.setText("Expenses");
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
        String action = pagerAdapter.getTabKey(position).equals("REVENUE") ? "ADD_REVENUE" : "ADD_EXPENSE";
        if (!com.afrinuts.farmos.utils.AccessMap.canUseFab(action, sessionManager.getUserRole())) {
            fabAdd.hide();
            fabAdd.setOnClickListener(null);
            return;
        }
        if ("REVENUE".equals(pagerAdapter.getTabKey(position))) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setImageResource(R.drawable.ic_add);
            fabAdd.setOnClickListener(v -> {
                RevenueFragment fragment = (RevenueFragment) pagerAdapter.getFragment(position);
                if (fragment != null) {
                    fragment.showAddRevenueDialog();
                } else {
                    Toast.makeText(this, "Could not open revenue form", Toast.LENGTH_SHORT).show();
                }
            });
        } else if ("EXPENSES".equals(pagerAdapter.getTabKey(position))) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setImageResource(R.drawable.ic_add);
            fabAdd.setOnClickListener(v -> {
                ExpensesFragment fragment = (ExpensesFragment) pagerAdapter.getFragment(position);
                if (fragment != null) {
                    fragment.showAddExpenseDialog();
                } else {
                    Toast.makeText(this, "Could not open expense form", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupFab() {
        updateFabForTab(viewPager.getCurrentItem());
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
        if (sessionManager.canManageUsers()) addMenuRow(menu, popup, "Team", R.drawable.ic_person_add, () ->
                startActivity(new Intent(this, UsersListActivity.class)));
        addMenuRow(menu, popup, "Settings", R.drawable.ic_settings, () ->
                startActivity(new Intent(this, SettingsActivity.class)));
        View divider = new View(this);
        divider.setBackgroundColor(0xFFE9E1CF);
        menu.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        addMenuRow(menu, popup, "Logout", R.drawable.ic_logout, () -> new MaterialAlertDialogBuilder(this)
                .setTitle("Logout").setMessage("Are you sure you want to logout?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.logout();
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }).show());
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
        text.setTypeface(Typeface.create(ResourcesCompat.getFont(this, R.font.manrope_medium), Typeface.NORMAL));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMarginStart(dp(12));
        row.addView(text, params);
        menu.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        row.setOnClickListener(v -> { popup.dismiss(); action.run(); });
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

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
        private final String[] tabKeys;

        public FinancePagerAdapter(FinanceHubActivity activity, String[] tabKeys) {
            super(activity);
            this.tabKeys = tabKeys;
            expensesFragment = new ExpensesFragment();
            revenueFragment = new RevenueFragment();
        }

        String getTabKey(int position) { return tabKeys[position]; }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return "REVENUE".equals(tabKeys[position]) ? revenueFragment : expensesFragment;
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        public Fragment getFragment(int position) {
            return "REVENUE".equals(tabKeys[position]) ? revenueFragment : expensesFragment;
        }
    }
}
