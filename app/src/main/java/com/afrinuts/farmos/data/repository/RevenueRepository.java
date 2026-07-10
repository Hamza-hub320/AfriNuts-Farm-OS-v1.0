package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;

import java.util.List;

public class RevenueRepository extends BaseRepository {
    private static final String TAG = "RevenueRepository";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FirestoreSyncManager firestoreSyncManager;  // Add this

    public RevenueRepository(Context context, long farmId) {
        super(context, farmId);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
    }

    public List<RevenueEntity> getRevenuesByBatchId(long batchId) {
        return database.revenueDao().getRevenuesByBatchId(batchId);
    }

    public void insertRevenue(RevenueEntity revenue, InsertCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Save locally FIRST
                long id = database.revenueDao().insert(revenue);
                revenue.setId(id);
                Log.d(TAG, "Revenue saved locally with ID: " + id);

                // 2. Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("revenues", id, SyncEntity.SyncAction.CREATE);
                    Log.d(TAG, "Queued revenue for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // 3. Trigger auto-sync
                syncManager.triggerAutoSync();

                // 4. Return success to UI
                mainHandler.post(() -> callback.onSuccess(id));

            } catch (Exception e) {
                Log.e(TAG, "Error inserting revenue", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public LiveData<List<RevenueEntity>> getAllRevenues() {
        return database.revenueDao().getAllRevenuesLiveData();
    }

    public LiveData<RevenueEntity> getRevenueById(long revenueId) {
        return database.revenueDao().getRevenueByIdLiveData(revenueId);
    }

    public LiveData<List<RevenueEntity>> getRevenuesForFarm() {
        return database.revenueDao().getRevenuesByFarmIdLiveData(farmId);
    }

    public void updateRevenue(RevenueEntity revenue, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.revenueDao().update(revenue);
                Log.d(TAG, "Revenue updated locally: " + revenue.getId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("revenues", revenue.getId(), SyncEntity.SyncAction.UPDATE);
                    Log.d(TAG, "Queued revenue update for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Try to update Firestore (firestoreHelper will handle success/failure)
                firestoreHelper.updateRevenue(revenue)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Revenue updated in Firestore");
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore update failed", e);
                            // Queue already exists, just return success
                            mainHandler.post(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error updating revenue", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void deleteRevenue(RevenueEntity revenue, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.revenueDao().delete(revenue);
                Log.d(TAG, "Revenue deleted locally: " + revenue.getId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("revenues", revenue.getId(), SyncEntity.SyncAction.DELETE);
                    Log.d(TAG, "Queued revenue deletion for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Try to delete from Firestore
                firestoreHelper.deleteRevenue(revenue.getId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Revenue deleted from Firestore");
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore delete failed", e);
                            // Queue already exists, just return success
                            mainHandler.post(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting revenue", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void syncRevenueFromFirestore(com.google.firebase.firestore.DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                RevenueEntity revenue = EntityMapper.mapToRevenue(document);
                if (revenue != null) {
                    RevenueEntity existing = database.revenueDao().getRevenueById(revenue.getId());
                    if (existing == null) {
                        database.revenueDao().insert(revenue);
                        Log.d(TAG, "Synced new revenue from Firestore: " + revenue.getId());
                    } else {
                        // Compare timestamps to avoid overwriting newer local data
                        if (revenue.getUpdatedAt() > existing.getUpdatedAt()) {
                            database.revenueDao().update(revenue);
                            Log.d(TAG, "Synced updated revenue from Firestore: " + revenue.getId());
                        } else {
                            Log.d(TAG, "Skipped older revenue update from Firestore: " + revenue.getId());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing revenue from Firestore", e);
            }
        });
    }

    // For synchronous operations
    public List<RevenueEntity> getRevenuesSync() {
        return database.revenueDao().getRevenuesByFarmId(farmId);
    }
}