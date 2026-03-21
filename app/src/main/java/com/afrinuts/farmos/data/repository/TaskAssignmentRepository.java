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
    private FirestoreSyncManager firestoreSyncManager;

    public TaskAssignmentRepository(Context context, long farmId) {
        super(context, farmId);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertAssignment(TaskAssignmentHistoryEntity assignment, InsertCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "📝 insertAssignment called - taskId: " + assignment.getTaskId() +
                        ", worker: " + assignment.getAssignedTo());

                // 1. Insert into local database FIRST
                long id = database.taskAssignmentDao().insert(assignment);
                assignment.setId(id);
                Log.d(TAG, "✅ Assignment saved locally with ID: " + id);

                // 2. Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("assignments", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                    Log.d(TAG, "📤 Queued assignment for sync (local change)");
                } else {
                    Log.d(TAG, "⏭️ Skipping queue - this is a remote update");
                }

                // 3. Return success immediately (offline-first)
                mainHandler.post(() -> callback.onSuccess(id));

            } catch (Exception e) {
                Log.e(TAG, "❌ Error inserting assignment", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
// READ - LiveData for UI
// ========================================

    public LiveData<List<TaskAssignmentHistoryEntity>> getAssignmentsForTaskLiveData(long taskId) {
        return database.taskAssignmentDao().getAssignmentsForTaskLiveData(taskId);
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
// READ - Callback methods (for background operations)
// ========================================

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

    // ========================================
    // UPDATE
    // ========================================

    public void completeAssignment(TaskAssignmentHistoryEntity assignment, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                assignment.completeAssignment();
                database.taskAssignmentDao().update(assignment);
                Log.d(TAG, "✅ Assignment completed locally: " + assignment.getId());

                // Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("assignments", assignment.getId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                    Log.d(TAG, "📤 Queued assignment update for sync");
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "❌ Error completing assignment", e);
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
                Log.d(TAG, "✅ Assignment deleted locally: " + assignment.getId());

                // Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("assignments", assignment.getId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                    Log.d(TAG, "📤 Queued assignment deletion for sync");
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "❌ Error deleting assignment", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // SYNC FROM FIRESTORE (called by listeners)
    // ========================================

    public void syncAssignmentFromFirestore(com.google.firebase.firestore.DocumentSnapshot document, long taskId) {
        executor.execute(() -> {
            try {
                TaskAssignmentHistoryEntity assignment = EntityMapper.mapToAssignment(document);
                if (assignment == null) return;

                // Ensure the assignment is linked to the correct task
                assignment.setTaskId(taskId);
                assignment.setFarmId(farmId);

                TaskAssignmentHistoryEntity existing =
                        database.taskAssignmentDao().getAssignmentById(assignment.getId());

                if (existing == null) {
                    database.taskAssignmentDao().insert(assignment);
                    Log.d(TAG, "🔄 Synced new assignment from Firestore: " + assignment.getId());
                } else {
                    // Use assignedAt as timestamp for conflict resolution
                    if (assignment.getAssignedAt() > existing.getAssignedAt()) {
                        database.taskAssignmentDao().update(assignment);
                        Log.d(TAG, "🔄 Synced updated assignment from Firestore: " + assignment.getId());
                    } else {
                        Log.d(TAG, "⏭️ Skipped older assignment update: " + assignment.getId());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error syncing assignment", e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}