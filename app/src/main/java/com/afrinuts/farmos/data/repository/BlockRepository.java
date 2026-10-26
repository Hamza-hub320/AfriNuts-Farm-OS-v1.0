package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.BlockDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockRepository {
    private static final String TAG = "BlockRepository";

    private BlockDao blockDao;
    private AppDatabase database;  // ✅ Add this
    private SyncManager syncManager;
    private FirebaseManager firebaseManager;
    private FirestoreSyncManager firestoreSyncManager;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private long farmId;

    public BlockRepository(Context context, long farmId) {
        this.database = AppDatabase.getInstance(context);  // ✅ Initialize database
        this.blockDao = database.blockDao();  // ✅ Get blockDao from database
        this.syncManager = SyncManager.getInstance(context);
        this.firebaseManager = FirebaseManager.getInstance();
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
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
            if (block.getHarvestHistory() == null) {
                block.setHarvestHistory(new Gson().toJson(new ArrayList<>()));
            }
            // 1. Insert into local Room DB
            long id = blockDao.insert(block);
            block.setId(id);
            Log.d(TAG, "Block inserted locally with ID: " + id +
                    " harvestCount: " + block.getHarvestCount());

            // 2. Queue for sync using SyncManager (only if NOT a remote update)
            if (!firestoreSyncManager.isRemoteUpdate()) {
                syncManager.queueForSync("blocks", id, SyncEntity.SyncAction.CREATE);
                Log.d(TAG, "Queued for sync (local change)");
            } else {
                Log.d(TAG, "Skipping queue - this is a remote update");
            }

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

    public BlockEntity getBlockByIdSync(long blockId) {
        return blockDao.getBlockById(blockId);
    }

    // ========================================
    // UPDATE
    // ========================================

    public CompletableFuture<Void> updateBlock(BlockEntity block) {
        return CompletableFuture.runAsync(() -> {
            try {
                // ✅ Update local database using blockDao
                blockDao.update(block);
                Log.d(TAG, "Block updated locally: " + block.getBlockName() +
                        " harvestCount: " + block.getHarvestCount() +
                        " totalHarvest: " + block.getTotalHarvestQuantity());

                // ✅ Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("blocks", block.getId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                // ✅ Also update Firestore directly (for immediate sync)
                Map<String, Object> blockMap = EntityMapper.blockToMap(block);
                firebaseManager.saveBlockMap(blockMap, String.valueOf(farmId), block.getId());

                Log.d(TAG, "Block synced to Firestore: " + block.getBlockName());

            } catch (Exception e) {
                Log.e(TAG, "Error updating block", e);
                throw new RuntimeException(e);
            }
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

            // 2. Queue for sync (only if NOT a remote update)
            if (!firestoreSyncManager.isRemoteUpdate()) {
                syncManager.queueForSync("blocks", block.getId(), SyncEntity.SyncAction.DELETE);
                Log.d(TAG, "Queued for sync (local change)");
            } else {
                Log.d(TAG, "Skipping queue - this is a remote update");
            }

            return null; // Void
        }, executorService);
    }

    // ========================================
    // SYNC METHODS (Called by AutoSyncWorker and listeners)
    // ========================================

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
                        Log.d(TAG, "Synced new block from Firestore: " + block.getBlockName() +
                                " harvestCount: " + block.getHarvestCount());
                    } else {
                        blockDao.update(block);
                        Log.d(TAG, "Synced updated block from Firestore: " + block.getBlockName() +
                                " harvestCount: " + block.getHarvestCount());
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
    // In BlockRepository.java - replace the documentToBlock method with this:

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

            // Create block with default constructor values
            BlockEntity block = new BlockEntity(farmId, blockName, hectareSize, notes);
            block.setId(id);
            block.setStatus(BlockEntity.BlockStatus.valueOf(status));
            block.setClearedDate(clearedDate);
            block.setPlowedDate(plowedDate);
            block.setPlantedDate(plantedDate);
            block.setSurvivalRate(survivalRate);
            block.setReplacementCount((int) replacementCount);

            // ✅ CRITICAL FIX: Parse harvest fields
            // 1. Harvest History (JSON string)
            String harvestHistory = doc.getString("harvestHistory");
            if (harvestHistory != null && !harvestHistory.isEmpty()) {
                block.setHarvestHistory(harvestHistory);
            } else {
                // Initialize empty array if null
                block.setHarvestHistory(new Gson().toJson(new ArrayList<>()));
            }

            // 2. Last Harvest Date
            Long lastHarvestDate = doc.getLong("lastHarvestDate");
            if (lastHarvestDate != null && lastHarvestDate > 0) {
                block.setLastHarvestDate(lastHarvestDate);
            }

            // 3. Last Harvest Quantity
            Double lastHarvestQuantity = doc.getDouble("lastHarvestQuantity");
            if (lastHarvestQuantity != null) {
                block.setLastHarvestQuantity(lastHarvestQuantity);
            }

            // 4. Total Harvest Quantity
            Double totalHarvestQuantity = doc.getDouble("totalHarvestQuantity");
            if (totalHarvestQuantity != null) {
                block.setTotalHarvestQuantity(totalHarvestQuantity);
            }

            // 5. Harvest Count
            Long harvestCount = doc.getLong("harvestCount");
            if (harvestCount != null) {
                block.setHarvestCount(harvestCount.intValue());
            }

            // Set audit timestamps
            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null) block.setCreatedAt(createdAt);

            Long updatedAt = doc.getLong("updatedAt");
            if (updatedAt != null) block.setUpdatedAt(updatedAt);

            Log.d(TAG, "Synced block: " + block.getBlockName() +
                    " harvestCount=" + block.getHarvestCount() +
                    " totalHarvest=" + block.getTotalHarvestQuantity());

            return block;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to block", e);
            return null;
        }
    }

    // Add this method to BlockRepository.java for debugging
    public void debugBlockData(long blockId) {
        executorService.execute(() -> {
            BlockEntity block = blockDao.getBlockById(blockId);
            if (block != null) {
                Log.d(TAG, "=== DEBUG BLOCK DATA ===");
                Log.d(TAG, "ID: " + block.getId());
                Log.d(TAG, "Name: " + block.getBlockName());
                Log.d(TAG, "Status: " + block.getStatus());
                Log.d(TAG, "Harvest Count: " + block.getHarvestCount());
                Log.d(TAG, "Total Harvest: " + block.getTotalHarvestQuantity());
                Log.d(TAG, "Last Harvest: " + block.getLastHarvestQuantity());
                Log.d(TAG, "Last Harvest Date: " + block.getLastHarvestDate());
                Log.d(TAG, "Harvest History: " + block.getHarvestHistory());

                // Parse and show harvest records
                List<BlockEntity.HarvestRecord> records = block.getHarvestRecords();
                Log.d(TAG, "Parsed Records Count: " + records.size());
                for (int i = 0; i < records.size(); i++) {
                    BlockEntity.HarvestRecord r = records.get(i);
                    Log.d(TAG, "Record " + i + ": " + r.getQuantityKg() + "kg on " + r.getFormattedDate());
                }
            } else {
                Log.d(TAG, "Block not found: " + blockId);
            }
        });
    }

    // ========================================
    // UTILITY
    // ========================================

    public void shutdown() {
        executorService.shutdown();
    }
}