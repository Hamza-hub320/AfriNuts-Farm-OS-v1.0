package com.afrinuts.farmos.ui.expenses;

import android.graphics.Color;
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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
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

public class ExpenseChartsActivity extends AppCompatActivity {

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<ExpenseEntity> allExpenses;
    private List<BlockEntity> allBlocks;
    private Map<Long, String> blockNameMap = new HashMap<>();

    // UI Elements
    private TextView totalExpensesValue;
    private TextView costPerHectareValue;
    private TextView costPerTreeValue;
    private PieChart pieChart;
    private BarChart barChart;
    private LinearLayout topExpensesContainer;
    private ChipGroup chartFilterGroup;

    private String currentFilter = "ALL_TIME"; // ALL_TIME, THIS_YEAR, THIS_MONTH

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_charts);

        // Initialize views
        initViews();

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

        totalExpensesValue = findViewById(R.id.totalExpensesValue);
        costPerHectareValue = findViewById(R.id.costPerHectareValue);
        costPerTreeValue = findViewById(R.id.costPerTreeValue);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        topExpensesContainer = findViewById(R.id.topExpensesContainer);
        chartFilterGroup = findViewById(R.id.chartFilterGroup);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Expense Analytics");
        }
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
        new Thread(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                // Load all blocks for name mapping
                allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                for (BlockEntity block : allBlocks) {
                    blockNameMap.put(block.getId(), block.getBlockName());
                }

                // Load all expenses
                allExpenses = database.expenseDao().getExpensesByFarmId(currentFarm.getId());

                runOnUiThread(this::refreshCharts);
            }
        }).start();
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

        totalExpensesValue.setText(String.format(Locale.getDefault(), "%,.0f XAF", total));
        costPerHectareValue.setText(String.format(Locale.getDefault(), "%,.0f XAF", costPerHa));
        costPerTreeValue.setText(String.format(Locale.getDefault(), "%,.0f XAF", costPerTree));
    }

    private void updatePieChart(List<ExpenseEntity> expenses) {
        Map<ExpenseEntity.ExpenseCategory, Double> categoryTotals = new HashMap<>();

        // Sum amounts per category
        for (ExpenseEntity expense : expenses) {
            ExpenseEntity.ExpenseCategory category = expense.getCategory();
            double current = categoryTotals.getOrDefault(category, 0.0);
            categoryTotals.put(category, current + expense.getAmount());
        }

        // If no data, show empty chart
        if (categoryTotals.isEmpty()) {
            pieChart.clear();
            pieChart.setCenterText("No expense data");
            pieChart.invalidate();
            return;
        }

        // Assign distinct, consistent color per category
        Map<ExpenseEntity.ExpenseCategory, Integer> categoryColorMap = new HashMap<>();
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.LAND_CLEARING, ContextCompat.getColor(this, R.color.primary));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.PLOWING, ContextCompat.getColor(this, R.color.accent));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.SEEDLINGS, ContextCompat.getColor(this, R.color.navy));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.LABOR, ContextCompat.getColor(this, R.color.teal));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.SECURITY, ContextCompat.getColor(this, R.color.olive));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.FENCING, ContextCompat.getColor(this, R.color.dark_orange));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.FERTILIZER, Color.parseColor("#9C27B0"));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.IRRIGATION, Color.parseColor("#FF9800"));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.EQUIPMENT, Color.parseColor("#F44336"));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.MAINTENANCE, Color.parseColor("#3F51B5"));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.PROCESSING_CENTER, Color.parseColor("#009688"));
        categoryColorMap.put(ExpenseEntity.ExpenseCategory.OTHER, Color.parseColor("#9E9E9E"));

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (Map.Entry<ExpenseEntity.ExpenseCategory, Double> entry : categoryTotals.entrySet()) {
            ExpenseEntity.ExpenseCategory category = entry.getKey();

            // Use display name instead of icon for legend
            entries.add(new PieEntry(
                    entry.getValue().floatValue(),
                    category.getDisplayName()
            ));

            colors.add(categoryColorMap.getOrDefault(
                    category,
                    ContextCompat.getColor(this, R.color.primary)
            ));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000000) {
                    return String.format(Locale.getDefault(), "%.1fM", value / 1000000);
                } else if (value >= 1000) {
                    return String.format(Locale.getDefault(), "%.0fK", value / 1000);
                } else {
                    return String.format(Locale.getDefault(), "%.0f", value);
                }
            }
        });

        // Calculate total for center text
        double total = 0;
        for (Double value : categoryTotals.values()) {
            total += value;
        }

        String centerText;
        if (total >= 1000000) {
            centerText = String.format(Locale.getDefault(), "Total\n%.1fM XAF", total / 1000000);
        } else if (total >= 1000) {
            centerText = String.format(Locale.getDefault(), "Total\n%.0fK XAF", total / 1000);
        } else {
            centerText = String.format(Locale.getDefault(), "Total\n%.0f XAF", total);
        }

        // Configure pie chart
        pieChart.setData(data);
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setTransparentCircleColor(Color.LTGRAY);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setCenterText(centerText);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(ContextCompat.getColor(this, R.color.text_dark));
        pieChart.setMaxAngle(360f);

        // Simple legend configuration - avoid custom entries if they cause issues
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);
        legend.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);

        // Don't set custom legend - let the chart generate it automatically
        // This avoids the IndexOutOfBoundsException

        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void updateBarChart(List<ExpenseEntity> expenses) {
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
        ArrayList<Integer> barColors = new ArrayList<>();

        // Create gradient of primary color
        int primaryColor = ContextCompat.getColor(this, R.color.primary);
        int accentColor = ContextCompat.getColor(this, R.color.accent);

        for (int i = 0; i < sortedMonths.size(); i++) {
            Map.Entry<String, Float> entry = sortedMonths.get(i);
            barEntries.add(new BarEntry(i, entry.getValue()));
            labels.add(entry.getKey());

            // Alternate colors for visual interest
            if (i % 2 == 0) {
                barColors.add(primaryColor);
            } else {
                barColors.add(accentColor);
            }
        }

        BarDataSet dataSet = new BarDataSet(barEntries, "Monthly Expenses");
        dataSet.setColors(barColors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000000) {
                    return String.format(Locale.getDefault(), "%.1fM", value / 1000000);
                } else if (value >= 1000) {
                    return String.format(Locale.getDefault(), "%.0fK", value / 1000);
                } else {
                    return String.format(Locale.getDefault(), "%.0f", value);
                }
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_dark));

        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.getAxisLeft().setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000000) {
                    return String.format(Locale.getDefault(), "%.1fM", value / 1000000);
                } else if (value >= 1000) {
                    return String.format(Locale.getDefault(), "%.0fK", value / 1000);
                }
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        barChart.invalidate();

        // Add custom legend for bar chart
        Legend barLegend = barChart.getLegend();
        barLegend.setEnabled(true);
        barLegend.setTextSize(12f);
        barLegend.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        barLegend.setForm(Legend.LegendForm.SQUARE);
        barLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        barLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
    }

    private void updateTopExpensesList(List<ExpenseEntity> expenses) {
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

            tvCategory.setText(expense.getCategory().getIcon() + " " + expense.getCategory().getDisplayName());
            tvAmount.setText(String.format(Locale.getDefault(), "%,.0f XAF", expense.getAmount()));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(expense.getDate())));

            topExpensesContainer.addView(itemView);
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