package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.SupplierScoreSnapshotDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.SupplierScoreSnapshotEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Insert-and-read facade. No update, no delete — a written snapshot cannot be
 * altered even by accident. The engine (portal side) appends through
 * appendSnapshot(); the app reads via getLatestBySupplierId / getHistoryBySupplierId
 * for Step 6c displays.
 */
public class SupplierScoreSnapshotRepository {
    private static final String TAG = "SnapshotRepo";

    private final SupplierScoreSnapshotDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final long farmId;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public SupplierScoreSnapshotRepository(Context context, long farmId) {
        this.dao = AppDatabase.getInstance(context).supplierScoreSnapshotDao();
        this.farmId = farmId;
    }

    // ========================================
    // WRITE — the only write door
    // ========================================

    public CompletableFuture<Long> appendSnapshot(SupplierScoreSnapshotEntity snapshot,
                                                  Callback<Long> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (snapshot.getScoringVersion() == null || snapshot.getScoringVersion().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Snapshot must carry a scoringVersion — reproducibility requirement");
                }
                if (snapshot.getCalculatedAt() <= 0) {
                    snapshot.setCalculatedAt(System.currentTimeMillis());
                }
                snapshot.setFarmId(farmId);

                long id = dao.insert(snapshot);
                snapshot.setId(id);

                Log.d(TAG, "Appended snapshot " + id + " for supplier "
                        + snapshot.getSupplierId()
                        + " (v=" + snapshot.getScoringVersion()
                        + ", overall=" + snapshot.getOverall() + ")");
                if (callback != null) callback.onSuccess(id);
                return id;
            } catch (Exception e) {
                Log.e(TAG, "Error appending snapshot", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // READS
    // ========================================

    public LiveData<SupplierScoreSnapshotEntity> getLatestLive(long supplierId) {
        return dao.getLatestBySupplierIdLive(supplierId);
    }

    public CompletableFuture<SupplierScoreSnapshotEntity> getLatest(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> dao.getLatestBySupplierId(supplierId), executor);
    }

    public LiveData<List<SupplierScoreSnapshotEntity>> getHistoryLive(long supplierId) {
        return dao.getHistoryBySupplierIdLive(supplierId);
    }

    public CompletableFuture<List<SupplierScoreSnapshotEntity>> getHistory(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> dao.getHistoryBySupplierId(supplierId), executor);
    }

    public LiveData<SupplierScoreSnapshotEntity> getSnapshotByIdLive(long id) {
        return dao.getByIdLive(id);
    }

    // ========================================
    // REMOTE SYNC APPLY — Firestore → Room only
    // ========================================

    /**
     * Snapshots flow DOWNWARD (portal → app). Because they are append-only and
     * identified by their auto-generated long ID, applying a remote snapshot is
     * a simple insert-if-absent. We never overwrite an existing local snapshot.
     */
    public void applySnapshotFromFirestore(DocumentSnapshot doc) {
        executor.execute(() -> {
            try {
                SupplierScoreSnapshotEntity remote = EntityMapper.mapToSnapshot(doc);
                if (remote == null) return;

                SupplierScoreSnapshotEntity local = dao.getById(remote.getId());
                if (local == null) {
                    dao.insert(remote);
                    Log.d(TAG, "➕ Applied remote snapshot " + remote.getId()
                            + " for supplier " + remote.getSupplierId());
                }
                // If it already exists, do NOTHING — snapshots are immutable.
            } catch (Exception e) {
                Log.e(TAG, "Error applying snapshot from Firestore", e);
            }
        });
    }

    public void shutdown() { executor.shutdown(); }
}