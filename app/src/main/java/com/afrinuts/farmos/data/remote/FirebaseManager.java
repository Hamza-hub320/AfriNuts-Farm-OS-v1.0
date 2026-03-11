package com.afrinuts.farmos.data.remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private static FirebaseManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Collection names
    private static final String COLLECTION_FARMS = "farms";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_BLOCKS = "blocks";
    private static final String COLLECTION_EXPENSES = "expenses";
    private static final String COLLECTION_REVENUES = "revenues";
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_PHOTOS = "photos";

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // ========================================
    // AUTHENTICATION
    // ========================================

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String error);
    }

    public void signInWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    public void signUpWithEmail(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    public void signOut() {
        auth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    // ========================================
    // FARM OPERATIONS
    // ========================================

    public void saveFarm(FarmEntity farm, String farmId) {
        Map<String, Object> farmMap = new HashMap<>();
        farmMap.put("id", farmId);
        farmMap.put("name", farm.getName());
        farmMap.put("location", farm.getLocation());
        farmMap.put("totalHectares", farm.getTotalHectares());
        farmMap.put("cashewHectares", farm.getCashewHectares());
        farmMap.put("treesPerHectare", farm.getTreesPerHectare());
        farmMap.put("plantingYear", farm.getPlantingYear());
        farmMap.put("createdAt", farm.getCreatedAt());
        farmMap.put("updatedAt", System.currentTimeMillis());

        db.collection(COLLECTION_FARMS).document(farmId)
                .set(farmMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Farm saved: " + farmId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving farm", e));
    }

    public Task<DocumentSnapshot> getFarm(String farmId) {
        return db.collection(COLLECTION_FARMS).document(farmId).get();
    }

    // ========================================
    // BLOCK OPERATIONS
    // ========================================

    public void saveBlock(BlockEntity block, String farmId) {
        Map<String, Object> blockMap = new HashMap<>();
        blockMap.put("id", block.getId());
        blockMap.put("farmId", farmId);
        blockMap.put("blockName", block.getBlockName());
        blockMap.put("hectareSize", block.getHectareSize());
        blockMap.put("status", block.getStatus().name());
        blockMap.put("clearedDate", block.getClearedDate());
        blockMap.put("plowedDate", block.getPlowedDate());
        blockMap.put("plantedDate", block.getPlantedDate());
        blockMap.put("survivalRate", block.getSurvivalRate());
        blockMap.put("replacementCount", block.getReplacementCount());
        blockMap.put("notes", block.getNotes());
        blockMap.put("createdAt", block.getCreatedAt());
        blockMap.put("updatedAt", System.currentTimeMillis());

        String docId = farmId + "_" + block.getBlockName();
        db.collection(COLLECTION_BLOCKS).document(docId)
                .set(blockMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Block saved: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving block", e));
    }

    public Task<QuerySnapshot> getBlocksForFarm(String farmId) {
        return db.collection(COLLECTION_BLOCKS)
                .whereEqualTo("farmId", farmId)
                .get();
    }

    // ========================================
    // SYNC OPERATIONS
    // ========================================

    public interface SyncCallback {
        void onComplete();
        void onError(String error);
    }

    public void syncAllData(String farmId, SyncCallback callback) {
        List<Task<?>> tasks = new ArrayList<>();

        // Add all data fetching tasks
        tasks.add(getBlocksForFarm(farmId));
        tasks.add(getExpensesForFarm(farmId));
        tasks.add(getRevenuesForFarm(farmId));
        tasks.add(getTasksForFarm(farmId));

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(results -> {
                    // Process results and update local database
                    // This would call your existing DAOs to insert/update
                    callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    // Add similar methods for expenses, revenues, tasks, photos...
}