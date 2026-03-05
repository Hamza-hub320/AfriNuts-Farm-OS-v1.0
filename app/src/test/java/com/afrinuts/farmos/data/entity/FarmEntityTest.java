package com.afrinuts.farmos.data.entity;

import com.afrinuts.farmos.data.local.entity.FarmEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class FarmEntityTest {

    private FarmEntity farm;

    @Before
    public void setUp() {
        farm = new FarmEntity(
                "AfriNuts Odienné",
                "Odienné, Côte d'Ivoire",
                35.0,
                35.0,
                100,
                2024
        );
    }

    @Test
    public void testFarmCreation() {
        assertNotNull(farm);
        assertEquals("AfriNuts Odienné", farm.getName());
        assertEquals("Odienné, Côte d'Ivoire", farm.getLocation());
        assertEquals(35.0, farm.getTotalHectares(), 0.01);
        assertEquals(35.0, farm.getCashewHectares(), 0.01);
        assertEquals(100, farm.getTreesPerHectare());
        assertEquals(2024, farm.getPlantingYear().intValue());
    }

    @Test
    public void testTotalTreesCalculation() {
        int expectedTrees = (int)(farm.getCashewHectares() * farm.getTreesPerHectare());
        assertEquals(3500, expectedTrees);
    }

    @Test
    public void testSettersAndGetters() {
        farm.setName("New Farm Name");
        farm.setLocation("New Location");
        farm.setTotalHectares(40.0);
        farm.setCashewHectares(38.0);
        farm.setTreesPerHectare(110);
        farm.setPlantingYear(2025);

        assertEquals("New Farm Name", farm.getName());
        assertEquals("New Location", farm.getLocation());
        assertEquals(40.0, farm.getTotalHectares(), 0.01);
        assertEquals(38.0, farm.getCashewHectares(), 0.01);
        assertEquals(110, farm.getTreesPerHectare());
        assertEquals(2025, farm.getPlantingYear().intValue());
    }

    @Test
    public void testTimestamps() {
        long beforeCreation = System.currentTimeMillis() - 1000;
        FarmEntity newFarm = new FarmEntity("Test", "Test", 1.0, 1.0, 100, 2024);
        long afterCreation = System.currentTimeMillis() + 1000;

        assertTrue(newFarm.getCreatedAt() >= beforeCreation);
        assertTrue(newFarm.getCreatedAt() <= afterCreation);
        assertEquals(newFarm.getCreatedAt(), newFarm.getUpdatedAt());
    }
}