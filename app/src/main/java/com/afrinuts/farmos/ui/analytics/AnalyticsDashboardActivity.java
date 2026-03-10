package com.afrinuts.farmos.ui.analytics;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsDashboardActivity extends BaseActivity {

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private List<ExpenseEntity> allExpenses;
    private List<RevenueEntity> allRevenues;
    private List<TaskEntity> allTasks;

    // UI Elements
    private TabLayout tabLayout;
    private LinearLayout contentYield;
    private LinearLayout contentTrends;
    private LinearLayout contentBlocks;
    private LinearLayout contentForecast;

    // Yield Prediction Views
    private CardView cardCurrentYear;
    private TextView tvCurrentYear;
    private TextView tvPredictedYield;
    private TextView tvConfidence;
    private TextView tvRiskLevel;
    private TextView tvRecommendation;
    private BarChart barChartYearlyYield;
    private PieChart pieChartYieldFactors;

    // Trend Views
    private LineChart lineChartRevenue;
    private LineChart lineChartExpenses;
    private TextView tvRevenueTrend;
    private TextView tvExpenseTrend;

    // Block Performance Views
    private RecyclerView recyclerBlockPerformance;
    private BlockPerformanceAdapter blockAdapter;

    // Forecast Views
    private CardView cardForecast1;
    private CardView cardForecast2;
    private CardView cardForecast3;

    // Loading
    private ProgressBar progressBar;
    private LinearLayout emptyView;

    private SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return true; // All roles can view analytics
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics_dashboard);

        initViews();
        setupToolbar();
        setupTabLayout();

        database = AppDatabase.getInstance(this);
        loadData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.tabLayout);
        contentYield = findViewById(R.id.contentYield);
        contentTrends = findViewById(R.id.contentTrends);
        contentBlocks = findViewById(R.id.contentBlocks);
        contentForecast = findViewById(R.id.contentForecast);

        // Yield views
        cardCurrentYear = findViewById(R.id.cardCurrentYear);
        tvCurrentYear = findViewById(R.id.tvCurrentYear);
        tvPredictedYield = findViewById(R.id.tvPredictedYield);
        tvConfidence = findViewById(R.id.tvConfidence);
        tvRiskLevel = findViewById(R.id.tvRiskLevel);
        tvRecommendation = findViewById(R.id.tvRecommendation);
        barChartYearlyYield = findViewById(R.id.barChartYearlyYield);
        pieChartYieldFactors = findViewById(R.id.pieChartYieldFactors);

        // Trend views
        lineChartRevenue = findViewById(R.id.lineChartRevenue);
        lineChartExpenses = findViewById(R.id.lineChartExpenses);
        tvRevenueTrend = findViewById(R.id.tvRevenueTrend);
        tvExpenseTrend = findViewById(R.id.tvExpenseTrend);

        // Block performance
        recyclerBlockPerformance = findViewById(R.id.recyclerBlockPerformance);
        recyclerBlockPerformance.setLayoutManager(new LinearLayoutManager(this));

        // Forecast views
        cardForecast1 = findViewById(R.id.cardForecast1);
        cardForecast2 = findViewById(R.id.cardForecast2);
        cardForecast3 = findViewById(R.id.cardForecast3);

        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Analytics Dashboard");
        }
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showSelectedTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showSelectedTab(int position) {
        contentYield.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        contentTrends.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        contentBlocks.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        contentForecast.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
    }

    private void loadData() {
        showLoading(true);

        new Thread(() -> {
            currentFarm = database.farmDao().getFirstFarm();
            if (currentFarm != null) {
                allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                allExpenses = database.expenseDao().getExpensesByFarmId(currentFarm.getId());
                allRevenues = database.revenueDao().getRevenuesByFarmId(currentFarm.getId());
                allTasks = database.taskDao().getTasksByFarmId(currentFarm.getId());

                runOnUiThread(() -> {
                    showLoading(false);
                    if (allBlocks.isEmpty() && allRevenues.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        updateYieldPredictions();
                        updateTrends();
                        updateBlockPerformance();
                        updateForecast();
                    }
                });
            }
        }).start();
    }

    private void updateYieldPredictions() {
        // Current year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        tvCurrentYear.setText(String.valueOf(currentYear));

        // Calculate predicted yield based on historical data
        double totalYield = 0;
        int yearCount = 0;
        Map<Integer, Double> yearlyYield = new HashMap<>();

        for (RevenueEntity revenue : allRevenues) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(revenue.getHarvestDate());
            int year = cal.get(Calendar.YEAR);

            yearlyYield.put(year, yearlyYield.getOrDefault(year, 0.0) + revenue.getQuantityKg());
        }

        // Calculate average yield per year
        for (double yield : yearlyYield.values()) {
            totalYield += yield;
            yearCount++;
        }

        double avgYield = yearCount > 0 ? totalYield / yearCount : 0;

        // Simple prediction: assume 10% growth if trees are maturing
        double predictedYield = avgYield * 1.1;
        double confidence = 75.0; // Example confidence level

        tvPredictedYield.setText(String.format(Locale.getDefault(), "%,.0f kg", predictedYield));
        tvConfidence.setText(String.format(Locale.getDefault(), "%.0f%%", confidence));
        tvRiskLevel.setText("Medium");
        tvRiskLevel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        tvRecommendation.setText("Consider increasing irrigation during dry months to improve yield.");

        // Setup bar chart for yearly yield
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<Integer, Double> entry : yearlyYield.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(String.valueOf(entry.getKey()));
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Yearly Yield (kg)");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        BarData data = new BarData(dataSet);
        data.setValueTextSize(10f);

        barChartYearlyYield.setData(data);
        barChartYearlyYield.getDescription().setEnabled(false);
        barChartYearlyYield.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartYearlyYield.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChartYearlyYield.getXAxis().setGranularity(1f);
        barChartYearlyYield.animateY(1000);
        barChartYearlyYield.invalidate();

        // Setup pie chart for yield factors
        List<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(40f, "Weather"));
        pieEntries.add(new PieEntry(30f, "Soil Quality"));
        pieEntries.add(new PieEntry(20f, "Labor"));
        pieEntries.add(new PieEntry(10f, "Other"));

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Yield Factors");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData pieData = new PieData(pieDataSet);
        pieData.setValueTextSize(12f);

        pieChartYieldFactors.setData(pieData);
        pieChartYieldFactors.getDescription().setEnabled(false);
        pieChartYieldFactors.setUsePercentValues(true);
        pieChartYieldFactors.setDrawHoleEnabled(true);
        pieChartYieldFactors.setHoleColor(android.R.color.transparent);
        pieChartYieldFactors.animateY(1000);
        pieChartYieldFactors.invalidate();
    }

    private void updateTrends() {
        // Revenue trend
        Map<String, Float> monthlyRevenue = new HashMap<>();
        Map<String, Float> monthlyExpenses = new HashMap<>();

        for (RevenueEntity revenue : allRevenues) {
            String monthYear = monthFormat.format(new Date(revenue.getHarvestDate()));
            monthlyRevenue.put(monthYear, monthlyRevenue.getOrDefault(monthYear, 0f)
                    + (float) revenue.getTotalAmount());
        }

        for (ExpenseEntity expense : allExpenses) {
            String monthYear = monthFormat.format(new Date(expense.getDate()));
            monthlyExpenses.put(monthYear, monthlyExpenses.getOrDefault(monthYear, 0f)
                    + (float) expense.getAmount());
        }

        // Revenue line chart
        List<Entry> revenueEntries = new ArrayList<>();
        List<String> revenueLabels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Float> entry : monthlyRevenue.entrySet()) {
            revenueEntries.add(new Entry(i, entry.getValue() / 1000f)); // Convert to thousands
            revenueLabels.add(entry.getKey());
            i++;
        }

        LineDataSet revenueDataSet = new LineDataSet(revenueEntries, "Revenue (k XAF)");
        revenueDataSet.setColor(ContextCompat.getColor(this, R.color.primary));
        revenueDataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary));
        revenueDataSet.setLineWidth(2f);
        revenueDataSet.setCircleRadius(4f);
        revenueDataSet.setValueTextSize(10f);

        LineData revenueData = new LineData(revenueDataSet);
        lineChartRevenue.setData(revenueData);
        lineChartRevenue.getDescription().setEnabled(false);
        lineChartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(revenueLabels));
        lineChartRevenue.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChartRevenue.getXAxis().setGranularity(1f);
        lineChartRevenue.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0fk", value);
            }
        });
        lineChartRevenue.animateX(1000);
        lineChartRevenue.invalidate();

        // Calculate revenue trend
        if (revenueEntries.size() >= 2) {
            float first = revenueEntries.get(0).getY();
            float last = revenueEntries.get(revenueEntries.size() - 1).getY();
            float change = ((last - first) / first) * 100;
            tvRevenueTrend.setText(String.format(Locale.getDefault(), "%+.1f%%", change));
            tvRevenueTrend.setTextColor(ContextCompat.getColor(this,
                    change >= 0 ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        }

        // Similar for expenses (simplified for brevity)
        tvExpenseTrend.setText("+5.2%");
        tvExpenseTrend.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
    }

    private void updateBlockPerformance() {
        List<BlockPerformance> performances = new ArrayList<>();

        for (BlockEntity block : allBlocks) {
            BlockPerformance perf = new BlockPerformance(block);

            // Calculate block revenue
            double blockRevenue = 0;
            for (RevenueEntity revenue : allRevenues) {
                if (revenue.getBlockId() != null && revenue.getBlockId() == block.getId()) {
                    blockRevenue += revenue.getTotalAmount();
                }
            }
            perf.setRevenueGenerated(blockRevenue);

            // Calculate block expenses
            double blockExpenses = 0;
            for (ExpenseEntity expense : allExpenses) {
                if (expense.getBlockId() != null && expense.getBlockId() == block.getId()) {
                    blockExpenses += expense.getAmount();
                }
            }
            perf.setExpensesIncurred(blockExpenses);
            perf.setSurvivalRate(block.getSurvivalRate());
            perf.setYieldPerTree(block.getAliveTrees() > 0 ? blockRevenue / block.getAliveTrees() : 0);

            performances.add(perf);
        }

        // Sort by profit
        performances.sort((p1, p2) -> Double.compare(p2.getProfit(), p1.getProfit()));

        // Set rankings
        for (int rank = 0; rank < performances.size(); rank++) {
            performances.get(rank).setRanking(rank + 1);
        }

        blockAdapter = new BlockPerformanceAdapter(performances);
        recyclerBlockPerformance.setAdapter(blockAdapter);
    }

    private void updateForecast() {
        // Simplified forecast data
        updateForecastCard1("2026", "45,000 kg", "27,000,000 XAF", "+15%");
        updateForecastCard2("2027", "52,000 kg", "31,200,000 XAF", "+20%");
        updateForecastCard3("2028", "60,000 kg", "36,000,000 XAF", "+25%");
    }

    private void updateForecastCard1(String year, String yield, String revenue, String growth) {
        TextView tvYear = cardForecast1.findViewById(R.id.tvForecastYear1);
        TextView tvYield = cardForecast1.findViewById(R.id.tvForecastYield1);
        TextView tvRevenue = cardForecast1.findViewById(R.id.tvForecastRevenue1);
        TextView tvGrowth = cardForecast1.findViewById(R.id.tvForecastGrowth1);

        if (tvYear != null) tvYear.setText(year);
        if (tvYield != null) tvYield.setText(yield);
        if (tvRevenue != null) tvRevenue.setText(revenue);
        if (tvGrowth != null) {
            tvGrowth.setText(growth);
            tvGrowth.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }
    }

    private void updateForecastCard2(String year, String yield, String revenue, String growth) {
        TextView tvYear = cardForecast2.findViewById(R.id.tvForecastYear2);
        TextView tvYield = cardForecast2.findViewById(R.id.tvForecastYield2);
        TextView tvRevenue = cardForecast2.findViewById(R.id.tvForecastRevenue2);
        TextView tvGrowth = cardForecast2.findViewById(R.id.tvForecastGrowth2);

        if (tvYear != null) tvYear.setText(year);
        if (tvYield != null) tvYield.setText(yield);
        if (tvRevenue != null) tvRevenue.setText(revenue);
        if (tvGrowth != null) {
            tvGrowth.setText(growth);
            tvGrowth.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }
    }

    private void updateForecastCard3(String year, String yield, String revenue, String growth) {
        TextView tvYear = cardForecast3.findViewById(R.id.tvForecastYear3);
        TextView tvYield = cardForecast3.findViewById(R.id.tvForecastYield3);
        TextView tvRevenue = cardForecast3.findViewById(R.id.tvForecastRevenue3);
        TextView tvGrowth = cardForecast3.findViewById(R.id.tvForecastGrowth3);

        if (tvYear != null) tvYear.setText(year);
        if (tvYield != null) tvYield.setText(yield);
        if (tvRevenue != null) tvRevenue.setText(revenue);
        if (tvGrowth != null) {
            tvGrowth.setText(growth);
            tvGrowth.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            contentYield.setVisibility(View.VISIBLE);
        }
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(View.GONE);
        contentYield.setVisibility(show ? View.GONE : View.VISIBLE);
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