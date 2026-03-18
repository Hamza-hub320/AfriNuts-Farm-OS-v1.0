package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.BlockDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockRepository {
    private static final String TAG = "BlockRepository";

    private BlockDao blockDao;
    private SyncManager syncManager;
    private FirebaseManager firebaseManager;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private long farmId;

    public BlockRepository(Context context, long farmId) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.blockDao = db.blockDao();
        this.syncManager = SyncManager.getInstance(context);
        this.firebaseManager = FirebaseManager.getInstance();
        this.farmId = farmId;
    }

    /**
     * Get all blocks synchronously (for background threads)
     * Use this ONLY in background threads, never on main thread
     */
    public List<BlockEntity> getBlocksSync() {
        // This runs on the calling thread - make sure it's a background thread!
        return blockDao.getBlocksByFarmId(farmId);
    }

    // ========================================
    // CREATE
    // ========================================

    public CompletableFuture<Long> insertBlock(BlockEntity block) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Insert into local Room DB
            long id = blockDao.insert(block);
            block.setId(id);
            Log.d(TAG, "Block inserted locally with ID: " + id);

            // 2. Queue for sync using SyncManager
            syncManager.queueForSync("block", id, SyncEntity.SyncAction.CREATE);

            return id;
        }, executorService);
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<BlockEntity>> getAllBlocks() {
        return blockDao.getBlocksByFarmIdLive(farmId);
    }

    public LiveData<BlockEntity> getBlockById(long blockId) {
        return blockDao.getBlockByIdLive(blockId);
    }

    public List<BlockEntity> getAllBlocksSync() {
        return blockDao.getBlocksByFarmId(farmId);
    }

    // ========================================
    // UPDATE
    // ========================================

    public CompletableFuture<Void> updateBlock(BlockEntity block) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Update local Room DB
            blockDao.update(block);
            Log.d(TAG, "Block updated locally: " + block.getId());

            // 2. Queue for sync
            syncManager.queueForSync("block", block.getId(), SyncEntity.SyncAction.UPDATE);

            return null; // Void
        }, executorService);
    }

    // ========================================
    // DELETE
    // ========================================

    public CompletableFuture<Void> deleteBlock(BlockEntity block) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Delete from local Room DB
            blockDao.delete(block);
            Log.d(TAG, "Block deleted locally: " + block.getId());

            // 2. Queue for sync
            syncManager.queueForSync("block", block.getId(), SyncEntity.SyncAction.DELETE);

            return null; // Void
        }, executorService);
    }

    // ========================================
    // SYNC METHODS (Called by AutoSyncWorker and listeners)
    // ========================================

    public BlockEntity getBlockByIdSync(long blockId) {
        return blockDao.getBlockById(blockId);
    }

    /**
     * Sync a block from Firestore (called when remote changes arrive)
     */
    public void syncBlockFromFirestore(DocumentSnapshot document) {
        executorService.execute(() -> {
            try {
                BlockEntity block = documentToBlock(document);
                if (block != null) {
                    BlockEntity existing = blockDao.getBlockById(block.getId());
                    if (existing == null) {
                        blockDao.insert(block);
                        Log.d(TAG, "Synced new block from Firestore: " + block.getBlockName());
                    } else {
                        blockDao.update(block);
                        Log.d(TAG, "Synced updated block from Firestore: " + block.getBlockName());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing block from Firestore", e);
            }
        });
    }

    /**
     * Convert Firestore document to BlockEntity
     */
    private BlockEntity documentToBlock(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            long farmId = doc.getLong("farmId");
            String blockName = doc.getString("blockName");
            double hectareSize = doc.getDouble("hectareSize");
            String status = doc.getString("status");
            Long clearedDate = doc.getLong("clearedDate");
            Long plowedDate = doc.getLong("plowedDate");
            Long plantedDate = doc.getLong("plantedDate");
            double survivalRate = doc.getDouble("survivalRate");
            long replacementCount = doc.getLong("replacementCount");
            String notes = doc.getString("notes");

            BlockEntity block = new BlockEntity(farmId, blockName, hectareSize, notes);
            block.setId(id);
            block.setStatus(BlockEntity.BlockStatus.valueOf(status));
            block.setClearedDate(clearedDate);
            block.setPlowedDate(plowedDate);
            block.setPlantedDate(plantedDate);
            block.setSurvivalRate(survivalRate);
            block.setReplacementCount((int) replacementCount);

            return block;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to block", e);
            return null;
        }
    }

    // ========================================
    // UTILITY
    // ========================================

    public void shutdown() {
        executorService.shutdown();
    }
}