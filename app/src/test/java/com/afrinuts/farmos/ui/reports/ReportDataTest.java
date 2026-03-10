package com.afrinuts.farmos.ui.reports;

import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class ReportDataTest {

    private FarmEntity farm;
    private List<BlockEntity> blocks;
    private List<ExpenseEntity> expenses;
    private List<RevenueEntity> revenues;
    private Date startDate;
    private Date endDate;

    @Before
    public void setUp() {
        farm = new FarmEntity("Test Farm", "Test Location", 35.0, 35.0, 100, 2024);
        farm.setId(1L);

        blocks = new ArrayList<>();
        BlockEntity block1 = new BlockEntity(1L, "A1", 1.0, null);
        block1.setId(1L);
        block1.setStatus(BlockEntity.BlockStatus.PLANTED);
        block1.setSurvivalRate(95.0);
        blocks.add(block1);

        expenses = new ArrayList<>();
        expenses.add(new ExpenseEntity(1L, 1L, ExpenseEntity.ExpenseCategory.LABOR,
                50000, System.currentTimeMillis() - 86400000, "Test expense"));

        revenues = new ArrayList<>();
        revenues.add(new RevenueEntity(1L, 1L, System.currentTimeMillis(),
                100.0, 1500.0, "Test Buyer", RevenueEntity.QualityGrade.PREMIUM, null));

        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 1);
        startDate = cal.getTime();

        cal.set(2024, Calendar.DECEMBER, 31);
        endDate = cal.getTime();
    }

    @Test
    public void testReportDataCreation() {
        ReportData report = new ReportData(farm, blocks, expenses, revenues,
                new ArrayList<>(), startDate, endDate);

        assertNotNull(report);
        assertEquals(farm, report.getFarm());
        assertEquals(blocks, report.getBlocks());
        assertEquals(expenses, report.getExpenses());
        assertEquals(revenues, report.getRevenues());
    }

    @Test
    public void testRevenueCalculation() {
        ReportData report = new ReportData(farm, blocks, expenses, revenues,
                new ArrayList<>(), startDate, endDate);

        assertEquals(150000.0, report.getTotalRevenue(), 0.01); // 100kg * 1500 XAF
    }

    @Test
    public void testExpenseCalculation() {
        ReportData report = new ReportData(farm, blocks, expenses, revenues,
                new ArrayList<>(), startDate, endDate);

        assertEquals(50000.0, report.getTotalExpenses(), 0.01);
    }

    @Test
    public void testProfitCalculation() {
        ReportData report = new ReportData(farm, blocks, expenses, revenues,
                new ArrayList<>(), startDate, endDate);

        assertEquals(100000.0, report.getNetProfit(), 0.01); // 150k - 50k
        assertEquals(66.67, report.getProfitMargin(), 0.1); // 100k / 150k = 66.67%
    }

    @Test
    public void testBlockStatistics() {
        ReportData report = new ReportData(farm, blocks, expenses, revenues,
                new ArrayList<>(), startDate, endDate);

        assertEquals(1, report.getTotalBlocks());
        assertEquals(1, report.getPlantedBlocks());
        assertEquals(100, report.getTotalTrees()); // 1 block * 100 trees
        assertEquals(95, report.getAliveTrees()); // 95% survival
    }

    @Test
    public void testFormattedDateRange() {
        ReportData report = new ReportData(farm, blocks, expenses, revenues,
                new ArrayList<>(), startDate, endDate);

        String dateRange = report.getFormattedDateRange();
        assertTrue(dateRange.contains("Jan 01, 2024"));
        assertTrue(dateRange.contains("Dec 31, 2024"));
    }

    @Test
    public void testFormattedValues() {
        ReportData report = new ReportData(farm, blocks, expenses, revenues,
                new ArrayList<>(), startDate, endDate);

        assertEquals("150,000 XAF", report.getFormattedTotalRevenue());
        assertEquals("50,000 XAF", report.getFormattedTotalExpenses());
        assertEquals("100,000 XAF", report.getFormattedNetProfit());
        assertEquals("66.7%", report.getFormattedProfitMargin());
    }
}