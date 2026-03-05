package com.afrinuts.farmos.data.entity;

import com.afrinuts.farmos.data.local.entity.TaskEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

public class TaskEntityTest {

    private TaskEntity task;
    private long dueDate;
    private long pastDate;

    @Before
    public void setUp() {
        dueDate = Calendar.getInstance().getTimeInMillis() + 86400000; // tomorrow
        pastDate = Calendar.getInstance().getTimeInMillis() - 86400000; // yesterday

        task = new TaskEntity(
                1L, // farmId
                2L, // blockId
                "Clear Block A1",
                "Clear land for planting cashew trees",
                TaskEntity.TaskType.CLEARING,
                TaskEntity.TaskStatus.PENDING,
                dueDate
        );
    }

    @Test
    public void testTaskCreation() {
        assertNotNull(task);
        assertEquals(1L, task.getFarmId());
        assertEquals(2L, task.getBlockId().longValue());
        assertEquals("Clear Block A1", task.getTitle());
        assertEquals("Clear land for planting cashew trees", task.getDescription());
        assertEquals(TaskEntity.TaskType.CLEARING, task.getType());
        assertEquals(TaskEntity.TaskStatus.PENDING, task.getStatus());
        assertEquals(dueDate, task.getDueDate());
        assertNull(task.getCompletedDate());
    }

    @Test
    public void testStatusTransition() {
        assertEquals(TaskEntity.TaskStatus.PENDING, task.getStatus());

        task.setStatus(TaskEntity.TaskStatus.IN_PROGRESS);
        assertEquals(TaskEntity.TaskStatus.IN_PROGRESS, task.getStatus());

        task.setStatus(TaskEntity.TaskStatus.COMPLETED);
        assertEquals(TaskEntity.TaskStatus.COMPLETED, task.getStatus());
        assertNotNull(task.getCompletedDate());
    }

    @Test
    public void testOverdueCalculation() {
        TaskEntity overdueTask = new TaskEntity(
                1L, null, "Overdue Task", "",
                TaskEntity.TaskType.OTHER,
                TaskEntity.TaskStatus.PENDING,
                pastDate
        );

        assertTrue(overdueTask.isOverdue());
        assertFalse(task.isOverdue()); // due tomorrow, not overdue
    }

    @Test
    public void testTaskTypeProperties() {
        TaskEntity.TaskType type = TaskEntity.TaskType.CLEARING;
        assertEquals("Land Clearing", type.getDisplayName());
        assertEquals("🚜", type.getIcon());
        assertEquals("Clear land for planting", type.getDescription());
    }

    @Test
    public void testTaskStatusProperties() {
        TaskEntity.TaskStatus status = TaskEntity.TaskStatus.PENDING;
        assertEquals("Pending", status.getDisplayName());
        assertEquals("⏳", status.getIcon());
        assertEquals("Not started", status.getDescription());
    }

    @Test
    public void testFarmWideTask() {
        TaskEntity farmWideTask = new TaskEntity(
                1L,
                null, // no blockId = farm-wide
                "Farm Maintenance",
                "General maintenance",
                TaskEntity.TaskType.MAINTENANCE,
                TaskEntity.TaskStatus.PENDING,
                dueDate
        );

        assertNull(farmWideTask.getBlockId());
    }
}