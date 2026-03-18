package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskAssignmentRepository extends BaseRepository {
    private static final String TAG = "TaskAssignmentRepo";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TaskAssignmentRepository(Context context, long farmId) {
        super(context, farmId);
    }

    // Add these to TaskAssignmentRepository.java
    public LiveData<List<TaskAssignmentHistoryEntity>> getAllAssignmentsForFarmLiveData() {
        return database.taskAssignmentDao().getAllAssignmentsForFarmLiveData(farmId);
    }

    public LiveData<List<TaskAssignmentHistoryEntity>> getAssignmentsForWorkerLiveData(String workerName) {
        return database.taskAssignmentDao().getAssignmentsForWorkerLiveData(farmId, workerName);
    }

    public LiveData<List<TaskAssignmentHistoryEntity>> getActiveAssignmentsLiveData() {
        return database.taskAssignmentDao().getActiveAssignmentsLiveData(farmId);
    }

    public LiveData<TaskAssignmentHistoryEntity> getAssignmentByIdLiveData(long assignmentId) {
        return database.taskAssignmentDao().getAssignmentByIdLiveData(assignmentId);
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertAssignment(TaskAssignmentHistoryEntity assignment, InsertCallback callback) {
        executor.execute(() -> {
            try {
                long id = database.taskAssignmentDao().insert(assignment);
                assignment.setId(id);

                Log.d(TAG, "Assignment saved locally: " + id);

                firestoreHelper.uploadAssignment(assignment)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Assignment uploaded: " + id);
                            mainHandler.post(() -> callback.onSuccess(id));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Upload failed, queueing sync", e);
                            syncManager.queueForSync("assignments", id, SyncEntity.SyncAction.CREATE);
                            mainHandler.post(() -> callback.onSuccess(id));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error inserting assignment", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public void getAllAssignments(RepositoryCallback<List<TaskAssignmentHistoryEntity>> callback) {
        executor.execute(() -> {
            try {
                List<TaskAssignmentHistoryEntity> result =
                        database.taskAssignmentDao().getAllAssignmentsForFarm(farmId);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error getting assignments", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getAssignmentsForWorker(String workerName, RepositoryCallback<List<TaskAssignmentHistoryEntity>> callback) {
        executor.execute(() -> {
            try {
                List<TaskAssignmentHistoryEntity> result =
                        database.taskAssignmentDao().getAssignmentsForWorker(farmId, workerName);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error getting worker assignments", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getActiveAssignments(RepositoryCallback<List<TaskAssignmentHistoryEntity>> callback) {
        executor.execute(() -> {
            try {
                List<TaskAssignmentHistoryEntity> result =
                        database.taskAssignmentDao().getActiveAssignments(farmId);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error getting active assignments", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // UPDATE
    // ========================================

    public void completeAssignment(TaskAssignmentHistoryEntity assignment, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                assignment.completeAssignment();
                database.taskAssignmentDao().update(assignment);

                firestoreHelper.updateAssignment(assignment)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Assignment completed: " + assignment.getId());
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Update failed, queueing sync", e);
                            syncManager.queueForSync("assignments", assignment.getId(), SyncEntity.SyncAction.UPDATE);
                            mainHandler.post(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error completing assignment", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deleteAssignment(TaskAssignmentHistoryEntity assignment, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.taskAssignmentDao().delete(assignment);

                firestoreHelper.deleteAssignment(assignment.getId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Assignment deleted: " + assignment.getId());
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Delete failed, queueing sync", e);
                            syncManager.queueForSync("assignments", assignment.getId(), SyncEntity.SyncAction.DELETE);
                            mainHandler.post(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting assignment", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // SYNC FROM FIRESTORE
    // ========================================

    public void syncAssignmentFromFirestore(com.google.firebase.firestore.DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                TaskAssignmentHistoryEntity assignment = EntityMapper.mapToAssignment(document);
                if (assignment != null) {
                    TaskAssignmentHistoryEntity existing =
                            database.taskAssignmentDao().getAssignmentById(assignment.getId());
                    if (existing == null) {
                        database.taskAssignmentDao().insert(assignment);
                        Log.d(TAG, "Synced new assignment: " + assignment.getId());
                    } else {
                        database.taskAssignmentDao().update(assignment);
                        Log.d(TAG, "Synced updated assignment: " + assignment.getId());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing assignment", e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}