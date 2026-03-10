package com.afrinuts.farmos.data.entity;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PhotoEntityTest {

    private PhotoEntity blockPhoto;
    private PhotoEntity taskPhoto;
    private PhotoEntity expensePhoto;

    @Before
    public void setUp() {
        long now = System.currentTimeMillis();

        blockPhoto = new PhotoEntity(
                1L, 1L, null, null,
                "/storage/photos/block1.jpg",
                "Block A1 after planting",
                now
        );

        taskPhoto = new PhotoEntity(
                1L, null, 5L, null,
                "/storage/photos/task5.jpg",
                "Clearing in progress",
                now
        );

        expensePhoto = new PhotoEntity(
                1L, null, null, 10L,
                "/storage/photos/expense10.jpg",
                "Receipt for seedlings",
                now
        );
    }

    @Test
    public void testPhotoCreation() {
        assertNotNull(blockPhoto);
        assertEquals(1L, blockPhoto.getFarmId());
        assertEquals(Long.valueOf(1L), blockPhoto.getBlockId());
        assertNull(blockPhoto.getTaskId());
        assertNull(blockPhoto.getExpenseId());
        assertEquals("/storage/photos/block1.jpg", blockPhoto.getFilePath());
        assertEquals("Block A1 after planting", blockPhoto.getCaption());
    }

    @Test
    public void testPhotoTypeDetection() {
        assertEquals(PhotoEntity.TYPE_BLOCK, blockPhoto.getPhotoType());
        assertEquals(PhotoEntity.TYPE_TASK, taskPhoto.getPhotoType());
        assertEquals(PhotoEntity.TYPE_EXPENSE, expensePhoto.getPhotoType());
    }

    @Test
    public void testTypeDisplayName() {
        assertEquals("Block Photo", blockPhoto.getTypeDisplayName());
        assertEquals("Task Photo", taskPhoto.getTypeDisplayName());
        assertEquals("Expense Photo", expensePhoto.getTypeDisplayName());
    }

    @Test
    public void testIconResourceIds() {
        // We can't test R directly in unit tests, but we can test the method returns valid values
        assertNotEquals(0, blockPhoto.getIconResId());
        assertNotEquals(0, taskPhoto.getIconResId());
        assertNotEquals(0, expensePhoto.getIconResId());
    }

    @Test
    public void testUpdatePhotoType() {
        blockPhoto.setBlockId(null);
        blockPhoto.setTaskId(10L);
        assertEquals(PhotoEntity.TYPE_TASK, blockPhoto.getPhotoType());

        blockPhoto.setExpenseId(20L);
        blockPhoto.setTaskId(null);
        assertEquals(PhotoEntity.TYPE_EXPENSE, blockPhoto.getPhotoType());
    }

    @Test
    public void testCaptionUpdate() {
        String newCaption = "Updated caption";
        blockPhoto.setCaption(newCaption);
        assertEquals(newCaption, blockPhoto.getCaption());
    }

    @Test
    public void testLocationData() {
        blockPhoto.setLatitude(9.51);
        blockPhoto.setLongitude(-7.5692);

        assertEquals(9.51, blockPhoto.getLatitude(), 0.001);
        assertEquals(-7.5692, blockPhoto.getLongitude(), 0.001);
    }
}