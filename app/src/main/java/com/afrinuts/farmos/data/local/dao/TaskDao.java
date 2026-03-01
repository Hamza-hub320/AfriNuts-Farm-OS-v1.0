package com.afrinuts.farmos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;

import java.util.List;

@Dao
public interface TaskDao {

    // Task CRUD
    @Insert
    long insertTask(TaskEntity task);

    @Update
    void updateTask(TaskEntity task);

    @Delete
    void deleteTask(TaskEntity task);

    @Query("SELECT * FROM tasks WHERE id = :id")
    TaskEntity getTaskById(long id);

    @Query("SELECT * FROM tasks WHERE farmId = :farmId ORDER BY dueDate ASC")
    List<TaskEntity> getTasksByFarmId(long farmId);

    @Query("SELECT * FROM tasks WHERE blockId = :blockId ORDER BY dueDate ASC")
    List<TaskEntity> getTasksByBlockId(long blockId);

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDate ASC")
    List<TaskEntity> getTasksByStatus(TaskEntity.TaskStatus status);

    @Query("SELECT * FROM tasks WHERE type = :type ORDER BY dueDate ASC")
    List<TaskEntity> getTasksByType(TaskEntity.TaskType type);

    @Query("SELECT * FROM tasks WHERE dueDate < :currentTime AND status != 'COMPLETED' ORDER BY dueDate ASC")
    List<TaskEntity> getOverdueTasks(long currentTime);

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED'")
    int getCompletedTaskCount();

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'COMPLETED'")
    int getPendingTaskCount();

    // Assignment History CRUD
    @Insert
    long insertAssignment(TaskAssignmentHistoryEntity assignment);

    @Update
    void updateAssignment(TaskAssignmentHistoryEntity assignment);

    @Delete
    void deleteAssignment(TaskAssignmentHistoryEntity assignment);

    @Query("SELECT * FROM task_assignments WHERE taskId = :taskId ORDER BY assignedAt DESC")
    List<TaskAssignmentHistoryEntity> getAssignmentsForTask(long taskId);

    @Query("SELECT * FROM task_assignments WHERE assignedTo = :worker AND unassignedAt IS NULL")
    List<TaskAssignmentHistoryEntity> getActiveAssignmentsForWorker(String worker);

    @Query("SELECT * FROM task_assignments WHERE taskId = :taskId AND unassignedAt IS NULL")
    TaskAssignmentHistoryEntity getCurrentAssignment(long taskId);

    @Query("SELECT assignedTo, COUNT(*) as taskCount FROM task_assignments " +
            "GROUP BY assignedTo ORDER BY taskCount DESC")
    List<WorkerTaskCount> getWorkerTaskCounts();

    // Inner class for worker statistics
    class WorkerTaskCount {
        public String assignedTo;
        public int taskCount;
    }
}