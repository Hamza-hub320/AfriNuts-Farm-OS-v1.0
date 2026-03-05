package com.afrinuts.farmos.data.entity;

import com.afrinuts.farmos.data.local.entity.BlockEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

public class BlockEntityTest {

    private BlockEntity block;
    private long testDate;

    @Before
    public void setUp() {
        testDate = Calendar.getInstance().getTimeInMillis();
        block = new BlockEntity(
                1L, // farmId
                "A1",
                1.0,
                "Test notes"
        );
    }

    @Test
    public void testBlockCreation() {
        assertNotNull(block);
        assertEquals(1L, block.getFarmId());
        assertEquals("A1", block.getBlockName());
        assertEquals(1.0, block.getHectareSize(), 0.01);
        assertEquals(BlockEntity.BlockStatus.NOT_CLEARED, block.getStatus());
        assertEquals("Test notes", block.getNotes());
    }

    @Test
    public void testStatusProgression() {
        // Initial state
        assertEquals(BlockEntity.BlockStatus.NOT_CLEARED, block.getStatus());
        assertNull(block.getClearedDate());
        assertNull(block.getPlowedDate());
        assertNull(block.getPlantedDate());

        // Progress to CLEARED
        block.setStatus(BlockEntity.BlockStatus.CLEARED);
        block.setClearedDate(testDate);
        assertEquals(BlockEntity.BlockStatus.CLEARED, block.getStatus());
        assertEquals(testDate, block.getClearedDate().longValue());

        // Progress to PLOWED
        block.setStatus(BlockEntity.BlockStatus.PLOWED);
        block.setPlowedDate(testDate + 86400000); // +1 day
        assertEquals(BlockEntity.BlockStatus.PLOWED, block.getStatus());
        assertEquals(testDate + 86400000, block.getPlowedDate().longValue());

        // Progress to PLANTED
        block.setStatus(BlockEntity.BlockStatus.PLANTED);
        block.setPlantedDate(testDate + 172800000); // +2 days
        block.setSurvivalRate(95.5);
        block.setReplacementCount(3);

        assertEquals(BlockEntity.BlockStatus.PLANTED, block.getStatus());
        assertEquals(testDate + 172800000, block.getPlantedDate().longValue());
        assertEquals(95.5, block.getSurvivalRate(), 0.01);
        assertEquals(3, block.getReplacementCount());
    }

    @Test
    public void testTreeCalculations() {
        block.setStatus(BlockEntity.BlockStatus.PLANTED);
        block.setSurvivalRate(92.5);

        int aliveTrees = block.getAliveTrees();
        int deadTrees = block.getDeadTrees();

        assertEquals(93, aliveTrees); // 92.5% of 100 = 92.5 -> rounded to 93
        assertEquals(7, deadTrees);
        assertEquals(100, aliveTrees + deadTrees);
    }

    @Test
    public void testRelevantDateByStatus() {
        block.setStatus(BlockEntity.BlockStatus.CLEARED);
        block.setClearedDate(testDate);
        assertEquals(testDate, block.getRelevantDate().longValue());
        assertEquals("Cleared", block.getDateLabel());

        block.setStatus(BlockEntity.BlockStatus.PLOWED);
        block.setPlowedDate(testDate + 86400000);
        assertEquals(testDate + 86400000, block.getRelevantDate().longValue());
        assertEquals("Plowed", block.getDateLabel());

        block.setStatus(BlockEntity.BlockStatus.PLANTED);
        block.setPlantedDate(testDate + 172800000);
        assertEquals(testDate + 172800000, block.getRelevantDate().longValue());
        assertEquals("Planted", block.getDateLabel());
    }

    @Test
    public void testIsPlanted() {
        assertFalse(block.isPlanted());

        block.setStatus(BlockEntity.BlockStatus.PLANTED);
        assertTrue(block.isPlanted());
    }
}