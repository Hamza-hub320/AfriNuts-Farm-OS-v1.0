package com.afrinuts.farmos.ui.expenses;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseChartsActivity extends AppCompatActivity {

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<ExpenseEntity> allExpenses;
    private List<BlockEntity> allBlocks;
    private Map<Long, String> blockNameMap = new HashMap<>();
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // UI Elements
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView totalExpensesValue;
    private TextView costPerHectareValue;
    private TextView costPerTreeValue;
    private PieChart pieChart;
    private BarChart barChart;
    private LinearLayout topExpensesContainer;
    private ChipGroup chartFilterGroup;
    private View progressBar;

    private String currentFilter = "ALL_TIME"; // ALL_TIME, THIS_YEAR, THIS_MONTH

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_charts);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup swipe to refresh
        setupSwipeRefresh();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Setup filter listeners
        setupFilters();

        // Load data
        loadData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Expense Analytics");
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        totalExpensesValue = findViewById(R.id.totalExpensesValue);
        costPerHectareValue = findViewById(R.id.costPerHectareValue);
        costPerTreeValue = findViewById(R.id.costPerTreeValue);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        topExpensesContainer = findViewById(R.id.topExpensesContainer);
        chartFilterGroup = findViewById(R.id.chartFilterGroup);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Expense Analytics");
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });

        // FIXED: Use ContextCompat.getColor() instead of getResources().getColor()
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_refresh) {
            refreshData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFilters() {
        Chip chipAllTime = findViewById(R.id.chipAllTime);
        Chip chipThisYear = findViewById(R.id.chipThisYear);
        Chip chipThisMonth = findViewById(R.id.chipThisMonth);

        chipAllTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL_TIME";
                refreshCharts();
            }
        });

        chipThisYear.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "THIS_YEAR";
                refreshCharts();
            }
        });

        chipThisMonth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "THIS_MONTH";
                refreshCharts();
            }
        });
    }

    private void loadData() {
        showLoading(true);

        databaseExecutor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();

            if (farm != null) {
                // Load all blocks for name mapping
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());
                Map<Long, String> blockMap = new HashMap<>();
                for (BlockEntity block : blocks) {
                    blockMap.put(block.getId(), block.getBlockName());
                }

                // Load all expenses
                List<ExpenseEntity> expenses = database.expenseDao().getExpensesByFarmId(farm.getId());

                runOnUiThread(() -> {
                    currentFarm = farm;
                    allBlocks = blocks;
                    blockNameMap = blockMap;
                    allExpenses = expenses;

                    showLoading(false);
                    refreshCharts();

                    // Stop refresh animation
                    swipeRefreshLayout.setRefreshing(false);
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(ExpenseChartsActivity.this, "Farm data not found", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void refreshCharts() {
        // Filter expenses based on selected time period
        List<ExpenseEntity> filteredExpenses = filterExpensesByTime(allExpenses);

        // Update summary cards
        updateSummaryCards(filteredExpenses);

        // Update pie chart
        updatePieChart(filteredExpenses);

        // Update bar chart
        updateBarChart(filteredExpenses);

        // Update top expenses list
        updateTopExpensesList(filteredExpenses);
    }

    private List<ExpenseEntity> filterExpensesByTime(List<ExpenseEntity> expenses) {
        if (expenses == null) return new ArrayList<>();
        if (currentFilter.equals("ALL_TIME")) {
            return expenses;
        }

        Calendar calendar = Calendar.getInstance();
        long startTime;

        if (currentFilter.equals("THIS_YEAR")) {
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

    private void updateSummaryCards(List<ExpenseEntity> expenses) {
        double total = 0;
        for (ExpenseEntity expense : expenses) {
            total += expense.getAmount();
        }

        double hectares = currentFarm != null ? currentFarm.getTotalHectares() : 35;
        double trees = currentFarm != null ?
                currentFarm.getCashewHectares() * currentFarm.getTreesPerHectare() : 3500;

        double costPerHa = total / hectares;
        double costPerTree = total / trees;

        // Format with abbreviations for large numbers
        totalExpensesValue.setText(formatLargeNumber(total) + " XAF");
        costPerHectareValue.setText(formatLargeNumber(costPerHa) + " XAF");
        costPerTreeValue.setText(formatLargeNumber(costPerTree) + " XAF");
    }

    // Helper method to format large numbers
    private String formatLargeNumber(double value) {
        if (value >= 1_000_000_000) { // Billions
            return String.format(Locale.getDefault(), "%.1fB", value / 1_000_000_000);
        } else if (value >= 1_000_000) { // Millions
            return String.format(Locale.getDefault(), "%.0fM", value / 1_000_000);
        } else if (value >= 1_000) { // Thousands
            return String.format(Locale.getDefault(), "%.0fK", value / 1_000);
        } else {
            return String.format(Locale.getDefault(), "%.0f", value);
        }
    }

    private void updatePieChart(List<ExpenseEntity> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            pieChart.clear();
            pieChart.invalidate();
            return;
        }

        Map<ExpenseEntity.ExpenseCategory, Double> categoryTotals = new HashMap<>();

        for (ExpenseEntity expense : expenses) {
            ExpenseEntity.ExpenseCategory category = expense.getCategory();
            double current = categoryTotals.getOrDefault(category, 0.0);
            categoryTotals.put(category, current + expense.getAmount());
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<ExpenseEntity.ExpenseCategory, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(),
                    entry.getKey().getDisplayName()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expenses by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f XAF", value);
            }
        });

        pieChart.setData(data);
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(35f);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void updateBarChart(List<ExpenseEntity> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            barChart.clear();
            barChart.invalidate();
            return;
        }

        Map<String, Float> monthlyTotals = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());

        for (ExpenseEntity expense : expenses) {
            String monthYear = sdf.format(new Date(expense.getDate()));
            float current = monthlyTotals.getOrDefault(monthYear, 0f);
            monthlyTotals.put(monthYear, current + (float) expense.getAmount());
        }

        // Sort months chronologically
        List<Map.Entry<String, Float>> sortedMonths = new ArrayList<>(monthlyTotals.entrySet());
        sortedMonths.sort((e1, e2) -> {
            try {
                Date d1 = new SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(e1.getKey());
                Date d2 = new SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(e2.getKey());
                return d1.compareTo(d2);
            } catch (Exception e) {
                return 0;
            }
        });

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < sortedMonths.size(); i++) {
            Map.Entry<String, Float> entry = sortedMonths.get(i);
            barEntries.add(new BarEntry(i, entry.getValue()));
            labels.add(entry.getKey());
        }

        BarDataSet dataSet = new BarDataSet(barEntries, "Monthly Expenses");
        dataSet.setColors(ContextCompat.getColor(this, R.color.primary));
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0fK", value / 1000);
            }
        });

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45f);

        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void updateTopExpensesList(List<ExpenseEntity> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            topExpensesContainer.removeAllViews();
            return;
        }

        // Sort expenses by amount (descending)
        List<ExpenseEntity> sorted = new ArrayList<>(expenses);
        sorted.sort((e1, e2) -> Double.compare(e2.getAmount(), e1.getAmount()));

        // Take top 5
        int limit = Math.min(5, sorted.size());
        sorted = sorted.subList(0, limit);

        topExpensesContainer.removeAllViews();

        for (ExpenseEntity expense : sorted) {
            View itemView = getLayoutInflater().inflate(R.layout.item_top_expense, topExpensesContainer, false);

            TextView tvCategory = itemView.findViewById(R.id.tvTopCategory);
            TextView tvAmount = itemView.findViewById(R.id.tvTopAmount);
            TextView tvDate = itemView.findViewById(R.id.tvTopDate);

            tvCategory.setText(expense.getCategory().getDisplayName());
            tvAmount.setText(String.format(Locale.getDefault(), "%,.0f XAF", expense.getAmount()));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(expense.getDate())));

            topExpensesContainer.addView(itemView);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}