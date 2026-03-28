package com.afrinuts.farmos.ui.analytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
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
import com.google.android.material.tabs.TabLayout;

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

public class AnalyticsDashboardFragment extends Fragment {

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks = new ArrayList<>();
    private List<ExpenseEntity> allExpenses = new ArrayList<>();
    private List<RevenueEntity> allRevenues = new ArrayList<>();
    private List<TaskEntity> allTasks = new ArrayList<>();
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TabLayout tabLayout;
    private LinearLayout contentYield;
    private LinearLayout contentTrends;
    private LinearLayout contentBlocks;
    private LinearLayout contentForecast;
    private ProgressBar progressBar;
    private LinearLayout emptyView;

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

    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics_dashboard, container, false);

        database = AppDatabase.getInstance(requireContext());
        initViews(view);
        setupTabLayout();
        loadData();

        return view;
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        contentYield = view.findViewById(R.id.contentYield);
        contentTrends = view.findViewById(R.id.contentTrends);
        contentBlocks = view.findViewById(R.id.contentBlocks);
        contentForecast = view.findViewById(R.id.contentForecast);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);

        // Yield views
        cardCurrentYear = view.findViewById(R.id.cardCurrentYear);
        tvCurrentYear = view.findViewById(R.id.tvCurrentYear);
        tvPredictedYield = view.findViewById(R.id.tvPredictedYield);
        tvConfidence = view.findViewById(R.id.tvConfidence);
        tvRiskLevel = view.findViewById(R.id.tvRiskLevel);
        tvRecommendation = view.findViewById(R.id.tvRecommendation);
        barChartYearlyYield = view.findViewById(R.id.barChartYearlyYield);
        pieChartYieldFactors = view.findViewById(R.id.pieChartYieldFactors);

        // Trend views
        lineChartRevenue = view.findViewById(R.id.lineChartRevenue);
        lineChartExpenses = view.findViewById(R.id.lineChartExpenses);
        tvRevenueTrend = view.findViewById(R.id.tvRevenueTrend);
        tvExpenseTrend = view.findViewById(R.id.tvExpenseTrend);

        // Block performance
        recyclerBlockPerformance = view.findViewById(R.id.recyclerBlockPerformance);
        recyclerBlockPerformance.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Forecast views
        cardForecast1 = view.findViewById(R.id.cardForecast1);
        cardForecast2 = view.findViewById(R.id.cardForecast2);
        cardForecast3 = view.findViewById(R.id.cardForecast3);
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                showTabContent(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    private void showTabContent(int position) {
        // Hide all content first
        contentYield.setVisibility(View.GONE);
        contentTrends.setVisibility(View.GONE);
        contentBlocks.setVisibility(View.GONE);
        contentForecast.setVisibility(View.GONE);

        // Show selected content
        switch (position) {
            case 0:
                contentYield.setVisibility(View.VISIBLE);
                break;
            case 1:
                contentTrends.setVisibility(View.VISIBLE);
                break;
            case 2:
                contentBlocks.setVisibility(View.VISIBLE);
                break;
            case 3:
                contentForecast.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void loadData() {
        showLoading(true);

        databaseExecutor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();

            if (farm != null) {
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());
                List<ExpenseEntity> expenses = database.expenseDao().getExpensesByFarmId(farm.getId());
                List<RevenueEntity> revenues = database.revenueDao().getRevenuesByFarmId(farm.getId());
                List<TaskEntity> tasks = database.taskDao().getTasksByFarmId(farm.getId());

                requireActivity().runOnUiThread(() -> {
                    currentFarm = farm;
                    allBlocks.clear();
                    allBlocks.addAll(blocks);
                    allExpenses.clear();
                    allExpenses.addAll(expenses);
                    allRevenues.clear();
                    allRevenues.addAll(revenues);
                    allTasks.clear();
                    allTasks.addAll(tasks);

                    showLoading(false);

                    if (allBlocks.isEmpty() && allRevenues.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        updateYieldPredictions();
                        updateTrends();
                        updateBlockPerformance();
                        updateForecast();
                        // Show Yield tab by default
                        showTabContent(0);
                    }
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true);
                });
            }
        });
    }

    private void updateYieldPredictions() {
        if (allRevenues.isEmpty()) {
            tvPredictedYield.setText("No data");
            tvConfidence.setText("0%");
            return;
        }

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
        double confidence = 75.0;

        tvPredictedYield.setText(String.format(Locale.getDefault(), "%,.0f kg", predictedYield));
        tvConfidence.setText(String.format(Locale.getDefault(), "%.0f%%", confidence));
        tvRiskLevel.setText("Medium");
        tvRiskLevel.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark));
        tvRecommendation.setText("Consider increasing irrigation during dry months to improve yield.");

        // Setup bar chart for yearly yield
        if (!yearlyYield.isEmpty()) {
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
        }

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
        if (!monthlyRevenue.isEmpty()) {
            List<Entry> revenueEntries = new ArrayList<>();
            List<String> revenueLabels = new ArrayList<>();
            int i = 0;
            for (Map.Entry<String, Float> entry : monthlyRevenue.entrySet()) {
                revenueEntries.add(new Entry(i, entry.getValue() / 1000f));
                revenueLabels.add(entry.getKey());
                i++;
            }

            LineDataSet revenueDataSet = new LineDataSet(revenueEntries, "Revenue (k XAF)");
            revenueDataSet.setColor(requireContext().getColor(R.color.primary));
            revenueDataSet.setCircleColor(requireContext().getColor(R.color.primary));
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
                float change = first > 0 ? ((last - first) / first) * 100 : 0;
                tvRevenueTrend.setText(String.format(Locale.getDefault(), "%+.1f%%", change));
                tvRevenueTrend.setTextColor(requireContext().getColor(
                        change >= 0 ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }
        }

        // Expense trend
        tvExpenseTrend.setText("+5.2%");
        tvExpenseTrend.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
    }

    private void updateBlockPerformance() {
        if (allBlocks.isEmpty()) return;

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
        updateForecastCard(cardForecast1, "2026", "45,000 kg", "27,000,000 XAF", "+15%");
        updateForecastCard(cardForecast2, "2027", "52,000 kg", "31,200,000 XAF", "+20%");
        updateForecastCard(cardForecast3, "2028", "60,000 kg", "36,000,000 XAF", "+25%");
    }

    private void updateForecastCard(CardView card, String year, String yield, String revenue, String growth) {
        if (card == null) return;

        // Find TextViews by ID - different IDs for each card
        TextView tvYear = card.findViewById(getYearTextViewId(card));
        TextView tvYield = card.findViewById(getYieldTextViewId(card));
        TextView tvRevenue = card.findViewById(getRevenueTextViewId(card));
        TextView tvGrowth = card.findViewById(getGrowthTextViewId(card));

        if (tvYear != null) tvYear.setText(year);
        if (tvYield != null) tvYield.setText(yield);
        if (tvRevenue != null) tvRevenue.setText(revenue);
        if (tvGrowth != null) {
            tvGrowth.setText(growth);
            tvGrowth.setTextColor(requireContext().getColor(android.R.color.holo_green_dark));
        }
    }

    private int getYearTextViewId(CardView card) {
        if (card.getId() == R.id.cardForecast1) return R.id.tvForecastYear1;
        if (card.getId() == R.id.cardForecast2) return R.id.tvForecastYear2;
        if (card.getId() == R.id.cardForecast3) return R.id.tvForecastYear3;
        return R.id.tvForecastYear1;
    }

    private int getYieldTextViewId(CardView card) {
        if (card.getId() == R.id.cardForecast1) return R.id.tvForecastYield1;
        if (card.getId() == R.id.cardForecast2) return R.id.tvForecastYield2;
        if (card.getId() == R.id.cardForecast3) return R.id.tvForecastYield3;
        return R.id.tvForecastYield1;
    }

    private int getRevenueTextViewId(CardView card) {
        if (card.getId() == R.id.cardForecast1) return R.id.tvForecastRevenue1;
        if (card.getId() == R.id.cardForecast2) return R.id.tvForecastRevenue2;
        if (card.getId() == R.id.cardForecast3) return R.id.tvForecastRevenue3;
        return R.id.tvForecastRevenue1;
    }

    private int getGrowthTextViewId(CardView card) {
        if (card.getId() == R.id.cardForecast1) return R.id.tvForecastGrowth1;
        if (card.getId() == R.id.cardForecast2) return R.id.tvForecastGrowth2;
        if (card.getId() == R.id.cardForecast3) return R.id.tvForecastGrowth3;
        return R.id.tvForecastGrowth1;
    }

    private void showLoading(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        });
    }

    private void showEmpty(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (emptyView != null) emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}