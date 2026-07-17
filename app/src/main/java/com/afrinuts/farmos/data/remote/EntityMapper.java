package com.afrinuts.farmos.data.remote;

import static com.afrinuts.farmos.data.remote.FirebaseManager.COLLECTION_FARMS;

import android.util.Log;

import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class EntityMapper {
    private static final String TAG = "EntityMapper";

    // ========================================
    // SAFE HELPERS
    // ========================================

    private static Long getLongSafe(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);

        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                Log.e("EntityMapper", "Invalid number format for field: " + field);
                return null;
            }
        }

        return null;
    }

    private static Long getNullableLong(DocumentSnapshot doc, String field) {
        if (!doc.contains(field)) return null;
        return getLongSafe(doc, field);
    }

    private static double getDoubleSafe(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static String getStringSafe(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value : "";
    }

    private static boolean getBooleanSafe(DocumentSnapshot doc, String field) {
        Boolean value = doc.getBoolean(field);
        return value != null ? value : false;
    }

    private static long getFarmId(DocumentSnapshot doc) {
        // Try to get as Long first
        Long farmIdLong = getNullableLong(doc, "farmId");
        if (farmIdLong != null) {
            return farmIdLong;
        }

        // Try to get as String and parse
        String farmIdStr = getStringSafe(doc, "farmId");
        if (!farmIdStr.isEmpty()) {
            try {
                return Long.parseLong(farmIdStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Cannot parse farmId: " + farmIdStr);
            }
        }

        // Default to 1 (your default farm)
        Log.w(TAG, "farmId not found, defaulting to 1");
        return 1;
    }

    // ========================================
// USER MAPPING
// ========================================

    public static Map<String, Object> userToMap(UserEntity user) {
        Map<String, Object> map = new HashMap<>();

        // Core fields
        map.put("uid", user.getUid());
        map.put("localId", user.getLocalId());
        map.put("name", user.getName());
        map.put("role", user.getRole().name());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("authMethod", user.getAuthMethod().name());
        map.put("isActive", user.isActive());
        map.put("farmId", user.getFarmId());

        // ✅ Security & Authentication fields
        map.put("lastLoginAt", user.getLastLoginAt());
        map.put("lastLoginIp", user.getLastLoginIp());
        map.put("lastLoginDevice", user.getLastLoginDevice());
        map.put("emailVerified", user.isEmailVerified());
        map.put("passwordChangedAt", user.getPasswordChangedAt());
        map.put("failedLoginAttempts", user.getFailedLoginAttempts());
        map.put("lockedUntil", user.getLockedUntil());
        map.put("twoFactorEnabled", user.isTwoFactorEnabled());
        map.put("twoFactorSecret", user.getTwoFactorSecret());

        // ✅ Account Status fields
        map.put("lastUpdatedAt", user.getLastUpdatedAt());
        map.put("deactivatedAt", user.getDeactivatedAt());
        map.put("deactivatedBy", user.getDeactivatedBy());
        map.put("deactivationReason", user.getDeactivationReason());

        // ✅ Profile fields
        map.put("bio", user.getBio());
        map.put("company", user.getCompany());
        map.put("title", user.getTitle());
        map.put("timezone", user.getTimezone());

        // Audit timestamps
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());

        return map;
    }

    public static UserEntity mapToUser(DocumentSnapshot doc) {
        try {
            String uid = doc.getId();  // Firebase Auth UID is the document ID

            // Core fields
            String name = getStringSafe(doc, "name");
            String roleStr = getStringSafe(doc, "role");
            String phone = getStringSafe(doc, "phone");
            String email = getStringSafe(doc, "email");
            String authMethodStr = getStringSafe(doc, "authMethod");
            boolean isActive = getBooleanSafe(doc, "isActive");
            long createdAt = getLongSafe(doc, "createdAt");
            long farmId = getFarmId(doc);

            // Get localId if exists (for backward compatibility)
            Long localId = getNullableLong(doc, "localId");

            // Create user with core fields
            UserEntity user = new UserEntity(
                    uid,
                    name,
                    UserEntity.UserRole.valueOf(roleStr),
                    phone,
                    email,
                    "", // password hash not needed when reading
                    farmId
            );

            if (localId != null) {
                user.setLocalId(localId);
            }

            // ✅ Security & Authentication fields
            user.setAuthMethod(UserEntity.AuthMethod.valueOf(authMethodStr));
            user.setActive(isActive);
            user.setCreatedAt(createdAt);

            // Last login info
            Long lastLoginAt = getNullableLong(doc, "lastLoginAt");
            if (lastLoginAt != null) user.setLastLoginAt(lastLoginAt);

            String lastLoginIp = doc.getString("lastLoginIp");
            if (lastLoginIp != null) user.setLastLoginIp(lastLoginIp);

            String lastLoginDevice = doc.getString("lastLoginDevice");
            if (lastLoginDevice != null) user.setLastLoginDevice(lastLoginDevice);

            // Email verification
            Boolean emailVerified = doc.getBoolean("emailVerified");
            if (emailVerified != null) user.setEmailVerified(emailVerified);

            // Password change tracking
            Long passwordChangedAt = getNullableLong(doc, "passwordChangedAt");
            if (passwordChangedAt != null) user.setPasswordChangedAt(passwordChangedAt);

            // Security settings
            Integer failedLoginAttempts = getIntSafe(doc, "failedLoginAttempts", 0);
            user.setFailedLoginAttempts(failedLoginAttempts);

            Long lockedUntil = getNullableLong(doc, "lockedUntil");
            if (lockedUntil != null) user.setLockedUntil(lockedUntil);

            Boolean twoFactorEnabled = doc.getBoolean("twoFactorEnabled");
            if (twoFactorEnabled != null) user.setTwoFactorEnabled(twoFactorEnabled);

            String twoFactorSecret = doc.getString("twoFactorSecret");
            if (twoFactorSecret != null) user.setTwoFactorSecret(twoFactorSecret);

            // ✅ Account Status fields
            Long lastUpdatedAt = getNullableLong(doc, "lastUpdatedAt");
            if (lastUpdatedAt != null) {
                user.setLastUpdatedAt(lastUpdatedAt);
            } else {
                user.setLastUpdatedAt(createdAt);
            }

            Long deactivatedAt = getNullableLong(doc, "deactivatedAt");
            if (deactivatedAt != null) user.setDeactivatedAt(deactivatedAt);

            String deactivatedBy = doc.getString("deactivatedBy");
            if (deactivatedBy != null) user.setDeactivatedBy(deactivatedBy);

            String deactivationReason = doc.getString("deactivationReason");
            if (deactivationReason != null) user.setDeactivationReason(deactivationReason);

            // ✅ Profile fields
            String bio = doc.getString("bio");
            if (bio != null) user.setBio(bio);

            String company = doc.getString("company");
            if (company != null) user.setCompany(company);

            String title = doc.getString("title");
            if (title != null) user.setTitle(title);

            String timezone = doc.getString("timezone");
            if (timezone != null) {
                user.setTimezone(timezone);
            } else {
                user.setTimezone("UTC"); // Default timezone
            }

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
        // ✅ Add harvest fields
        map.put("harvestHistory", block.getHarvestHistory());
        map.put("lastHarvestDate", block.getLastHarvestDate());
        map.put("lastHarvestQuantity", block.getLastHarvestQuantity());
        map.put("totalHarvestQuantity", block.getTotalHarvestQuantity());
        map.put("harvestCount", block.getHarvestCount());
        map.put("createdAt", block.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static BlockEntity mapToBlock(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());

            // Get farmId
            long farmId = 1L;
            Long farmIdLong = getNullableLong(doc, "farmId");
            if (farmIdLong != null) {
                farmId = farmIdLong;
            } else {
                String farmIdStr = getStringSafe(doc, "farmId");
                if (!farmIdStr.isEmpty()) {
                    try {
                        farmId = Long.parseLong(farmIdStr);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Invalid farmId format, using default: " + farmIdStr);
                    }
                }
            }

            String blockName = getStringSafe(doc, "blockName");
            if (blockName.isEmpty()) {
                Log.e(TAG, "Missing blockName for block: " + id);
                return null;
            }

            double hectareSize = getDoubleSafe(doc, "hectareSize");
            String notes = doc.getString("notes");

            // ✅ Create block with proper constructor
            BlockEntity block = new BlockEntity(farmId, blockName, hectareSize, notes);
            block.setId(id);

            // Set status
            try {
                String status = getStringSafe(doc, "status");
                if (!status.isEmpty()) {
                    block.setStatus(BlockEntity.BlockStatus.valueOf(status));
                }
            } catch (Exception e) {
                Log.e(TAG, "Invalid status for block: " + id, e);
            }

            // Set dates
            block.setClearedDate(getNullableLong(doc, "clearedDate"));
            block.setPlowedDate(getNullableLong(doc, "plowedDate"));
            block.setPlantedDate(getNullableLong(doc, "plantedDate"));
            block.setSurvivalRate(getDoubleSafe(doc, "survivalRate"));

            Long replacementCountValue = getNullableLong(doc, "replacementCount");
            block.setReplacementCount(replacementCountValue != null ? replacementCountValue.intValue() : 0);

            // ✅ Parse harvest fields - FIX: Use proper parsing
            // Get harvestHistory as string
            String harvestHistory = doc.getString("harvestHistory");
            if (harvestHistory != null && !harvestHistory.isEmpty()) {
                block.setHarvestHistory(harvestHistory);
            } else {
                // Initialize empty if null
                block.setHarvestHistory(new Gson().toJson(new ArrayList<>()));
            }

            // ✅ Parse lastHarvestDate - handle both Long and String
            Object lastHarvestDateObj = doc.get("lastHarvestDate");
            if (lastHarvestDateObj != null) {
                if (lastHarvestDateObj instanceof Long) {
                    block.setLastHarvestDate((Long) lastHarvestDateObj);
                } else if (lastHarvestDateObj instanceof String) {
                    try {
                        block.setLastHarvestDate(Long.parseLong((String) lastHarvestDateObj));
                    } catch (NumberFormatException e) {
                        block.setLastHarvestDate(null);
                    }
                }
            }

            // ✅ Parse lastHarvestQuantity
            Object lastHarvestQtyObj = doc.get("lastHarvestQuantity");
            if (lastHarvestQtyObj != null) {
                if (lastHarvestQtyObj instanceof Double) {
                    block.setLastHarvestQuantity((Double) lastHarvestQtyObj);
                } else if (lastHarvestQtyObj instanceof Long) {
                    block.setLastHarvestQuantity(((Long) lastHarvestQtyObj).doubleValue());
                } else if (lastHarvestQtyObj instanceof String) {
                    try {
                        block.setLastHarvestQuantity(Double.parseDouble((String) lastHarvestQtyObj));
                    } catch (NumberFormatException e) {
                        block.setLastHarvestQuantity(0);
                    }
                }
            }

            // ✅ Parse totalHarvestQuantity
            Object totalHarvestQtyObj = doc.get("totalHarvestQuantity");
            if (totalHarvestQtyObj != null) {
                if (totalHarvestQtyObj instanceof Double) {
                    block.setTotalHarvestQuantity((Double) totalHarvestQtyObj);
                } else if (totalHarvestQtyObj instanceof Long) {
                    block.setTotalHarvestQuantity(((Long) totalHarvestQtyObj).doubleValue());
                } else if (totalHarvestQtyObj instanceof String) {
                    try {
                        block.setTotalHarvestQuantity(Double.parseDouble((String) totalHarvestQtyObj));
                    } catch (NumberFormatException e) {
                        block.setTotalHarvestQuantity(0);
                    }
                }
            }

            // ✅ Parse harvestCount
            Object harvestCountObj = doc.get("harvestCount");
            if (harvestCountObj != null) {
                if (harvestCountObj instanceof Long) {
                    block.setHarvestCount(((Long) harvestCountObj).intValue());
                } else if (harvestCountObj instanceof Integer) {
                    block.setHarvestCount((Integer) harvestCountObj);
                } else if (harvestCountObj instanceof String) {
                    try {
                        block.setHarvestCount(Integer.parseInt((String) harvestCountObj));
                    } catch (NumberFormatException e) {
                        block.setHarvestCount(0);
                    }
                }
            }

            block.setCreatedAt(getLongSafe(doc, "createdAt"));
            block.setUpdatedAt(getLongSafe(doc, "updatedAt"));

            return block;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping block document", e);
            return null;
        }
    }

    public static int getIntSafe(DocumentSnapshot doc, String field, int defaultValue) {
        try {
            Object value = doc.get(field);
            if (value == null) return defaultValue;
            if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
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

        // ✅ Add new fields for procurement tracking
        map.put("batchId", expense.getBatchId());
        map.put("supplierId", expense.getSupplierId());
        map.put("batchNumber", expense.getBatchNumber());
        map.put("supplierName", expense.getSupplierName());

        map.put("category", expense.getCategory().name());
        map.put("amount", expense.getAmount());
        map.put("date", expense.getDate());
        map.put("description", expense.getDescription());
        map.put("receiptPath", expense.getReceiptPath());
        map.put("firestoreId", expense.getFirestoreId());
        map.put("createdAt", expense.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static ExpenseEntity mapToExpense(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            long farmId = getLongSafe(doc, "farmId");
            Long blockId = getNullableLong(doc, "blockId");

            // ✅ Get new fields for procurement tracking
            Long batchId = getNullableLong(doc, "batchId");
            Long supplierId = getNullableLong(doc, "supplierId");
            String batchNumber = doc.getString("batchNumber");
            String supplierName = doc.getString("supplierName");

            String categoryStr = getStringSafe(doc, "category");
            double amount = getDoubleSafe(doc, "amount");
            long date = getLongSafe(doc, "date");
            String description = getStringSafe(doc, "description");
            String receiptPath = doc.getString("receiptPath");
            String firestoreId = doc.getString("firestoreId");
            long createdAt = getLongSafe(doc, "createdAt");
            long updatedAt = getLongSafe(doc, "updatedAt");

            ExpenseEntity expense = new ExpenseEntity(
                    farmId,
                    blockId,
                    batchId,
                    supplierId,
                    ExpenseEntity.ExpenseCategory.valueOf(categoryStr),
                    amount,
                    date,
                    description
            );
            expense.setId(id);
            expense.setReceiptPath(receiptPath);
            expense.setFirestoreId(firestoreId);
            expense.setCreatedAt(createdAt);
            expense.setUpdatedAt(updatedAt > 0 ? updatedAt : createdAt);

            // ✅ Set denormalized fields
            expense.setBatchNumber(batchNumber);
            expense.setSupplierName(supplierName);

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

        // ✅ Add new fields for procurement tracking
        map.put("batchId", revenue.getBatchId());
        map.put("supplierId", revenue.getSupplierId());
        map.put("batchNumber", revenue.getBatchNumber());
        map.put("supplierName", revenue.getSupplierName());

        map.put("harvestDate", revenue.getHarvestDate());
        map.put("quantityKg", revenue.getQuantityKg());
        map.put("pricePerKg", revenue.getPricePerKg());
        map.put("totalAmount", revenue.getTotalAmount());
        map.put("buyer", revenue.getBuyer());
        map.put("quality", revenue.getQuality().name());
        map.put("notes", revenue.getNotes());
        map.put("createdAt", revenue.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        map.put("harvestCycleId", revenue.getHarvestCycleId());
        map.put("harvestYear", revenue.getHarvestYear());
        map.put("harvestSeason", revenue.getHarvestSeason());
        return map;
    }

    public static RevenueEntity mapToRevenue(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            long farmId = getLongSafe(doc, "farmId");
            Long blockId = getNullableLong(doc, "blockId");

            // ✅ Get new fields for procurement tracking
            Long batchId = getNullableLong(doc, "batchId");
            Long supplierId = getNullableLong(doc, "supplierId");
            String batchNumber = doc.getString("batchNumber");
            String supplierName = doc.getString("supplierName");

            long harvestDate = getLongSafe(doc, "harvestDate");
            double quantityKg = getDoubleSafe(doc, "quantityKg");
            double pricePerKg = getDoubleSafe(doc, "pricePerKg");
            String buyer = getStringSafe(doc, "buyer");
            String qualityStr = getStringSafe(doc, "quality");
            String notes = getStringSafe(doc, "notes");
            long createdAt = getLongSafe(doc, "createdAt");
            long updatedAt = getLongSafe(doc, "updatedAt");
            String harvestCycleId = doc.getString("harvestCycleId");

            // Handle harvestYear - could be stored as Long or Integer in Firestore
            Object harvestYearObj = doc.get("harvestYear");
            int harvestYear = 0;
            if (harvestYearObj instanceof Long) {
                harvestYear = ((Long) harvestYearObj).intValue();
            } else if (harvestYearObj instanceof Integer) {
                harvestYear = (Integer) harvestYearObj;
            } else if (harvestYearObj instanceof String) {
                try {
                    harvestYear = Integer.parseInt((String) harvestYearObj);
                } catch (NumberFormatException e) {
                    harvestYear = 0;
                }
            }

            // Handle harvestSeason similarly
            Object harvestSeasonObj = doc.get("harvestSeason");
            int harvestSeason = 1;
            if (harvestSeasonObj instanceof Long) {
                harvestSeason = ((Long) harvestSeasonObj).intValue();
            } else if (harvestSeasonObj instanceof Integer) {
                harvestSeason = (Integer) harvestSeasonObj;
            } else if (harvestSeasonObj instanceof String) {
                try {
                    harvestSeason = Integer.parseInt((String) harvestSeasonObj);
                } catch (NumberFormatException e) {
                    harvestSeason = 1;
                }
            }

            RevenueEntity revenue = new RevenueEntity(
                    farmId,
                    blockId,
                    batchId,
                    supplierId,
                    harvestDate,
                    quantityKg,
                    pricePerKg,
                    buyer,
                    RevenueEntity.QualityGrade.valueOf(qualityStr),
                    notes
            );
            revenue.setId(id);
            revenue.setCreatedAt(createdAt);
            revenue.setUpdatedAt(updatedAt > 0 ? updatedAt : createdAt);
            revenue.setHarvestCycleId(harvestCycleId);
            revenue.setHarvestYear(harvestYear);
            revenue.setHarvestSeason(harvestSeason);

            // ✅ Set denormalized fields
            revenue.setBatchNumber(batchNumber);
            revenue.setSupplierName(supplierName);

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
            SupplierEntity.SupplierType type;
            try {
                type = SupplierEntity.SupplierType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                type = SupplierEntity.SupplierType.COOPERATIVE;
            }
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

    // ========================================
    // QUALITY BATCH MAPPING
    // ========================================

    public static QualityBatchEntity fromFirestoreQualityBatch(DocumentSnapshot doc) {
        return mapToQualityBatch(doc);
    }

    public static Map<String, Object> qualityBatchToMap(QualityBatchEntity batch) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", batch.getId());
        map.put("farmId", batch.getFarmId());
        map.put("blockId", batch.getBlockId());
        map.put("harvestCycleId", batch.getHarvestCycleId());
        map.put("verificationCode", batch.getVerificationCode());
        map.put("supplierId", batch.getSupplierId());
        map.put("sourceType", batch.getSourceType() != null ? batch.getSourceType().name() : null);
        map.put("batchNumber", batch.getBatchNumber());
        map.put("processingDate", batch.getProcessingDate());
        map.put("moistureContent", batch.getMoistureContent());
        map.put("nutCountPerKg", batch.getNutCountPerKg());
        map.put("defectiveRate", batch.getDefectiveRate());
        map.put("kernelRatio", batch.getKernelRatio());
        map.put("weightKg", batch.getWeightKg());
        map.put("qualityGrade", batch.getQualityGrade().name());
        map.put("testResults", batch.getTestResults());
        map.put("qualityScore", batch.getQualityScore());
        map.put("notes", batch.getNotes());
        map.put("createdAt", batch.getCreatedAt());
        map.put("updatedAt", batch.getUpdatedAt());
        map.put("isSynced", batch.isSynced());
        map.put("noChildLabor", batch.isNoChildLabor());
        map.put("farmOsMonitored", batch.isFarmOsMonitored());
        map.put("firebreakProtection", batch.isFirebreakProtection());
        map.put("sustainableHarvesting", batch.isSustainableHarvesting());
        map.put("verifiedAt", batch.getVerifiedAt());
        return map;
    }

    public static QualityBatchEntity mapToQualityBatch(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            long farmId = getFarmId(doc);
            Long blockId = getNullableLong(doc, "blockId");
            Long harvestCycleId = getNullableLong(doc, "harvestCycleId");
            Long supplierId = getNullableLong(doc, "supplierId");
            String sourceTypeStr = getStringSafe(doc, "sourceType");
            String batchNumber = getStringSafe(doc, "batchNumber");
            long processingDate = getLongSafe(doc, "processingDate");
            double moistureContent = getDoubleSafe(doc, "moistureContent");
            Long nutCountLong = getLongSafe(doc, "nutCountPerKg");
            int nutCountPerKg = nutCountLong != null ? nutCountLong.intValue() : 0;
            double defectiveRate = getDoubleSafe(doc, "defectiveRate");
            double kernelRatio = getDoubleSafe(doc, "kernelRatio");
            double weightKg = getDoubleSafe(doc, "weightKg");
            String gradeStr = getStringSafe(doc, "qualityGrade");
            String verificationCode = doc.getString("verificationCode");
            String testResults = doc.getString("testResults");
            double qualityScore = getDoubleSafe(doc, "qualityScore");
            String notes = doc.getString("notes");
            boolean noChildLabor = doc.getBoolean("noChildLabor") != null && doc.getBoolean("noChildLabor");
            boolean farmOsMonitored = doc.getBoolean("farmOsMonitored") != null && doc.getBoolean("farmOsMonitored");
            boolean firebreakProtection = doc.getBoolean("firebreakProtection") != null && doc.getBoolean("firebreakProtection");
            boolean sustainableHarvesting = doc.getBoolean("sustainableHarvesting") != null && doc.getBoolean("sustainableHarvesting");
            Long verifiedAtLong = getLongSafe(doc, "verifiedAt");
            long verifiedAt = verifiedAtLong != null ? verifiedAtLong : 0;
            long createdAt = getLongSafe(doc, "createdAt");
            long updatedAt = getLongSafe(doc, "updatedAt");
            boolean isSynced = getBooleanSafe(doc, "isSynced");

            // Parse source type
            QualityBatchEntity.SourceType sourceType = QualityBatchEntity.SourceType.OWN_FARM;
            if (sourceTypeStr != null && !sourceTypeStr.isEmpty()) {
                try {
                    sourceType = QualityBatchEntity.SourceType.valueOf(sourceTypeStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown source type: " + sourceTypeStr);
                }
            }

            // FIXED: Use the correct constructor with 14 arguments
            QualityBatchEntity batch = new QualityBatchEntity(
                    farmId,
                    blockId,
                    harvestCycleId,
                    supplierId,
                    sourceType,
                    batchNumber,
                    processingDate,
                    moistureContent,
                    nutCountPerKg,
                    defectiveRate,
                    kernelRatio,
                    weightKg,
                    QualityBatchEntity.QualityGrade.valueOf(gradeStr),
                    notes
            );
            batch.setId(id);
            batch.setTestResults(testResults);
            batch.setQualityScore(qualityScore);
            batch.setCreatedAt(createdAt);
            batch.setUpdatedAt(updatedAt);
            batch.setVerificationCode(verificationCode);
            batch.setSynced(isSynced);
            batch.setNoChildLabor(noChildLabor);
            batch.setFarmOsMonitored(farmOsMonitored);
            batch.setFirebreakProtection(firebreakProtection);
            batch.setSustainableHarvesting(sustainableHarvesting);
            batch.setVerifiedAt(verifiedAt);

            return batch;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping quality batch", e);
            return null;
        }
    }

    // ========================================
    // SUPPLIER MAPPING
    // ========================================

    public static SupplierEntity fromFirestoreSupplier(DocumentSnapshot doc) {
        return mapToSupplier(doc);
    }

    public static Map<String, Object> supplierToMap(SupplierEntity supplier) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", supplier.getId());
        map.put("farmId", supplier.getFarmId());
        map.put("name", supplier.getName());
        map.put("type", supplier.getType() != null ? supplier.getType().name() : null);
        map.put("contactPerson", supplier.getContactPerson());
        map.put("phone", supplier.getPhone());
        map.put("email", supplier.getEmail());
        map.put("location", supplier.getLocation());
        map.put("region", supplier.getRegion());
        map.put("country", supplier.getCountry());
        map.put("latitude", supplier.getLatitude());
        map.put("longitude", supplier.getLongitude());
        map.put("hasLocation", supplier.hasLocation());
        map.put("registrationNumber", supplier.getRegistrationNumber());
        map.put("taxId", supplier.getTaxId());
        map.put("annualVolumeKg", supplier.getAnnualVolumeKg());
        map.put("averageQualityScore", supplier.getAverageQualityScore());
        map.put("rating", supplier.getRating());
        map.put("certificationType", supplier.getCertificationType());
        map.put("certificationNumber", supplier.getCertificationNumber());
        map.put("certificationExpiryDate", supplier.getCertificationExpiryDate());
        map.put("isCertified", supplier.isCertified());
        map.put("status", supplier.getStatus() != null ? supplier.getStatus().name() : null);
        map.put("isActive", supplier.isActive());
        map.put("notes", supplier.getNotes());

        // ✅ ADD FARMER PROFILE FIELDS
        map.put("photoUrl", supplier.getPhotoUrl());
        map.put("yearsFarming", supplier.getYearsFarming());
        map.put("bio", supplier.getBio());
        map.put("quote", supplier.getQuote());
        map.put("farmSize", supplier.getFarmSize());
        map.put("firebreakProtection", supplier.isFirebreakProtection());
        map.put("sustainableHarvesting", supplier.isSustainableHarvesting());
        map.put("noChildLabor", supplier.isNoChildLabor());

        map.put("createdAt", supplier.getCreatedAt());
        map.put("updatedAt", supplier.getUpdatedAt());
        map.put("isSynced", supplier.isSynced());
        return map;
    }

    public static SupplierEntity mapToSupplier(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            long farmId = getFarmId(doc);
            String name = getStringSafe(doc, "name");
            String typeStr = getStringSafe(doc, "type");
            String contactPerson = getStringSafe(doc, "contactPerson");
            String phone = getStringSafe(doc, "phone");
            String email = getStringSafe(doc, "email");
            String location = getStringSafe(doc, "location");
            String region = getStringSafe(doc, "region");
            String country = getStringSafe(doc, "country");
            String registrationNumber = getStringSafe(doc, "registrationNumber");
            String taxId = getStringSafe(doc, "taxId");
            double annualVolumeKg = getDoubleSafe(doc, "annualVolumeKg");
            double averageQualityScore = getDoubleSafe(doc, "averageQualityScore");
            Long ratingLong = getLongSafe(doc, "rating");
            int rating = ratingLong != null ? ratingLong.intValue() : 3;
            String certificationType = getStringSafe(doc, "certificationType");
            String certificationNumber = getStringSafe(doc, "certificationNumber");
            long certificationExpiryDate = getLongSafe(doc, "certificationExpiryDate");
            boolean isCertified = getBooleanSafe(doc, "isCertified");
            String statusStr = getStringSafe(doc, "status");
            boolean isActive = getBooleanSafe(doc, "isActive");
            String notes = getStringSafe(doc, "notes");

            // ✅ ADD FARMER PROFILE FIELDS
            String photoUrl = doc.getString("photoUrl");
            Long yearsFarmingLong = getLongSafe(doc, "yearsFarming");
            int yearsFarming = yearsFarmingLong != null ? yearsFarmingLong.intValue() : 0;
            String bio = doc.getString("bio");
            String quote = doc.getString("quote");
            Double farmSize = doc.getDouble("farmSize");
            Boolean firebreakProtection = doc.getBoolean("firebreakProtection");
            Boolean sustainableHarvesting = doc.getBoolean("sustainableHarvesting");
            Boolean noChildLabor = doc.getBoolean("noChildLabor");

            long createdAt = getLongSafe(doc, "createdAt");
            long updatedAt = getLongSafe(doc, "updatedAt");
            boolean isSynced = getBooleanSafe(doc, "isSynced");

            SupplierEntity supplier = new SupplierEntity(
                    farmId, name, SupplierEntity.SupplierType.valueOf(typeStr),
                    contactPerson, phone, location, registrationNumber
            );
            supplier.setId(id);
            supplier.setEmail(email);
            supplier.setRegion(region);
            supplier.setCountry(country);
            supplier.setRegistrationNumber(registrationNumber);
            supplier.setTaxId(taxId);
            supplier.setAnnualVolumeKg(annualVolumeKg);
            supplier.setAverageQualityScore(averageQualityScore);
            supplier.setRating(rating);
            supplier.setCertificationType(certificationType);
            supplier.setCertificationNumber(certificationNumber);
            supplier.setCertificationExpiryDate(certificationExpiryDate);
            supplier.setCertified(isCertified);
            supplier.setStatus(SupplierEntity.SupplierStatus.valueOf(statusStr));
            supplier.setActive(isActive);
            supplier.setNotes(notes);

            // ✅ SET FARMER PROFILE FIELDS
            supplier.setPhotoUrl(photoUrl);
            supplier.setYearsFarming(yearsFarming);
            supplier.setBio(bio);
            supplier.setQuote(quote);
            supplier.setFarmSize(farmSize != null ? farmSize : 0);
            supplier.setFirebreakProtection(firebreakProtection != null && firebreakProtection);
            supplier.setSustainableHarvesting(sustainableHarvesting != null && sustainableHarvesting);
            supplier.setNoChildLabor(noChildLabor != null && noChildLabor);

            supplier.setCreatedAt(createdAt);
            supplier.setUpdatedAt(updatedAt);
            supplier.setSynced(isSynced);

            return supplier;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping supplier", e);
            return null;
        }
    }

// ========================================
// BATCH TRACEABILITY MAPPING
// ========================================

    public static Map<String, Object> batchTraceabilityToMap(BatchTraceabilityEntity traceability) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", traceability.getId());
        map.put("batchNumber", traceability.getBatchNumber());
        map.put("farmId", traceability.getFarmId());
        map.put("harvestCycleId", traceability.getHarvestCycleId());
        map.put("harvestYear", traceability.getHarvestYear());
        map.put("qualityBatchId", traceability.getQualityBatchId());
        map.put("supplierId", traceability.getSupplierId());
        map.put("supplierName", traceability.getSupplierName());
        map.put("qualityGrade", traceability.getQualityGrade());
        map.put("certificationIds", traceability.getCertificationIds());

        // ✅ Add verification code
        map.put("verificationCode", traceability.getVerificationCode());

        // ✅ Use sustainabilityMetrics instead of sustainabilityMetricsId
        map.put("sustainabilityMetrics", traceability.getSustainabilityMetrics());

        map.put("processingLocation", traceability.getProcessingLocation());
        map.put("processingMethod", traceability.getProcessingMethod());
        map.put("processingFacility", traceability.getProcessingFacility());
        map.put("rawWeightKg", traceability.getRawWeightKg());
        map.put("kernelWeightKg", traceability.getKernelWeightKg());
        map.put("yieldPercentage", traceability.getYieldPercentage());
        map.put("shellingMethod", traceability.getShellingMethod());
        map.put("gradingMethod", traceability.getGradingMethod());

        map.put("processingDate", traceability.getProcessingDate());
        map.put("exportDate", traceability.getExportDate());
        map.put("buyer", traceability.getBuyer());
        map.put("destination", traceability.getDestination());
        map.put("weightKg", traceability.getWeightKg());
        map.put("grade", traceability.getGrade());
        map.put("pricePerKg", traceability.getPricePerKg());
        map.put("totalAmount", traceability.getTotalAmount());
        map.put("documents", traceability.getDocuments());
        map.put("traceabilityUrl", traceability.getTraceabilityUrl());
        map.put("createdAt", traceability.getCreatedAt());
        map.put("updatedAt", traceability.getUpdatedAt());
        map.put("isSynced", traceability.isSynced());
        return map;
    }

    public static BatchTraceabilityEntity mapToBatchTraceability(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            long farmId = getLongSafe(doc, "farmId");
            String batchNumber = getStringSafe(doc, "batchNumber");
            Long harvestCycleId = getNullableLong(doc, "harvestCycleId");
            Long qualityBatchId = getNullableLong(doc, "qualityBatchId");
            Long supplierId = getNullableLong(doc, "supplierId");
            String supplierName = doc.getString("supplierName");
            String qualityGrade = doc.getString("qualityGrade");
            String certificationIds = doc.getString("certificationIds");

            // ✅ Read verification code
            String verificationCode = doc.getString("verificationCode");

            // ✅ Read sustainabilityMetrics instead of sustainabilityMetricsId
            String sustainabilityMetrics = doc.getString("sustainabilityMetrics");

            int harvestYear = 0;
            Object harvestYearObj = doc.get("harvestYear");
            if (harvestYearObj instanceof Long) {
                harvestYear = ((Long) harvestYearObj).intValue();
            } else if (harvestYearObj instanceof Integer) {
                harvestYear = (Integer) harvestYearObj;
            } else if (harvestYearObj instanceof String) {
                try {
                    harvestYear = Integer.parseInt((String) harvestYearObj);
                } catch (NumberFormatException e) {
                    harvestYear = 0;
                }
            }

            long processingDate = getLongSafe(doc, "processingDate");
            Long exportDate = getNullableLong(doc, "exportDate");
            String buyer = doc.getString("buyer");
            String destination = doc.getString("destination");
            double weightKg = getDoubleSafe(doc, "weightKg");
            String grade = getStringSafe(doc, "grade");
            double pricePerKg = getDoubleSafe(doc, "pricePerKg");
            double totalAmount = getDoubleSafe(doc, "totalAmount");
            String processingLocation = doc.getString("processingLocation");
            String processingMethod = doc.getString("processingMethod");
            String processingFacility = doc.getString("processingFacility");
            double rawWeightKg = getDoubleSafe(doc, "rawWeightKg");
            double kernelWeightKg = getDoubleSafe(doc, "kernelWeightKg");
            double yieldPercentage = getDoubleSafe(doc, "yieldPercentage");
            String shellingMethod = doc.getString("shellingMethod");
            String gradingMethod = doc.getString("gradingMethod");
            String documents = doc.getString("documents");
            String traceabilityUrl = doc.getString("traceabilityUrl");
            long createdAt = getLongSafe(doc, "createdAt");
            long updatedAt = getLongSafe(doc, "updatedAt");
            boolean isSynced = getBooleanSafe(doc, "isSynced");

            BatchTraceabilityEntity traceability = new BatchTraceabilityEntity(
                    farmId,
                    batchNumber,
                    processingDate,
                    weightKg,
                    grade,
                    pricePerKg
            );
            traceability.setId(id);
            traceability.setHarvestCycleId(harvestCycleId);
            traceability.setQualityBatchId(qualityBatchId);
            traceability.setSupplierId(supplierId);
            traceability.setHarvestYear(harvestYear);
            traceability.setSupplierName(supplierName);
            traceability.setQualityGrade(qualityGrade);
            traceability.setCertificationIds(certificationIds);

            // ✅ Set verification code
            traceability.setVerificationCode(verificationCode);

            // ✅ Set sustainabilityMetrics
            traceability.setSustainabilityMetrics(sustainabilityMetrics);

            traceability.setExportDate(exportDate);
            traceability.setBuyer(buyer);
            traceability.setDestination(destination);
            traceability.setTotalAmount(totalAmount);

            // ✅ Set new processing fields
            traceability.setProcessingLocation(processingLocation);
            traceability.setProcessingMethod(processingMethod);
            traceability.setProcessingFacility(processingFacility);
            traceability.setKernelWeightKg(kernelWeightKg);
            traceability.setRawWeightKg(rawWeightKg);
            traceability.setYieldPercentage(yieldPercentage);
            traceability.setShellingMethod(shellingMethod);
            traceability.setGradingMethod(gradingMethod);
            traceability.setDocuments(documents);
            traceability.setTraceabilityUrl(traceabilityUrl != null ? traceabilityUrl :
                    traceability.generateTraceabilityUrl(batchNumber));
            traceability.setCreatedAt(createdAt);
            traceability.setUpdatedAt(updatedAt > 0 ? updatedAt : createdAt);
            traceability.setSynced(isSynced);

            return traceability;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping batch traceability", e);
            return null;
        }
    }

    /**
     * Helper method to safely get int values from DocumentSnapshot
     * Handles Long, Integer, and String types
     */
    private static int getIntSafe(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // ========================================
// MASTER BATCH TRACEABILITY OPERATIONS
// ========================================

    // In EntityMapper.java - add this method

    public static Map<String, Object> masterTraceabilityToMap(MasterBatchTraceabilityEntity master) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", master.getId());
        map.put("masterBatchNumber", master.getMasterBatchNumber());
        map.put("shipmentName", master.getShipmentName());
        map.put("shipmentId", master.getShipmentId());
        map.put("totalWeightKg", master.getTotalWeightKg());
        map.put("totalBatches", master.getTotalBatches());
        map.put("grades", master.getGrades());
        map.put("certifications", master.getCertifications());
        map.put("batchNumbers", master.getBatchNumbersJson());
        map.put("batchTraceabilityIds", master.getBatchTraceabilityIdsJson());
        map.put("buyer", master.getBuyer());
        map.put("destination", master.getDestination());
        map.put("exportDate", master.getExportDate());
        map.put("traceabilityUrl", master.getTraceabilityUrl());
        map.put("createdAt", master.getCreatedAt());
        map.put("updatedAt", master.getUpdatedAt());
        map.put("isSynced", master.isSynced());
        return map;
    }

    // Also add mapToMasterTraceability for reading from Firestore
    public static MasterBatchTraceabilityEntity mapToMasterTraceability(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");
            String masterBatchNumber = doc.getString("masterBatchNumber");
            String shipmentName = doc.getString("shipmentName");

            MasterBatchTraceabilityEntity master = new MasterBatchTraceabilityEntity(masterBatchNumber, shipmentName);

            master.setId(id);
            master.setShipmentId(doc.getString("shipmentId"));

            Double totalWeight = doc.getDouble("totalWeightKg");
            if (totalWeight != null) master.setTotalWeightKg(totalWeight);

            String verificationCode = doc.getString("verificationCode");

            Long totalBatches = doc.getLong("totalBatches");
            if (totalBatches != null) master.setTotalBatches(totalBatches.intValue());

            master.setGrades(doc.getString("grades"));
            master.setCertifications(doc.getString("certifications"));
            master.setBatchNumbersJson(doc.getString("batchNumbers"));
            master.setBatchTraceabilityIdsJson(doc.getString("batchTraceabilityIds"));
            master.setBuyer(doc.getString("buyer"));
            master.setDestination(doc.getString("destination"));

            Long exportDate = doc.getLong("exportDate");
            if (exportDate != null) master.setExportDate(exportDate);

            master.setTraceabilityUrl(doc.getString("traceabilityUrl"));

            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null) master.setCreatedAt(createdAt);

            Long updatedAt = doc.getLong("updatedAt");
            if (updatedAt != null) master.setUpdatedAt(updatedAt);

            Boolean isSynced = doc.getBoolean("isSynced");
            if (isSynced != null) master.setSynced(isSynced);

            return master;

        } catch (Exception e) {
            Log.e(TAG, "Error mapping master traceability", e);
            return null;
        }
    }

    // ========================================
    // FIRESTORE TO ENTITY ALIASES (for listeners)
    // ========================================

    public static BlockEntity fromFirestoreBlock(DocumentSnapshot doc) {
        return mapToBlock(doc);
    }

    public static ExpenseEntity fromFirestoreExpense(DocumentSnapshot doc) {
        return mapToExpense(doc);
    }

    public static RevenueEntity fromFirestoreRevenue(DocumentSnapshot doc) {
        return mapToRevenue(doc);
    }

    public static TaskEntity fromFirestoreTask(DocumentSnapshot doc) {
        return mapToTask(doc);
    }

    public static PhotoEntity fromFirestorePhoto(DocumentSnapshot doc) {
        return mapToPhoto(doc);
    }

    public static UserEntity fromFirestoreUser(DocumentSnapshot doc) {
        return mapToUser(doc);
    }

    public static TaskAssignmentHistoryEntity fromFirestoreAssignment(DocumentSnapshot doc) {
        return mapToAssignment(doc);
    }
}