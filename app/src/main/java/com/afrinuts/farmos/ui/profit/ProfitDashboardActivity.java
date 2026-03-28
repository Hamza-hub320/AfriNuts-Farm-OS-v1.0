package com.afrinuts.farmos.ui.profit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import android.widget.ProgressBar;
import com.afrinuts.farmos.ui.BaseHubActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfitDashboardActivity extends BaseHubActivity {

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<RevenueEntity> allRevenues = new ArrayList<>();
    private List<ExpenseEntity> allExpenses = new ArrayList<>();
    private List<BlockEntity> allBlocks = new ArrayList<>();
    private ProfitAnalytics currentAnalytics;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // UI Elements
    private SwipeRefreshLayout swipeRefreshLayout;
    private ChipGroup dateRangeChipGroup;
    private TextView totalRevenueValue;
    private TextView totalExpensesValue;
    private TextView netProfitValue;
    private TextView profitMarginValue;
    private TextView profitPerHectare;
    private TextView profitPerTree;
    private TextView marginPercent;
    private LinearLayout recentTransactionsContainer;
    private ProgressBar progressBar;
    private LinearLayout emptyView;

    private String currentDateRange = "ALL_TIME"; // ALL_TIME, THIS_YEAR, THIS_MONTH

    @Override
    protected boolean checkRoleAccess() {
        // Everyone can view profit dashboard (owners, managers, investors)
        return true;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_profit_dashboard;
    }

    @Override
    protected int getCurrentNavItem() {
        return 2; // Finance tab
    }

    @Override
    protected void onHubCreate(Bundle savedInstanceState) {
        database = AppDatabase.getInstance(this);
        initViews();
        setupSwipeRefresh();
        setupDateRangeFilters();
        loadData();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        dateRangeChipGroup = findViewById(R.id.dateRangeChipGroup);
        totalRevenueValue = findViewById(R.id.totalRevenueValue);
        totalExpensesValue = findViewById(R.id.totalExpensesValue);
        netProfitValue = findViewById(R.id.netProfitValue);
        profitMarginValue = findViewById(R.id.profitMarginValue);
        profitPerHectare = findViewById(R.id.profitPerHectare);
        profitPerTree = findViewById(R.id.profitPerTree);
        marginPercent = findViewById(R.id.marginPercent);
        recentTransactionsContainer = findViewById(R.id.recentTransactionsContainer);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
    }


    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });

        // Configure colors for the refresh indicator
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.primary),
                ContextCompat.getColor(this, R.color.accent),
                ContextCompat.getColor(this, android.R.color.holo_orange_dark),
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
        );
    }

    private void refreshData() {
        // Show refresh indicator
        swipeRefreshLayout.setRefreshing(true);

        // Reload data
        loadData();
    }


    private void setupDateRangeFilters() {
        Chip chipAllTime = findViewById(R.id.chipAllTime);
        Chip chipThisYear = findViewById(R.id.chipThisYear);
        Chip chipThisMonth = findViewById(R.id.chipThisMonth);

        chipAllTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentDateRange = "ALL_TIME";
                refreshAnalytics();
            }
        });

        chipThisYear.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentDateRange = "THIS_YEAR";
                refreshAnalytics();
            }
        });

        chipThisMonth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentDateRange = "THIS_MONTH";
                refreshAnalytics();
            }
        });
    }

    private void loadData() {
        showLoading(true);

        databaseExecutor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();

            if (farm != null) {
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());
                List<RevenueEntity> revenues = database.revenueDao().getRevenuesByFarmId(farm.getId());
                List<ExpenseEntity> expenses = database.expenseDao().getExpensesByFarmId(farm.getId());

                runOnUiThread(() -> {
                    currentFarm = farm;
                    allBlocks.clear();
                    allBlocks.addAll(blocks);
                    allRevenues.clear();
                    allRevenues.addAll(revenues);
                    allExpenses.clear();
                    allExpenses.addAll(expenses);

                    showLoading(false);
                    refreshAnalytics();

                    // Stop refresh animation
                    swipeRefreshLayout.setRefreshing(false);
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    showEmptyState(true);
                });
            }
        });
    }

    private void refreshAnalytics() {
        // Filter data based on selected date range
        List<RevenueEntity> filteredRevenues = filterRevenuesByDate(allRevenues);
        List<ExpenseEntity> filteredExpenses = filterExpensesByDate(allExpenses);

        double hectares = currentFarm != null ? currentFarm.getTotalHectares() : 35;
        double trees = currentFarm != null ?
                currentFarm.getCashewHectares() * currentFarm.getTreesPerHectare() : 3500;

        currentAnalytics = new ProfitAnalytics(filteredRevenues, filteredExpenses, hectares, trees);
        updateUI();
    }

    private List<RevenueEntity> filterRevenuesByDate(List<RevenueEntity> revenues) {
        if (currentDateRange.equals("ALL_TIME")) return revenues;

        Calendar calendar = Calendar.getInstance();
        long startTime;

        if (currentDateRange.equals("THIS_YEAR")) {
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            startTime = calendar.getTimeInMillis();
        } else { // THIS_MONTH
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            startTime = calendar.getTimeInMillis();
        }

        List<RevenueEntity> filtered = new ArrayList<>();
        for (RevenueEntity revenue : revenues) {
            if (revenue.getHarvestDate() >= startTime) {
                filtered.add(revenue);
            }
        }
        return filtered;
    }

    private List<ExpenseEntity> filterExpensesByDate(List<ExpenseEntity> expenses) {
        if (currentDateRange.equals("ALL_TIME")) return expenses;

        Calendar calendar = Calendar.getInstance();
        long startTime;

        if (currentDateRange.equals("THIS_YEAR")) {
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            startTime = calendar.getTimeInMillis();
        } else { // THIS_MONTH
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            startTime = calendar.getTimeInMillis();
        }

        List<ExpenseEntity> filtered = new ArrayList<>();
        for (ExpenseEntity expense : expenses) {
            if (expense.getDate() >= startTime) {
                filtered.add(expense);
            }
        }
        return filtered;
    }

    private void updateUI() {
        if (currentAnalytics == null) return;

        // Summary
        totalRevenueValue.setText(String.format(Locale.getDefault(),
                "%,.0f XAF", currentAnalytics.getTotalRevenue()));
        totalExpensesValue.setText(String.format(Locale.getDefault(),
                "%,.0f XAF", currentAnalytics.getTotalExpenses()));
        netProfitValue.setText(String.format(Locale.getDefault(),
                "%,.0f XAF", currentAnalytics.getNetProfit()));

        String marginText = String.format(Locale.getDefault(),
                "%.1f%% margin", currentAnalytics.getProfitMargin());
        profitMarginValue.setText(marginText);

        // Per unit metrics
        profitPerHectare.setText(String.format(Locale.getDefault(),
                "%,.0f XAF", currentAnalytics.getProfitPerHectare()));
        profitPerTree.setText(String.format(Locale.getDefault(),
                "%,.0f XAF", currentAnalytics.getProfitPerTree()));
        marginPercent.setText(String.format(Locale.getDefault(),
                "%.1f%%", currentAnalytics.getProfitMargin()));

        // Recent transactions
        displayRecentTransactions();

        // Hide empty state if we have data
        showEmptyState(false);
    }

    private void displayRecentTransactions() {
        if (currentAnalytics == null || currentAnalytics.getRecentTransactions() == null) return;

        recentTransactionsContainer.removeAllViews();

        for (ProfitAnalytics.ProfitTransaction transaction :
                currentAnalytics.getRecentTransactions()) {

            View itemView = getLayoutInflater().inflate(
                    R.layout.item_recent_transaction, recentTransactionsContainer, false);

            ImageView ivTransactionIcon = itemView.findViewById(R.id.ivTransactionIcon);
            TextView tvTransactionDesc = itemView.findViewById(R.id.tvTransactionDesc);
            TextView tvTransactionDate = itemView.findViewById(R.id.tvTransactionDate);
            TextView tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount);

            // Set icon using the resource ID from the transaction
            ivTransactionIcon.setImageResource(transaction.getIconResId());

            // Set tint based on transaction type
            int tintColor = ContextCompat.getColor(this,
                    transaction.isRevenue() ? R.color.primary : android.R.color.holo_red_dark);
            ivTransactionIcon.setColorFilter(tintColor);

            tvTransactionDesc.setText(transaction.getDescription());
            tvTransactionDate.setText(transaction.getFormattedDate());

            String amountText = transaction.getSign() + " " + transaction.getFormattedAmount();
            tvTransactionAmount.setText(amountText);

            int color = ContextCompat.getColor(this, transaction.getAmountColor());
            tvTransactionAmount.setTextColor(color);

            // Add click listener
            itemView.setOnClickListener(v -> {
                if (transaction.isRevenue()) {
                    Intent intent = new Intent(
                            this, com.afrinuts.farmos.ui.revenue.RevenueDetailActivity.class);
                    intent.putExtra("revenue_id", transaction.getId());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(
                            this, com.afrinuts.farmos.ui.expenses.ExpenseDetailActivity.class);
                    intent.putExtra("expense_id", transaction.getId());
                    startActivity(intent);
                }
            });

            recentTransactionsContainer.addView(itemView);
        }
    }

    private void showEmptyState(boolean show) {
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recentTransactionsContainer != null) {
            recentTransactionsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recentTransactionsContainer != null) {
            recentTransactionsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (emptyView != null && !show) {
            emptyView.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}