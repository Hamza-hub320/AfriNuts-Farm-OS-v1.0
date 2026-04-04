package com.afrinuts.farmos.ui.expenses;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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

public class ExpenseChartsFragment extends Fragment {

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<ExpenseEntity> allExpenses;
    private List<BlockEntity> allBlocks;
    private ExpenseRepository expenseRepository;
    private Map<Long, String> blockNameMap = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView totalExpensesValue;
    private TextView costPerHectareValue;
    private TextView costPerTreeValue;
    private PieChart pieChart;
    private BarChart barChart;
    private LineChart lineChart;
    private LinearLayout topExpensesContainer;
    private ChipGroup chartFilterGroup;
    private View progressBar;
    private View emptyView;

    private String currentFilter = "ALL_TIME"; // ALL_TIME, THIS_YEAR, THIS_MONTH

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense_charts, container, false);

        database = AppDatabase.getInstance(requireContext());

        initViews(view);
        setupSwipeRefresh();
        setupFilters(view);
        loadData();

        return view;
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        totalExpensesValue = view.findViewById(R.id.totalExpensesValue);
        costPerHectareValue = view.findViewById(R.id.costPerHectareValue);
        costPerTreeValue = view.findViewById(R.id.costPerTreeValue);
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        lineChart = view.findViewById(R.id.lineChart);
        topExpensesContainer = view.findViewById(R.id.topExpensesContainer);
        chartFilterGroup = view.findViewById(R.id.chartFilterGroup);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });

        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.primary),
                ContextCompat.getColor(requireContext(), R.color.accent)
        );
    }

    private void refreshData() {
        swipeRefreshLayout.setRefreshing(true);
        loadData();
    }

    private void setupFilters(View view) {
        Chip chipAllTime = view.findViewById(R.id.chipAllTime);
        Chip chipThisYear = view.findViewById(R.id.chipThisYear);
        Chip chipThisMonth = view.findViewById(R.id.chipThisMonth);

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
        showEmpty(false);

        executor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();

            if (farm != null) {
                expenseRepository = new ExpenseRepository(requireContext(), farm.getId());

                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());
                Map<Long, String> blockMap = new HashMap<>();
                for (BlockEntity block : blocks) {
                    blockMap.put(block.getId(), block.getBlockName());
                }

                final Map<Long, String> finalBlockMap = blockMap;
                final FarmEntity finalFarm = farm;
                final List<BlockEntity> finalBlocks = blocks;

                requireActivity().runOnUiThread(() -> {
                    expenseRepository.getAllExpenses().observe(getViewLifecycleOwner(), new Observer<List<ExpenseEntity>>() {
                        @Override
                        public void onChanged(List<ExpenseEntity> expenses) {
                            currentFarm = finalFarm;
                            allBlocks = finalBlocks;
                            blockNameMap = finalBlockMap;
                            allExpenses = expenses;

                            showLoading(false);

                            if (allExpenses == null || allExpenses.isEmpty()) {
                                showEmpty(true);
                            } else {
                                showEmpty(false);
                                refreshCharts();
                            }

                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(requireContext(), "Farm data not found", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void refreshCharts() {
        if (allExpenses == null) {
            allExpenses = new ArrayList<>();
        }

        List<ExpenseEntity> filteredExpenses = filterExpensesByTime(allExpenses);

        if (filteredExpenses.isEmpty()) {
            showEmpty(true);
            return;
        }

        showEmpty(false);
        updateSummaryCards(filteredExpenses);
        updatePieChart(filteredExpenses);
        updateBarChart(filteredExpenses);
        updateLineChart(filteredExpenses);
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

        totalExpensesValue.setText(formatLargeNumber(total) + " XAF");
        costPerHectareValue.setText(formatLargeNumber(costPerHa) + " XAF");
        costPerTreeValue.setText(formatLargeNumber(costPerTree) + " XAF");
    }

    private String formatLargeNumber(double value) {
        if (value >= 1_000_000_000) {
            return String.format(Locale.getDefault(), "%.1fB", value / 1_000_000_000);
        } else if (value >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.0fM", value / 1_000_000);
        } else if (value >= 1_000) {
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
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f XAF", value);
            }
        });

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Configure pie chart style
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(35f);
        pieChart.setEntryLabelTextSize(12f);

        // Set legend text color based on theme
        Legend legend = pieChart.getLegend();
        legend.setTextColor(getThemeTextColor());
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);

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
            monthlyTotals.put(monthYear, monthlyTotals.getOrDefault(monthYear, 0f) + (float) expense.getAmount());
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
            float value = entry.getValue() / 1000f; // Convert to K XAF
            barEntries.add(new BarEntry(i, value));
            labels.add(entry.getKey());
        }

        BarDataSet dataSet = new BarDataSet(barEntries, "Monthly Expenses (k XAF)");
        dataSet.setColors(ContextCompat.getColor(requireContext(), R.color.primary));
        dataSet.setValueTextColor(getThemeTextColor());
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0fK", value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);

        // Configure X-axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45f);
        xAxis.setTextColor(getThemeAxisColor());
        xAxis.setTextSize(11f);

        // Configure Y-axis
        barChart.getAxisLeft().setTextColor(getThemeAxisColor());
        barChart.getAxisLeft().setTextSize(11f);
        barChart.getAxisRight().setEnabled(false);

        // Configure legend
        Legend legend = barChart.getLegend();
        legend.setTextColor(getThemeTextColor());
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setTextSize(12f);

        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void updateLineChart(List<ExpenseEntity> expenses) {
        if (lineChart == null) return;

        if (expenses == null || expenses.isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            return;
        }

        Map<String, Float> monthlyTotals = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());

        for (ExpenseEntity expense : expenses) {
            String monthYear = sdf.format(new Date(expense.getDate()));
            monthlyTotals.put(monthYear, monthlyTotals.getOrDefault(monthYear, 0f) + (float) expense.getAmount());
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

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < sortedMonths.size(); i++) {
            Map.Entry<String, Float> entry = sortedMonths.get(i);
            entries.add(new Entry(i, entry.getValue() / 1000f)); // Convert to K XAF
            labels.add(entry.getKey());
        }

        LineDataSet dataSet = new LineDataSet(entries, "Expense Trend");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.accent));
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary));
        dataSet.setCircleRadius(4f);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(getThemeTextColor());
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.primary));
        dataSet.setFillAlpha(50);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0fK", value);
            }
        });

        LineData data = new LineData(dataSet);

        // Configure X-axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getThemeAxisColor());
        xAxis.setTextSize(11f);

        // Configure Y-axis
        lineChart.getAxisLeft().setTextColor(getThemeAxisColor());
        lineChart.getAxisLeft().setTextSize(11f);
        lineChart.getAxisRight().setEnabled(false);

        // Configure legend
        Legend legend = lineChart.getLegend();
        legend.setTextColor(getThemeTextColor());
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setTextSize(12f);

        lineChart.setData(data);
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.animateXY(1000, 1000);
        lineChart.invalidate();
    }

    private void updateTopExpensesList(List<ExpenseEntity> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            topExpensesContainer.removeAllViews();
            return;
        }

        List<ExpenseEntity> sorted = new ArrayList<>(expenses);
        sorted.sort((e1, e2) -> Double.compare(e2.getAmount(), e1.getAmount()));

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
            tvDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(expense.getDate())));

            topExpensesContainer.addView(itemView);
        }
    }

    /**
     * Get the appropriate text color based on current theme
     */
    private int getThemeTextColor() {
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            return ContextCompat.getColor(requireContext(), R.color.text_primary);
        } else {
            return ContextCompat.getColor(requireContext(), R.color.text_dark);
        }
    }

    /**
     * Get the appropriate axis color based on current theme
     */
    private int getThemeAxisColor() {
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            return ContextCompat.getColor(requireContext(), R.color.text_secondary);
        } else {
            return ContextCompat.getColor(requireContext(), R.color.navy);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.GONE : emptyView.getVisibility());
        }
    }

    private void showEmpty(boolean show) {
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (pieChart != null) {
            pieChart.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (barChart != null) {
            barChart.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (lineChart != null) {
            lineChart.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (topExpensesContainer != null) {
            topExpensesContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (expenseRepository != null) {
            expenseRepository.shutdown();
        }
        executor.shutdown();
    }
}