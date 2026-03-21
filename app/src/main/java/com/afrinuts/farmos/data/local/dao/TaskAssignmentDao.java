package com.afrinuts.farmos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;

import java.util.List;

@Dao
public interface TaskAssignmentDao {

    @Insert
    long insert(TaskAssignmentHistoryEntity assignment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(TaskAssignmentHistoryEntity assignment);

    @Update
    void update(TaskAssignmentHistoryEntity assignment);

    @Delete
    void delete(TaskAssignmentHistoryEntity assignment);

    @Query("DELETE FROM task_assignments WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM task_assignments WHERE id = :id")
    TaskAssignmentHistoryEntity getAssignmentById(long id);

    @Query("SELECT * FROM task_assignments WHERE id = :id")
    LiveData<TaskAssignmentHistoryEntity> getAssignmentByIdLiveData(long id);

    @Query("SELECT * FROM task_assignments WHERE taskId = :taskId ORDER BY assignedAt DESC")
    List<TaskAssignmentHistoryEntity> getAssignmentsForTask(long taskId);

    @Query("SELECT * FROM task_assignments WHERE assignedTo = :workerName AND farmId = :farmId ORDER BY assignedAt DESC")
    List<TaskAssignmentHistoryEntity> getAssignmentsForWorker(long farmId, String workerName);

    @Query("SELECT * FROM task_assignments WHERE assignedTo = :workerName AND farmId = :farmId")
    LiveData<List<TaskAssignmentHistoryEntity>> getAssignmentsForWorkerLiveData(long farmId, String workerName);

    @Query("SELECT * FROM task_assignments WHERE unassignedAt IS NULL AND farmId = :farmId")
    List<TaskAssignmentHistoryEntity> getActiveAssignments(long farmId);

    @Query("SELECT * FROM task_assignments WHERE unassignedAt IS NULL AND farmId = :farmId")
    LiveData<List<TaskAssignmentHistoryEntity>> getActiveAssignmentsLiveData(long farmId);

    @Query("SELECT * FROM task_assignments WHERE farmId = :farmId ORDER BY assignedAt DESC")
    List<TaskAssignmentHistoryEntity> getAllAssignmentsForFarm(long farmId);

    @Query("SELECT * FROM task_assignments WHERE farmId = :farmId ORDER BY assignedAt DESC")
    LiveData<List<TaskAssignmentHistoryEntity>> getAllAssignmentsForFarmLiveData(long farmId);

    @Query("SELECT * FROM task_assignments ORDER BY assignedAt DESC")
    List<TaskAssignmentHistoryEntity> getAllAssignments();

    @Query("SELECT COUNT(*) FROM task_assignments WHERE assignedTo = :workerName AND farmId = :farmId")
    int getTaskCountForWorker(long farmId, String workerName);

    @Query("SELECT COUNT(*) FROM task_assignments WHERE assignedTo = :workerName AND farmId = :farmId AND unassignedAt IS NOT NULL")
    int getCompletedTaskCountForWorker(long farmId, String workerName);

    @Query("SELECT * FROM task_assignments WHERE taskId = :taskId ORDER BY assignedAt DESC")
    LiveData<List<TaskAssignmentHistoryEntity>> getAssignmentsForTaskLiveData(long taskId);
    }