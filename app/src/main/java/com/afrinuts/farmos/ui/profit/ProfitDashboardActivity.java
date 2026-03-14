package com.afrinuts.farmos.ui.profit;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfitDashboardActivity extends BaseActivity {

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<RevenueEntity> allRevenues = new ArrayList<>();
    private List<ExpenseEntity> allExpenses = new ArrayList<>();
    private List<BlockEntity> allBlocks = new ArrayList<>();
    private ProfitAnalytics currentAnalytics;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // UI Elements
    private ChipGroup dateRangeChipGroup;
    private TextView totalRevenueValue;
    private TextView totalExpensesValue;
    private TextView netProfitValue;
    private TextView profitMarginValue;
    private TextView profitPerHectare;
    private TextView profitPerTree;
    private TextView marginPercent;
    private LinearLayout recentTransactionsContainer;
    private View progressBar;

    private String currentDateRange = "ALL_TIME"; // ALL_TIME, THIS_YEAR, THIS_MONTH

    @Override
    protected boolean checkRoleAccess() {
        // Everyone can view profit dashboard (owners, managers, investors)
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profit_dashboard);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Setup filters
        setupDateRangeFilters();

        // Load data
        loadData();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Profit Dashboard");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate back to MainActivity with proper flags
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    // Show empty state or error
                });
            }
        });
    }

    private void refreshAnalytics() {
        // This runs on UI thread, but filtering is fast (in-memory operations)
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

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recentTransactionsContainer != null) {
            recentTransactionsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        // Navigate back to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}