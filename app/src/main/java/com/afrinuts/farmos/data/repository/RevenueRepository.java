package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;

import java.util.List;

public class RevenueRepository extends BaseRepository {
    private static final String TAG = "RevenueRepository";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RevenueRepository(Context context, long farmId) {
        super(context, farmId); // update BaseRepository accordingly
    }

    public void insertRevenue(RevenueEntity revenue, InsertCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Save locally FIRST
                long id = database.revenueDao().insert(revenue);
                revenue.setId(id);
                Log.d(TAG, "Revenue saved locally with ID: " + id);

                // 2. Queue for sync IMMEDIATELY
                syncManager.queueForSync("revenues", id, SyncEntity.SyncAction.CREATE);

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

                firestoreHelper.updateRevenue(revenue)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Revenue updated in Firestore");
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore update failed", e);
                            syncManager.queueForSync("revenues", revenue.getId(), SyncEntity.SyncAction.UPDATE);
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

                firestoreHelper.deleteRevenue(revenue.getId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Revenue deleted from Firestore");
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore delete failed", e);
                            syncManager.queueForSync("revenues", revenue.getId(), SyncEntity.SyncAction.DELETE);
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
                        Log.d(TAG, "Synced new revenue: " + revenue.getId());
                    } else {
                        database.revenueDao().update(revenue);
                        Log.d(TAG, "Synced updated revenue: " + revenue.getId());
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