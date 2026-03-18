package com.afrinuts.farmos.data.remote;

import android.util.Log;

import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class EntityMapper {
    private static final String TAG = "EntityMapper";

    // ========================================
    // SAFE HELPERS
    // ========================================

    private static long getLongSafe(DocumentSnapshot doc, String field) {
        Long value = doc.getLong(field);
        return value != null ? value : 0;
    }

    private static Long getNullableLong(DocumentSnapshot doc, String field) {
        return doc.contains(field) ? doc.getLong(field) : null;
    }

    private static double getDoubleSafe(DocumentSnapshot doc, String field) {
        Double value = doc.getDouble(field);
        return value != null ? value : 0;
    }

    private static String getStringSafe(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value : "";
    }

    private static boolean getBooleanSafe(DocumentSnapshot doc, String field) {
        Boolean value = doc.getBoolean(field);
        return value != null ? value : false;
    }

    // ========================================
    // USER MAPPING
    // ========================================

    public static Map<String, Object> userToMap(UserEntity user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("role", user.getRole().name());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("authMethod", user.getAuthMethod().name());
        map.put("isActive", user.isActive());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        // farmId is added at the FirestoreHelper level, not here
        return map;
    }

    public static UserEntity mapToUser(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            String name = getStringSafe(doc, "name");
            String roleStr = getStringSafe(doc, "role");
            String phone = getStringSafe(doc, "phone");
            String email = getStringSafe(doc, "email");
            String authMethodStr = getStringSafe(doc, "authMethod");
            boolean isActive = getBooleanSafe(doc, "isActive");
            long createdAt = getLongSafe(doc, "createdAt");

            UserEntity user = new UserEntity(
                    name,
                    UserEntity.UserRole.valueOf(roleStr),
                    phone,
                    email,
                    ""
            );
            user.setId(id);
            user.setAuthMethod(UserEntity.AuthMethod.valueOf(authMethodStr));
            user.setActive(isActive);
            user.setCreatedAt(createdAt);
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping user document", e);
            return null;
        }
    }

    // ========================================
    // BLOCK MAPPING
    // ========================================

    public static Map<String, Object> blockToMap(BlockEntity block) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", block.getId());
        map.put("farmId", block.getFarmId());
        map.put("blockName", block.getBlockName());
        map.put("hectareSize", block.getHectareSize());
        map.put("status", block.getStatus().name());
        map.put("clearedDate", block.getClearedDate());
        map.put("plowedDate", block.getPlowedDate());
        map.put("plantedDate", block.getPlantedDate());
        map.put("survivalRate", block.getSurvivalRate());
        map.put("replacementCount", block.getReplacementCount());
        map.put("notes", block.getNotes());
        map.put("createdAt", block.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static BlockEntity mapToBlock(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            long farmId = getLongSafe(doc, "farmId");
            String blockName = getStringSafe(doc, "blockName");
            double hectareSize = getDoubleSafe(doc, "hectareSize");
            String notes = doc.getString("notes");

            BlockEntity block = new BlockEntity(farmId, blockName, hectareSize, notes);
            block.setId(id);

            try {
                block.setStatus(BlockEntity.BlockStatus.valueOf(getStringSafe(doc, "status")));
            } catch (Exception e) {
                Log.e(TAG, "Invalid status");
            }

            block.setClearedDate(getLongSafe(doc, "clearedDate"));
            block.setPlowedDate(getLongSafe(doc, "plowedDate"));
            block.setPlantedDate(getLongSafe(doc, "plantedDate"));
            block.setSurvivalRate(getDoubleSafe(doc, "survivalRate"));
            block.setReplacementCount((int) getLongSafe(doc, "replacementCount"));
            block.setCreatedAt(getLongSafe(doc, "createdAt"));
            block.setUpdatedAt(getLongSafe(doc, "updatedAt"));
            return block;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping block document", e);
            return null;
        }
    }

    // ========================================
    // EXPENSE MAPPING
    // ========================================

    public static Map<String, Object> expenseToMap(ExpenseEntity expense) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", expense.getId());
        map.put("farmId", expense.getFarmId());
        map.put("blockId", expense.getBlockId());
        map.put("category", expense.getCategory().name());
        map.put("amount", expense.getAmount());
        map.put("date", expense.getDate());
        map.put("description", expense.getDescription());
        map.put("receiptPath", expense.getReceiptPath());
        map.put("createdAt", expense.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static ExpenseEntity mapToExpense(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            long farmId = getLongSafe(doc, "farmId");
            Long blockId = getNullableLong(doc, "blockId");
            String categoryStr = getStringSafe(doc, "category");
            double amount = getDoubleSafe(doc, "amount");
            long date = getLongSafe(doc, "date");
            String description = getStringSafe(doc, "description");
            String receiptPath = doc.getString("receiptPath");
            long createdAt = getLongSafe(doc, "createdAt");

            ExpenseEntity expense = new ExpenseEntity(
                    farmId,
                    blockId,
                    ExpenseEntity.ExpenseCategory.valueOf(categoryStr),
                    amount,
                    date,
                    description
            );
            expense.setId(id);
            expense.setReceiptPath(receiptPath);
            expense.setCreatedAt(createdAt);
            return expense;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping expense document", e);
            return null;
        }
    }

    // ========================================
    // REVENUE MAPPING
    // ========================================

    public static Map<String, Object> revenueToMap(RevenueEntity revenue) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", revenue.getId());
        map.put("farmId", revenue.getFarmId());
        map.put("blockId", revenue.getBlockId());
        map.put("harvestDate", revenue.getHarvestDate());
        map.put("quantityKg", revenue.getQuantityKg());
        map.put("pricePerKg", revenue.getPricePerKg());
        map.put("totalAmount", revenue.getTotalAmount());
        map.put("buyer", revenue.getBuyer());
        map.put("quality", revenue.getQuality().name());
        map.put("notes", revenue.getNotes());
        map.put("createdAt", revenue.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static RevenueEntity mapToRevenue(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            long farmId = getLongSafe(doc, "farmId");
            Long blockId = getNullableLong(doc, "blockId");
            long harvestDate = getLongSafe(doc, "harvestDate");
            double quantityKg = getDoubleSafe(doc, "quantityKg");
            double pricePerKg = getDoubleSafe(doc, "pricePerKg");
            String buyer = getStringSafe(doc, "buyer");
            String qualityStr = getStringSafe(doc, "quality");
            String notes = getStringSafe(doc, "notes");
            long createdAt = getLongSafe(doc, "createdAt");

            RevenueEntity revenue = new RevenueEntity(
                    farmId,
                    blockId,
                    harvestDate,
                    quantityKg,
                    pricePerKg,
                    buyer,
                    RevenueEntity.QualityGrade.valueOf(qualityStr),
                    notes
            );
            revenue.setId(id);
            revenue.setCreatedAt(createdAt);
            return revenue;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping revenue document", e);
            return null;
        }
    }

    // ========================================
    // TASK MAPPING
    // ========================================

    public static Map<String, Object> taskToMap(TaskEntity task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("farmId", task.getFarmId());
        map.put("blockId", task.getBlockId());
        map.put("title", task.getTitle());
        map.put("description", task.getDescription());
        map.put("status", task.getStatus().name());
        map.put("type", task.getType().name());
        map.put("dueDate", task.getDueDate());
        map.put("completedDate", task.getCompletedDate());
        map.put("createdAt", task.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static TaskEntity mapToTask(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            long farmId = getLongSafe(doc, "farmId");
            Long blockId = getNullableLong(doc, "blockId");
            String title = getStringSafe(doc, "title");
            String description = getStringSafe(doc, "description");
            String statusStr = getStringSafe(doc, "status");
            String typeStr = getStringSafe(doc, "type");
            long dueDate = getLongSafe(doc, "dueDate");
            Long completedDate = getNullableLong(doc, "completedDate");
            long createdAt = getLongSafe(doc, "createdAt");

            TaskEntity task = new TaskEntity(
                    farmId,
                    blockId,
                    title,
                    description,
                    TaskEntity.TaskType.valueOf(typeStr),
                    TaskEntity.TaskStatus.valueOf(statusStr),
                    dueDate
            );
            task.setId(id);
            task.setCompletedDate(completedDate);
            task.setCreatedAt(createdAt);
            return task;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping task document", e);
            return null;
        }
    }

    // ========================================
// TASK ASSIGNMENT HISTORY MAPPING
// ========================================

    public static Map<String, Object> assignmentToMap(TaskAssignmentHistoryEntity assignment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", assignment.getId());
        map.put("taskId", assignment.getTaskId());
        map.put("assignedTo", assignment.getAssignedTo());
        map.put("assignedAt", assignment.getAssignedAt());
        map.put("unassignedAt", assignment.getUnassignedAt());
        map.put("farmId", assignment.getFarmId());
        return map;
    }

    public static TaskAssignmentHistoryEntity mapToAssignment(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            long taskId = getLongSafe(doc, "taskId");
            String assignedTo = getStringSafe(doc, "assignedTo");
            long assignedAt = getLongSafe(doc, "assignedAt");
            Long unassignedAt = getNullableLong(doc, "unassignedAt");
            long farmId = getLongSafe(doc, "farmId");

            // ✅ Use the correct constructor with 4 parameters (farmId, taskId, assignedTo, assignedAt)
            TaskAssignmentHistoryEntity assignment = new TaskAssignmentHistoryEntity(
                    farmId,
                    taskId,
                    assignedTo,
                    assignedAt
            );
            assignment.setId(id);
            assignment.setUnassignedAt(unassignedAt);

            return assignment;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping assignment document", e);
            return null;
        }
    }

    // ========================================
    // PHOTO MAPPING
    // ========================================

    public static Map<String, Object> photoToMap(PhotoEntity photo) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", photo.getId());
        map.put("farmId", photo.getFarmId());
        map.put("blockId", photo.getBlockId());
        map.put("taskId", photo.getTaskId());
        map.put("expenseId", photo.getExpenseId());
        map.put("filePath", photo.getFilePath());
        map.put("caption", photo.getCaption());
        map.put("takenAt", photo.getTakenAt());
        map.put("latitude", photo.getLatitude());
        map.put("longitude", photo.getLongitude());
        map.put("photoType", photo.getPhotoType());
        map.put("createdAt", photo.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static PhotoEntity mapToPhoto(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            long farmId = getLongSafe(doc, "farmId");
            Long blockId = getNullableLong(doc, "blockId");
            Long taskId = getNullableLong(doc, "taskId");
            Long expenseId = getNullableLong(doc, "expenseId");
            String filePath = getStringSafe(doc, "filePath");
            String caption = getStringSafe(doc, "caption");
            long takenAt = getLongSafe(doc, "takenAt");
            double latitude = getDoubleSafe(doc, "latitude");
            double longitude = getDoubleSafe(doc, "longitude");
            String photoType = getStringSafe(doc, "photoType");
            long createdAt = getLongSafe(doc, "createdAt");

            PhotoEntity photo = new PhotoEntity(
                    farmId, blockId, taskId, expenseId,
                    filePath, caption, takenAt
            );
            photo.setId(id);
            photo.setLatitude(latitude);
            photo.setLongitude(longitude);
            photo.setPhotoType(photoType);
            photo.setCreatedAt(createdAt);
            return photo;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping photo document", e);
            return null;
        }
    }
}