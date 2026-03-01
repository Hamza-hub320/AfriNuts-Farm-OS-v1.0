package com.afrinuts.farmos.ui.profit;

import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfitAnalytics {
    private double totalRevenue;
    private double totalExpenses;
    private double netProfit;
    private double profitMargin;
    private double revenuePerHectare;
    private double expensePerHectare;
    private double profitPerHectare;
    private double revenuePerTree;
    private double expensePerTree;
    private double profitPerTree;

    private Map<String, Double> monthlyProfit = new HashMap<>();
    private Map<String, Double> profitByBlock = new HashMap<>();
    private List<ProfitTransaction> recentTransactions = new ArrayList<>();

    public ProfitAnalytics(List<RevenueEntity> revenues, List<ExpenseEntity> expenses,
                           double totalHectares, double totalTrees) {
        calculateTotals(revenues, expenses);
        calculatePerUnitMetrics(totalHectares, totalTrees);
        calculateMonthlyProfit(revenues, expenses);
        calculateProfitByBlock(revenues, expenses);
        buildRecentTransactions(revenues, expenses);
    }

    private void calculateTotals(List<RevenueEntity> revenues, List<ExpenseEntity> expenses) {
        totalRevenue = 0;
        for (RevenueEntity revenue : revenues) {
            totalRevenue += revenue.getTotalAmount();
        }

        totalExpenses = 0;
        for (ExpenseEntity expense : expenses) {
            totalExpenses += expense.getAmount();
        }

        netProfit = totalRevenue - totalExpenses;
        profitMargin = totalRevenue > 0 ? (netProfit / totalRevenue) * 100 : 0;
    }

    private void calculatePerUnitMetrics(double hectares, double trees) {
        revenuePerHectare = hectares > 0 ? totalRevenue / hectares : 0;
        expensePerHectare = hectares > 0 ? totalExpenses / hectares : 0;
        profitPerHectare = hectares > 0 ? netProfit / hectares : 0;

        revenuePerTree = trees > 0 ? totalRevenue / trees : 0;
        expensePerTree = trees > 0 ? totalExpenses / trees : 0;
        profitPerTree = trees > 0 ? netProfit / trees : 0;
    }

    private void calculateMonthlyProfit(List<RevenueEntity> revenues, List<ExpenseEntity> expenses) {
        Map<String, Double> monthlyRevenue = new HashMap<>();
        Map<String, Double> monthlyExpenses = new HashMap<>();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());

        for (RevenueEntity revenue : revenues) {
            String monthYear = sdf.format(new Date(revenue.getHarvestDate()));
            monthlyRevenue.put(monthYear, monthlyRevenue.getOrDefault(monthYear, 0.0)
                    + revenue.getTotalAmount());
        }

        for (ExpenseEntity expense : expenses) {
            String monthYear = sdf.format(new Date(expense.getDate()));
            monthlyExpenses.put(monthYear, monthlyExpenses.getOrDefault(monthYear, 0.0)
                    + expense.getAmount());
        }

        // Calculate profit for each month
        for (String month : monthlyRevenue.keySet()) {
            double revenue = monthlyRevenue.getOrDefault(month, 0.0);
            double expense = monthlyExpenses.getOrDefault(month, 0.0);
            monthlyProfit.put(month, revenue - expense);
        }

        // Add months with only expenses
        for (String month : monthlyExpenses.keySet()) {
            if (!monthlyProfit.containsKey(month)) {
                monthlyProfit.put(month, -monthlyExpenses.get(month));
            }
        }
    }

    private void calculateProfitByBlock(List<RevenueEntity> revenues, List<ExpenseEntity> expenses) {
        Map<Long, Double> blockRevenue = new HashMap<>();
        Map<Long, Double> blockExpenses = new HashMap<>();

        for (RevenueEntity revenue : revenues) {
            if (revenue.getBlockId() != null) {
                Long blockId = revenue.getBlockId();
                blockRevenue.put(blockId, blockRevenue.getOrDefault(blockId, 0.0)
                        + revenue.getTotalAmount());
            }
        }

        for (ExpenseEntity expense : expenses) {
            if (expense.getBlockId() != null) {
                Long blockId = expense.getBlockId();
                blockExpenses.put(blockId, blockExpenses.getOrDefault(blockId, 0.0)
                        + expense.getAmount());
            }
        }

        for (Long blockId : blockRevenue.keySet()) {
            double revenue = blockRevenue.getOrDefault(blockId, 0.0);
            double expense = blockExpenses.getOrDefault(blockId, 0.0);
            profitByBlock.put(String.valueOf(blockId), revenue - expense);
        }
    }

    private void buildRecentTransactions(List<RevenueEntity> revenues, List<ExpenseEntity> expenses) {
        List<ProfitTransaction> allTransactions = new ArrayList<>();

        for (RevenueEntity revenue : revenues) {
            allTransactions.add(new ProfitTransaction(
                    revenue.getId(),
                    true,
                    revenue.getHarvestDate(),
                    revenue.getTotalAmount(),
                    revenue.getQuality().getIcon() + " " + revenue.getQuality().getDisplayName(),
                    revenue.getBlockId()
            ));
        }

        for (ExpenseEntity expense : expenses) {
            allTransactions.add(new ProfitTransaction(
                    expense.getId(),
                    false,
                    expense.getDate(),
                    -expense.getAmount(),
                    expense.getCategory().getIcon() + " " + expense.getCategory().getDisplayName(),
                    expense.getBlockId()
            ));
        }

        // Sort by date (most recent first)
        allTransactions.sort((t1, t2) -> Long.compare(t2.getDate(), t1.getDate()));

        // Take top 10
        int limit = Math.min(10, allTransactions.size());
        recentTransactions = allTransactions.subList(0, limit);
    }

    // Getters
    public double getTotalRevenue() { return totalRevenue; }
    public double getTotalExpenses() { return totalExpenses; }
    public double getNetProfit() { return netProfit; }
    public double getProfitMargin() { return profitMargin; }
    public double getRevenuePerHectare() { return revenuePerHectare; }
    public double getExpensePerHectare() { return expensePerHectare; }
    public double getProfitPerHectare() { return profitPerHectare; }
    public double getRevenuePerTree() { return revenuePerTree; }
    public double getExpensePerTree() { return expensePerTree; }
    public double getProfitPerTree() { return profitPerTree; }
    public Map<String, Double> getMonthlyProfit() { return monthlyProfit; }
    public Map<String, Double> getProfitByBlock() { return profitByBlock; }
    public List<ProfitTransaction> getRecentTransactions() { return recentTransactions; }

    public static class ProfitTransaction {
        private long id;
        private boolean isRevenue;
        private long date;
        private double amount;
        private String description;
        private Long blockId;

        public ProfitTransaction(long id, boolean isRevenue, long date,
                                 double amount, String description, Long blockId) {
            this.id = id;
            this.isRevenue = isRevenue;
            this.date = date;
            this.amount = amount;
            this.description = description;
            this.blockId = blockId;
        }

        public long getId() { return id; }
        public boolean isRevenue() { return isRevenue; }
        public long getDate() { return date; }
        public double getAmount() { return amount; }
        public String getDescription() { return description; }
        public Long getBlockId() { return blockId; }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(date));
        }

        public String getFormattedAmount() {
            return String.format(Locale.getDefault(), "%,.0f XAF", Math.abs(amount));
        }

        public int getAmountColor() {
            return isRevenue ? android.R.color.holo_green_dark : android.R.color.holo_red_dark;
        }

        public String getSign() {
            return isRevenue ? "+" : "-";
        }
    }
}