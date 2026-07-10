package com.afrinuts.farmos.data.remote;

import android.util.Log;

import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {
    private static final String TAG = "FirestoreHelper";

    private final FirebaseFirestore db;
    private final long farmId;  // change from String to long

    private static final String COLLECTION_FARMS = "farms";
    private static final String SUBCOLLECTION_BLOCKS = "blocks";
    private static final String SUBCOLLECTION_EXPENSES = "expenses";
    private static final String SUBCOLLECTION_REVENUES = "revenues";
    private static final String SUBCOLLECTION_TASKS = "tasks";
    private static final String SUBCOLLECTION_PHOTOS = "photos";
    private static final String STORAGE_BUCKET = "photos";
    private static final String SUBCOLLECTION_TASK_ASSIGNMENTS = "task_assignments";

    // Updated constructor
    public FirestoreHelper(long farmId) {
        this.db = FirebaseFirestore.getInstance();
        this.farmId = farmId;
    }

    // ===============================
    // Example: BLOCKS
    // ===============================
    public Task<Void> uploadBlock(BlockEntity block) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", block.getId());
        data.put("farmId", farmId); // now a number
        data.put("blockName", block.getBlockName());
        data.put("hectareSize", block.getHectareSize());
        data.put("status", block.getStatus().name());
        data.put("clearedDate", block.getClearedDate());
        data.put("plowedDate", block.getPlowedDate());
        data.put("plantedDate", block.getPlantedDate());
        data.put("survivalRate", block.getSurvivalRate());
        data.put("replacementCount", block.getReplacementCount());
        data.put("notes", block.getNotes());
        data.put("createdAt", block.getCreatedAt());
        data.put("updatedAt", System.currentTimeMillis());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId)) // Firestore document ID must be string
                .collection(SUBCOLLECTION_BLOCKS)
                .document(String.valueOf(block.getId()))
                .set(data)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Block uploaded with ID: " + block.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error uploading block", e));
    }

    public Task<Void> updateBlock(BlockEntity block) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", block.getStatus().name());
        updates.put("clearedDate", block.getClearedDate());
        updates.put("plowedDate", block.getPlowedDate());
        updates.put("plantedDate", block.getPlantedDate());
        updates.put("survivalRate", block.getSurvivalRate());
        updates.put("replacementCount", block.getReplacementCount());
        updates.put("notes", block.getNotes());
        updates.put("updatedAt", System.currentTimeMillis());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_BLOCKS)
                .document(String.valueOf(block.getId()))
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Block updated: " + block.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating block", e));
    }

    public Task<Void> deleteBlock(long blockId) {
        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_BLOCKS)
                .document(String.valueOf(blockId))
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Block deleted: " + blockId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error deleting block", e));
    }

    // ========================================
    // EXPENSES
    // ========================================

    public Task<Void> uploadExpense(ExpenseEntity expense) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", expense.getId());
        data.put("farmId", farmId);
        data.put("blockId", expense.getBlockId());
        data.put("category", expense.getCategory().name());
        data.put("amount", expense.getAmount());
        data.put("date", expense.getDate());
        data.put("description", expense.getDescription());
        data.put("receiptPath", expense.getReceiptPath());
        data.put("createdAt", expense.getCreatedAt());
        data.put("updatedAt", System.currentTimeMillis());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_EXPENSES)
                .document(String.valueOf(expense.getId()))
                .set(data)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Expense uploaded with ID: " + expense.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error uploading expense", e));
    }

    public Task<Void> updateExpense(ExpenseEntity expense) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("amount", expense.getAmount());
        updates.put("description", expense.getDescription());
        updates.put("receiptPath", expense.getReceiptPath());
        updates.put("updatedAt", System.currentTimeMillis());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_EXPENSES)
                .document(String.valueOf(expense.getId()))
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Expense updated: " + expense.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating expense", e));
    }

    public Task<Void> deleteExpense(long expenseId) {
        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_EXPENSES)
                .document(String.valueOf(expenseId))
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Expense deleted: " + expenseId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error deleting expense", e));
    }

    // ========================================
    // REVENUES
    // ========================================

    public Task<Void> uploadRevenue(RevenueEntity revenue) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", revenue.getId());
        data.put("farmId", farmId);
        data.put("blockId", revenue.getBlockId());
        data.put("harvestDate", revenue.getHarvestDate());
        data.put("quantityKg", revenue.getQuantityKg());
        data.put("pricePerKg", revenue.getPricePerKg());
        data.put("totalAmount", revenue.getTotalAmount());
        data.put("buyer", revenue.getBuyer());
        data.put("quality", revenue.getQuality().name());
        data.put("notes", revenue.getNotes());
        data.put("createdAt", revenue.getCreatedAt());
        data.put("updatedAt", System.currentTimeMillis());
        data.put("harvestCycleId", revenue.getHarvestCycleId());
        data.put("harvestYear", revenue.getHarvestYear());
        data.put("harvestSeason", revenue.getHarvestSeason());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_REVENUES)
                .document(String.valueOf(revenue.getId()))
                .set(data)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Revenue uploaded with ID: " + revenue.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error uploading revenue", e));
    }

    public Task<Void> updateRevenue(RevenueEntity revenue) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("quantityKg", revenue.getQuantityKg());
        updates.put("pricePerKg", revenue.getPricePerKg());
        updates.put("totalAmount", revenue.getTotalAmount());
        updates.put("buyer", revenue.getBuyer());
        updates.put("notes", revenue.getNotes());
        updates.put("updatedAt", System.currentTimeMillis());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_REVENUES)
                .document(String.valueOf(revenue.getId()))
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Revenue updated: " + revenue.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating revenue", e));
    }

    public Task<Void> deleteRevenue(long revenueId) {
        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_REVENUES)
                .document(String.valueOf(revenueId))
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Revenue deleted: " + revenueId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error deleting revenue", e));
    }

    // ========================================
    // TASKS
    // ========================================

    public Task<Void> uploadTask(TaskEntity task) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", task.getId());
        data.put("farmId", farmId);
        data.put("blockId", task.getBlockId());
        data.put("title", task.getTitle());
        data.put("description", task.getDescription());
        data.put("status", task.getStatus().name());
        data.put("type", task.getType().name());
        data.put("dueDate", task.getDueDate());
        data.put("completedDate", task.getCompletedDate());
        data.put("createdAt", task.getCreatedAt());
        data.put("updatedAt", System.currentTimeMillis());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_TASKS)
                .document(String.valueOf(task.getId()))
                .set(data)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Task uploaded with ID: " + task.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error uploading task", e));
    }

    public Task<Void> updateTask(TaskEntity task) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", task.getStatus().name());
        updates.put("title", task.getTitle());
        updates.put("description", task.getDescription());
        updates.put("dueDate", task.getDueDate());
        updates.put("completedDate", task.getCompletedDate());
        updates.put("updatedAt", System.currentTimeMillis());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_TASKS)
                .document(String.valueOf(task.getId()))
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Task updated: " + task.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating task", e));
    }

    public Task<Void> deleteTask(long taskId) {
        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_TASKS)
                .document(String.valueOf(taskId))
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Task deleted: " + taskId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error deleting task", e));
    }

    public StorageReference getStorageReference() {
        return FirebaseStorage.getInstance().getReference();
    }

    public com.google.android.gms.tasks.Task<Void> savePhotoMetadata(PhotoEntity photo, String downloadUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", photo.getId());
        data.put("farmId", farmId);
        data.put("blockId", photo.getBlockId());
        data.put("taskId", photo.getTaskId());
        data.put("expenseId", photo.getExpenseId());
        data.put("filePath", downloadUrl);
        data.put("caption", photo.getCaption());
        data.put("takenAt", photo.getTakenAt());
        data.put("latitude", photo.getLatitude());
        data.put("longitude", photo.getLongitude());
        data.put("photoType", photo.getPhotoType());
        data.put("createdAt", photo.getCreatedAt());
        data.put("updatedAt", System.currentTimeMillis());

        // Determine the correct Firestore path
        String collectionPath;
        String docId = String.valueOf(photo.getId());

        if (photo.getBlockId() != null && photo.getBlockId() > 0) {
            // Photo belongs to a block - store under farms/{farmId}/blocks/{blockId}/photos/{photoId}
            return db.collection(COLLECTION_FARMS)
                    .document(String.valueOf(farmId))
                    .collection("blocks")
                    .document(String.valueOf(photo.getBlockId()))
                    .collection("photos")
                    .document(docId)
                    .set(data);
        } else if (photo.getTaskId() != null && photo.getTaskId() > 0) {
            // Photo belongs to a task
            return db.collection(COLLECTION_FARMS)
                    .document(String.valueOf(farmId))
                    .collection("tasks")
                    .document(String.valueOf(photo.getTaskId()))
                    .collection("photos")
                    .document(docId)
                    .set(data);
        } else if (photo.getExpenseId() != null && photo.getExpenseId() > 0) {
            // Photo belongs to an expense
            return db.collection(COLLECTION_FARMS)
                    .document(String.valueOf(farmId))
                    .collection("expenses")
                    .document(String.valueOf(photo.getExpenseId()))
                    .collection("photos")
                    .document(docId)
                    .set(data);
        } else {
            // General farm photos
            return db.collection(COLLECTION_FARMS)
                    .document(String.valueOf(farmId))
                    .collection("photos")
                    .document(docId)
                    .set(data);
        }
    }

    public com.google.android.gms.tasks.Task<Void> updatePhotoMetadata(PhotoEntity photo) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("caption", photo.getCaption());
        updates.put("updatedAt", System.currentTimeMillis());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_PHOTOS)
                .document(String.valueOf(photo.getId()))
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Photo metadata updated: " + photo.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating photo metadata", e));
    }

    public com.google.android.gms.tasks.Task<Void> deletePhotoMetadata(long photoId) {
        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))
                .collection(SUBCOLLECTION_PHOTOS)
                .document(String.valueOf(photoId))
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Photo metadata deleted: " + photoId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error deleting photo metadata", e));
    }

    public com.google.android.gms.tasks.Task<Void> deleteFromStorage(String storagePath) {
        return FirebaseStorage.getInstance().getReference()
                .child(storagePath)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "File deleted from storage: " + storagePath))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error deleting file from storage", e));
    }

    public Task<Void> uploadAssignment(TaskAssignmentHistoryEntity assignment) {
        Map<String, Object> data = EntityMapper.assignmentToMap(assignment);

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))   // <--- convert long to String
                .collection(SUBCOLLECTION_TASK_ASSIGNMENTS)
                .document(String.valueOf(assignment.getId()))
                .set(data)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Assignment uploaded: " + assignment.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error uploading assignment", e));
    }

    public Task<Void> updateAssignment(TaskAssignmentHistoryEntity assignment) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("unassignedAt", assignment.getUnassignedAt());

        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))   // <--- convert long to String
                .collection(SUBCOLLECTION_TASK_ASSIGNMENTS)
                .document(String.valueOf(assignment.getId()))
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Assignment updated: " + assignment.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating assignment", e));
    }

    public Task<Void> deleteAssignment(long assignmentId) {
        return db.collection(COLLECTION_FARMS)
                .document(String.valueOf(farmId))   // <--- convert long to String
                .collection(SUBCOLLECTION_TASK_ASSIGNMENTS)
                .document(String.valueOf(assignmentId))
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Assignment deleted: " + assignmentId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error deleting assignment", e));
    }
}