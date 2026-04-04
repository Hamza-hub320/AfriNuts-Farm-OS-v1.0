package com.afrinuts.farmos.ui.analytics;

import com.afrinuts.farmos.data.local.entity.BlockEntity;

import java.util.Locale;

public class BlockPerformance {
    private BlockEntity block;
    private double survivalRate;
    private double yieldPerTree;
    private double revenueGenerated;
    private double expensesIncurred;
    private double profit;
    private double profitMargin;
    private int ranking; // 1 = best performing

    public BlockPerformance(BlockEntity block) {
        this.block = block;
    }

    public BlockEntity getBlock() { return block; }

    public double getSurvivalRate() { return survivalRate; }
    public void setSurvivalRate(double survivalRate) { this.survivalRate = survivalRate; }

    public double getYieldPerTree() { return yieldPerTree; }
    public void setYieldPerTree(double yieldPerTree) { this.yieldPerTree = yieldPerTree; }

    public double getRevenueGenerated() { return revenueGenerated; }
    public void setRevenueGenerated(double revenueGenerated) {
        this.revenueGenerated = revenueGenerated;
        calculateProfit();
    }

    public double getExpensesIncurred() { return expensesIncurred; }
    public void setExpensesIncurred(double expensesIncurred) {
        this.expensesIncurred = expensesIncurred;
        calculateProfit();
    }

    public double getProfit() { return profit; }
    public double getProfitMargin() { return profitMargin; }
    public int getRanking() { return ranking; }
    public void setRanking(int ranking) { this.ranking = ranking; }

    private void calculateProfit() {
        profit = revenueGenerated - expensesIncurred;
        profitMargin = revenueGenerated > 0 ? (profit / revenueGenerated) * 100 : 0;
    }

    public String getFormattedProfit() {
        // Shorten large numbers (e.g., 125,000 → 125k)
        if (Math.abs(profit) >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.1fM XAF", profit / 1_000_000);
        } else if (Math.abs(profit) >= 1_000) {
            return String.format(Locale.getDefault(), "%.0fk XAF", profit / 1_000);
        }
        return String.format(Locale.getDefault(), "%,.0f XAF", profit);
    }

    public String getFormattedProfitMargin() {
        return String.format(Locale.getDefault(), "%.0f%%", profitMargin);
    }

    public String getFormattedSurvivalRate() {
        return String.format(Locale.getDefault(), "%.0f%%", survivalRate);
    }
}