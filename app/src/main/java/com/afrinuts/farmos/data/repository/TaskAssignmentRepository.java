package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskAssignmentRepository extends BaseRepository {
    private static final String TAG = "TaskAssignmentRepo";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private FirestoreSyncManager firestoreSyncManager;  // Add this

    public TaskAssignmentRepository(Context context, long farmId) {
        super(context, farmId);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
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

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("assignments", id, SyncEntity.SyncAction.CREATE);
                    Log.d(TAG, "Queued assignment for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Try to upload to Firestore
                firestoreHelper.uploadAssignment(assignment)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Assignment uploaded: " + id);
                            mainHandler.post(() -> callback.onSuccess(id));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Upload failed, item already queued", e);
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
                Log.d(TAG, "Assignment completed locally: " + assignment.getId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("assignments", assignment.getId(), SyncEntity.SyncAction.UPDATE);
                    Log.d(TAG, "Queued assignment update for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Try to update Firestore
                firestoreHelper.updateAssignment(assignment)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Assignment updated in Firestore: " + assignment.getId());
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore update failed", e);
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
                Log.d(TAG, "Assignment deleted locally: " + assignment.getId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("assignments", assignment.getId(), SyncEntity.SyncAction.DELETE);
                    Log.d(TAG, "Queued assignment deletion for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Try to delete from Firestore
                firestoreHelper.deleteAssignment(assignment.getId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Assignment deleted from Firestore: " + assignment.getId());
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore delete failed", e);
                            mainHandler.post(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting assignment", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // SYNC FROM FIRESTORE with Conflict Resolution
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
                        Log.d(TAG, "Synced new assignment from Firestore: " + assignment.getId());
                    } else {
                        // Use assignedAt as timestamp for conflict resolution
                        if (assignment.getAssignedAt() > existing.getAssignedAt()) {
                            database.taskAssignmentDao().update(assignment);
                            Log.d(TAG, "Synced updated assignment from Firestore: " + assignment.getId());
                        } else {
                            Log.d(TAG, "Skipped older assignment update: " + assignment.getId());
                        }
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