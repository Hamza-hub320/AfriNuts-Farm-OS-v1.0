package com.afrinuts.farmos.ui.profit;

import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ProfitAnalyticsTest {

    private List<RevenueEntity> revenues;
    private List<ExpenseEntity> expenses;
    private ProfitAnalytics analytics;

    @Before
    public void setUp() {
        revenues = new ArrayList<>();
        expenses = new ArrayList<>();

        // Add test revenue
        revenues.add(createRevenue(1L, 100.0, 1500.0)); // 150,000 XAF
        revenues.add(createRevenue(1L, 200.0, 1600.0)); // 320,000 XAF
        revenues.add(createRevenue(2L, 150.0, 1550.0)); // 232,500 XAF

        // Add test expenses
        expenses.add(createExpense(1L, 50000.0)); // 50,000 XAF
        expenses.add(createExpense(1L, 75000.0)); // 75,000 XAF
        expenses.add(createExpense(null, 100000.0)); // 100,000 XAF farm-wide

        analytics = new ProfitAnalytics(revenues, expenses, 35.0, 3500.0);
    }

    private RevenueEntity createRevenue(Long blockId, double quantity, double price) {
        return new RevenueEntity(
                1L, blockId, System.currentTimeMillis(),
                quantity, price, "Test Buyer",
                RevenueEntity.QualityGrade.PREMIUM, null
        );
    }

    private ExpenseEntity createExpense(Long blockId, double amount) {
        return new ExpenseEntity(
                1L, blockId,
                ExpenseEntity.ExpenseCategory.LABOR,
                amount, System.currentTimeMillis(),
                "Test expense"
        );
    }

    @Test
    public void testTotalCalculations() {
        double expectedRevenue = 150000 + 320000 + 232500; // 702,500 XAF
        double expectedExpenses = 50000 + 75000 + 100000; // 225,000 XAF
        double expectedProfit = expectedRevenue - expectedExpenses; // 477,500 XAF

        assertEquals(expectedRevenue, analytics.getTotalRevenue(), 0.01);
        assertEquals(expectedExpenses, analytics.getTotalExpenses(), 0.01);
        assertEquals(expectedProfit, analytics.getNetProfit(), 0.01);

        double expectedMargin = (expectedProfit / expectedRevenue) * 100;
        assertEquals(expectedMargin, analytics.getProfitMargin(), 0.01);
    }

    @Test
    public void testPerUnitMetrics() {
        double expectedProfitPerHa = analytics.getNetProfit() / 35.0;
        double expectedProfitPerTree = analytics.getNetProfit() / 3500.0;

        assertEquals(expectedProfitPerHa, analytics.getProfitPerHectare(), 0.01);
        assertEquals(expectedProfitPerTree, analytics.getProfitPerTree(), 0.01);
    }

    @Test
    public void testRecentTransactions() {
        List<ProfitAnalytics.ProfitTransaction> transactions = analytics.getRecentTransactions();

        // Should have 6 transactions (3 revenue + 3 expense)
        assertEquals(6, transactions.size());

        // First transaction should be most recent (we'll trust sorting works)
        ProfitAnalytics.ProfitTransaction first = transactions.get(0);
        assertNotNull(first.getFormattedDate());
        assertNotNull(first.getFormattedAmount());
    }
}