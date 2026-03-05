package com.afrinuts.farmos.data.entity;

import com.afrinuts.farmos.data.local.entity.RevenueEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

public class RevenueEntityTest {

    private RevenueEntity revenue;
    private long testDate;

    @Before
    public void setUp() {
        testDate = Calendar.getInstance().getTimeInMillis();
        revenue = new RevenueEntity(
                1L, // farmId
                2L, // blockId
                testDate,
                350.5, // kg
                1500.0, // price per kg
                "Olam",
                RevenueEntity.QualityGrade.PREMIUM,
                "First harvest of the season"
        );
    }

    @Test
    public void testRevenueCreation() {
        assertNotNull(revenue);
        assertEquals(1L, revenue.getFarmId());
        assertEquals(2L, revenue.getBlockId().longValue());
        assertEquals(testDate, revenue.getHarvestDate());
        assertEquals(350.5, revenue.getQuantityKg(), 0.01);
        assertEquals(1500.0, revenue.getPricePerKg(), 0.01);
        assertEquals(525750.0, revenue.getTotalAmount(), 0.01); // 350.5 * 1500
        assertEquals("Olam", revenue.getBuyer());
        assertEquals(RevenueEntity.QualityGrade.PREMIUM, revenue.getQuality());
    }

    @Test
    public void testTotalAmountCalculation() {
        revenue.setQuantityKg(500.0);
        revenue.setPricePerKg(2000.0);
        assertEquals(1000000.0, revenue.getTotalAmount(), 0.01);
    }

    @Test
    public void testQualityGradeProperties() {
        RevenueEntity.QualityGrade grade = RevenueEntity.QualityGrade.PREMIUM;
        assertEquals("Premium", grade.getDisplayName());
        assertEquals("⭐", grade.getIcon());
        assertEquals("Best quality, large nuts", grade.getDescription());
    }

    @Test
    public void testFormattedMethods() {
        assertEquals("525,750 XAF", revenue.getFormattedTotal());
        assertEquals("350.5 kg", revenue.getFormattedQuantity());
        assertEquals("1,500 XAF/kg", revenue.getFormattedPrice());
    }

    @Test
    public void testProcessingCenterRevenue() {
        RevenueEntity processingRevenue = new RevenueEntity(
                1L,
                null, // no blockId = processing center
                testDate,
                1000.0,
                800.0,
                "Local Processor",
                RevenueEntity.QualityGrade.PROCESSING,
                null
        );

        assertTrue(processingRevenue.isFarmWide());
        assertEquals(RevenueEntity.QualityGrade.PROCESSING, processingRevenue.getQuality());
    }
}