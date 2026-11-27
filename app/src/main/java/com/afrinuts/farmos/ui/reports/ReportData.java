package com.afrinuts.farmos.ui.reports;

import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportData {
    private FarmEntity farm;
    private List<BlockEntity> blocks;
    private List<ExpenseEntity> expenses;
    private List<RevenueEntity> revenues;
    private List<TaskEntity> tasks;
    private List<QualityBatchEntity> qualityBatches = java.util.Collections.emptyList();
    private List<BatchTraceabilityEntity> traceabilities = java.util.Collections.emptyList();
    private List<MasterBatchTraceabilityEntity> masters = java.util.Collections.emptyList();

    private Date startDate;
    private Date endDate;

    // Summary statistics
    private double totalRevenue;
    private double totalExpenses;
    private double netProfit;
    private double profitMargin;
    private int totalBlocks;
    private int plantedBlocks;
    private int totalTrees;
    private int aliveTrees;

    public ReportData(FarmEntity farm, List<BlockEntity> blocks,
                      List<ExpenseEntity> expenses, List<RevenueEntity> revenues,
                      List<TaskEntity> tasks, Date startDate, Date endDate) {
        this.farm = farm;
        this.blocks = blocks;
        this.expenses = expenses;
        this.revenues = revenues;
        this.tasks = tasks;
        this.startDate = startDate;
        this.endDate = endDate;

        calculateStatistics();
    }

    public ReportData(FarmEntity farm, List<BlockEntity> blocks,
                      List<ExpenseEntity> expenses, List<RevenueEntity> revenues,
                      List<TaskEntity> tasks, List<QualityBatchEntity> qualityBatches,
                      List<BatchTraceabilityEntity> traceabilities,
                      List<MasterBatchTraceabilityEntity> masters, Date startDate, Date endDate) {
        this(farm, blocks, expenses, revenues, tasks, startDate, endDate);
        this.qualityBatches = qualityBatches == null ? java.util.Collections.emptyList() : qualityBatches;
        this.traceabilities = traceabilities == null ? java.util.Collections.emptyList() : traceabilities;
        this.masters = masters == null ? java.util.Collections.emptyList() : masters;
    }

    private void calculateStatistics() {
        // Revenue
        totalRevenue = 0;
        for (RevenueEntity revenue : revenues) {
            totalRevenue += revenue.getTotalAmount();
        }

        // Expenses
        totalExpenses = 0;
        for (ExpenseEntity expense : expenses) {
            totalExpenses += expense.getAmount();
        }

        // Profit
        netProfit = totalRevenue - totalExpenses;
        profitMargin = totalRevenue > 0 ? (netProfit / totalRevenue) * 100 : 0;

        // Blocks
        totalBlocks = blocks.size();
        plantedBlocks = 0;
        for (BlockEntity block : blocks) {
            if (block.isPlanted()) {
                plantedBlocks++;
            }
        }

        // Trees
        totalTrees = totalBlocks * 100;
        aliveTrees = 0;
        for (BlockEntity block : blocks) {
            aliveTrees += block.getAliveTrees();
        }
    }

    // Getters
    public FarmEntity getFarm() { return farm; }
    public List<BlockEntity> getBlocks() { return blocks; }
    public List<ExpenseEntity> getExpenses() { return expenses; }
    public List<RevenueEntity> getRevenues() { return revenues; }
    public List<TaskEntity> getTasks() { return tasks; }
    public List<QualityBatchEntity> getQualityBatches() { return qualityBatches; }
    public List<BatchTraceabilityEntity> getTraceabilities() { return traceabilities; }
    public List<MasterBatchTraceabilityEntity> getMasters() { return masters; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }

    public double getTotalRevenue() { return totalRevenue; }
    public double getTotalExpenses() { return totalExpenses; }
    public double getNetProfit() { return netProfit; }
    public double getProfitMargin() { return profitMargin; }
    public int getTotalBlocks() { return totalBlocks; }
    public int getPlantedBlocks() { return plantedBlocks; }
    public int getTotalTrees() { return totalTrees; }
    public int getAliveTrees() { return aliveTrees; }

    public String getFormattedDateRange() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(startDate) + " - " + sdf.format(endDate);
    }

    public String getFormattedTotalRevenue() {
        return String.format(Locale.getDefault(), "%,.0f XAF", totalRevenue);
    }

    public String getFormattedTotalExpenses() {
        return String.format(Locale.getDefault(), "%,.0f XAF", totalExpenses);
    }

    public String getFormattedNetProfit() {
        return String.format(Locale.getDefault(), "%,.0f XAF", netProfit);
    }

    public String getFormattedProfitMargin() {
        return String.format(Locale.getDefault(), "%.1f%%", profitMargin);
    }
}
