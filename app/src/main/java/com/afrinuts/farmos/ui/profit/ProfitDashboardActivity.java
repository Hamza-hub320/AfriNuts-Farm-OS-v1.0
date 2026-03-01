package com.afrinuts.farmos.ui.profit;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ProfitDashboardActivity extends AppCompatActivity {

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<RevenueEntity> allRevenues = new ArrayList<>();
    private List<ExpenseEntity> allExpenses = new ArrayList<>();
    private List<BlockEntity> allBlocks = new ArrayList<>();
    private ProfitAnalytics currentAnalytics;

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

    private String currentDateRange = "ALL_TIME"; // ALL_TIME, THIS_YEAR, THIS_MONTH

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
        Toolbar toolbar = findViewById(R.id.toolbar);
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
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profit Dashboard");
        }
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
        new Thread(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                allRevenues = database.revenueDao().getRevenuesByFarmId(currentFarm.getId());
                allExpenses = database.expenseDao().getExpensesByFarmId(currentFarm.getId());

                runOnUiThread(() -> refreshAnalytics());
            }
        }).start();
    }

    private void refreshAnalytics() {
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
        recentTransactionsContainer.removeAllViews();

        for (ProfitAnalytics.ProfitTransaction transaction :
                currentAnalytics.getRecentTransactions()) {

            View itemView = getLayoutInflater().inflate(
                    R.layout.item_recent_transaction, recentTransactionsContainer, false);

            TextView tvTransactionIcon = itemView.findViewById(R.id.tvTransactionIcon);
            TextView tvTransactionDesc = itemView.findViewById(R.id.tvTransactionDesc);
            TextView tvTransactionDate = itemView.findViewById(R.id.tvTransactionDate);
            TextView tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount);

            // Set icon based on type
            if (transaction.isRevenue()) {
                tvTransactionIcon.setText("💰");
            } else {
                tvTransactionIcon.setText("💸");
            }

            tvTransactionDesc.setText(transaction.getDescription());
            tvTransactionDate.setText(transaction.getFormattedDate());

            String amountText = transaction.getSign() + " " + transaction.getFormattedAmount();
            tvTransactionAmount.setText(amountText);

            int color = ContextCompat.getColor(this, transaction.getAmountColor());
            tvTransactionAmount.setTextColor(color);

            // Add click listener to navigate to original item
            itemView.setOnClickListener(v -> {
                if (transaction.isRevenue()) {
                    // Navigate to RevenueDetailActivity
                    android.content.Intent intent = new android.content.Intent(
                            this, com.afrinuts.farmos.ui.revenue.RevenueDetailActivity.class);
                    intent.putExtra("revenue_id", transaction.getId());
                    startActivity(intent);
                } else {
                    // Navigate to ExpenseDetailActivity
                    android.content.Intent intent = new android.content.Intent(
                            this, com.afrinuts.farmos.ui.expenses.ExpenseDetailActivity.class);
                    intent.putExtra("expense_id", transaction.getId());
                    startActivity(intent);
                }
            });

            recentTransactionsContainer.addView(itemView);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}