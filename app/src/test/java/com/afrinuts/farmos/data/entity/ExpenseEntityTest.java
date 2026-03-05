package com.afrinuts.farmos.data.entity;

import com.afrinuts.farmos.data.local.entity.ExpenseEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

public class ExpenseEntityTest {

    private ExpenseEntity expense;
    private long testDate;

    @Before
    public void setUp() {
        testDate = Calendar.getInstance().getTimeInMillis();
        expense = new ExpenseEntity(
                1L, // farmId
                null, // farm-wide
                ExpenseEntity.ExpenseCategory.LAND_CLEARING,
                5250000.0,
                testDate,
                "Land clearing for 35-hectare plantation"
        );
    }

    @Test
    public void testExpenseCreation() {
        assertNotNull(expense);
        assertEquals(1L, expense.getFarmId());
        assertNull(expense.getBlockId());
        assertEquals(ExpenseEntity.ExpenseCategory.LAND_CLEARING, expense.getCategory());
        assertEquals(5250000.0, expense.getAmount(), 0.01);
        assertEquals(testDate, expense.getDate());
        assertEquals("Land clearing for 35-hectare plantation", expense.getDescription());
        assertTrue(expense.isFarmWide());
    }

    @Test
    public void testBlockSpecificExpense() {
        ExpenseEntity blockExpense = new ExpenseEntity(
                1L,
                2L, // blockId
                ExpenseEntity.ExpenseCategory.FERTILIZER,
                250000.0,
                testDate,
                "Fertilizer for Block A1"
        );

        assertFalse(blockExpense.isFarmWide());
        assertEquals(2L, blockExpense.getBlockId().longValue());
    }

    @Test
    public void testCategoryProperties() {
        ExpenseEntity.ExpenseCategory category = ExpenseEntity.ExpenseCategory.LAND_CLEARING;
        assertEquals("Land Clearing", category.getDisplayName());
        assertEquals("🚜", category.getIcon());
        assertEquals("35 hectares plantation", category.getDefaultDescription());
    }

    @Test
    public void testSettersAndGetters() {
        expense.setAmount(6000000.0);
        expense.setDescription("Updated description");
        expense.setCategory(ExpenseEntity.ExpenseCategory.PLOWING);

        assertEquals(6000000.0, expense.getAmount(), 0.01);
        assertEquals("Updated description", expense.getDescription());
        assertEquals(ExpenseEntity.ExpenseCategory.PLOWING, expense.getCategory());
    }
}