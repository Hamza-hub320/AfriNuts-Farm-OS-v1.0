package com.afrinuts.farmos.data.remote;

import static com.afrinuts.farmos.data.remote.FirebaseManager.COLLECTION_FARMS;

import android.util.Log;

import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.local.entity.SupplierScoreSnapshotEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.reflect.TypeToken;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // The portal writes these as numbers when it resolves a dispute
// (e.g. 11.0 -> 9.0); the app stores them as text. Accept both.
    private static String getStringOrNumber(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        if (value instanceof Number) return String.valueOf(value);
        return String.valueOf(value);
    }

    private static Double toNullableDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return null;
    }

    private static long parseLongOrZero(String value) {
        try { return value != null ? Long.parseLong(value) : 0; }
        catch (NumberFormatException e) { return 0; }
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

        // Batch / supplier — the substance of a revenue record.
        map.put("batchId", revenue.getBatchId());
        map.put("supplierId", revenue.getSupplierId());
        map.put("batchNumber", revenue.getBatchNumber());
        map.put("supplierName", revenue.getSupplierName());
        map.put("invoiceNumber", revenue.getInvoiceNumber());

        map.put("quantityKg", revenue.getQuantityKg());
        map.put("pricePerKg", revenue.getPricePerKg());
        map.put("totalAmount", revenue.getTotalAmount());
        map.put("buyer", revenue.getBuyer());
        map.put("quality", revenue.getQuality().name());
        map.put("notes", revenue.getNotes());

        // createdAt doubles as the sale date (A1: harvestDate collapsed into it).
        map.put("createdAt", revenue.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());

        // Deliberately absent (migration 40→41, Part A1):
        //   blockId, harvestDate, harvestCycleId, harvestYear, harvestSeason
        // These were junk writes. Portal's per-block P&L (A6) reads block
        // harvestHistory and allocates batch revenue by output share.
        return map;
    }

    public static RevenueEntity mapToRevenue(DocumentSnapshot doc) {
        try {
            long id = Long.parseLong(doc.getId());
            long farmId = getLongSafe(doc, "farmId");

            Long batchId = getNullableLong(doc, "batchId");
            Long supplierId = getNullableLong(doc, "supplierId");
            String batchNumber = doc.getString("batchNumber");
            String supplierName = doc.getString("supplierName");

            double quantityKg = getDoubleSafe(doc, "quantityKg");
            double pricePerKg = getDoubleSafe(doc, "pricePerKg");
            String buyer = getStringSafe(doc, "buyer");
            String qualityStr = getStringSafe(doc, "quality");
            String notes = getStringSafe(doc, "notes");

            long createdAt = getLongSafe(doc, "createdAt");
            long updatedAt = getLongSafe(doc, "updatedAt");

            // Defensive: a legacy doc may carry harvestDate but not createdAt.
            // We treat harvestDate as the sale date to preserve it (matches the
            // migration's `harvestDate AS createdAt` mapping). New docs carry
            // createdAt directly.
            long saleDate = createdAt > 0 ? createdAt : getLongSafe(doc, "harvestDate");

            RevenueEntity revenue = new RevenueEntity(
                    farmId,
                    batchId,
                    supplierId,
                    saleDate,
                    quantityKg,
                    pricePerKg,
                    buyer,
                    RevenueEntity.QualityGrade.valueOf(qualityStr),
                    notes
            );
            revenue.setId(id);
            // saleDate is already in createdAt via the constructor; only overwrite
            // if it was 0 and we have an updatedAt fallback to keep the row valid.
            if (revenue.getCreatedAt() == 0 && updatedAt > 0) {
                revenue.setCreatedAt(updatedAt);
            }
            revenue.setUpdatedAt(updatedAt > 0 ? updatedAt : revenue.getCreatedAt());

            // Denormalized for display
            revenue.setBatchNumber(batchNumber);
            revenue.setSupplierName(supplierName);
            revenue.setInvoiceNumber(doc.getString("invoiceNumber"));

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
        map.put("startDate", task.getStartDate());
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
            task.setStartDate(getNullableLong(doc, "startDate"));
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
        map.put("inspectionId", photo.getInspectionId());
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
            String inspectionId = doc.getString("inspectionId");
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
            photo.setInspectionId(inspectionId);
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
        map.put("productType", batch.getProductType());
        map.put("mouldyRate", batch.getMouldyRate());
        map.put("immatureRate", batch.getImmatureRate());
        map.put("insectDamagedRate", batch.getInsectDamagedRate());
        map.put("shrivelledRate", batch.getShrivelledRate());
        map.put("foreignMatter", batch.getForeignMatter());
        map.put("dryingMethod", batch.getDryingMethod());
        map.put("rcnQualityScore", batch.getRcnQualityScore());
        map.put("brokenKernels", batch.getBrokenKernels());
        map.put("aflatoxinPpb", batch.getAflatoxinPpb());
        map.put("heavyMetalsPpb", batch.getHeavyMetalsPpb());
        map.put("pesticideResiduePpb", batch.getPesticideResiduePpb());
        map.put("kernelQualityScore", batch.getKernelQualityScore());
        map.put("korGPerKg", batch.getKorGPerKg());
        map.put("korSampleGrams", batch.getKorSampleGrams());
        map.put("korKernelGrams", batch.getKorKernelGrams());
        map.put("inspectionDate", batch.getInspectionDate());
        map.put("inspectedBy", batch.getInspectedBy());
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

            if ("PROCESSING".equalsIgnoreCase(gradeStr)) {
                gradeStr = "REJECT";
            }

            QualityBatchEntity.QualityGrade grade;
            try {
                grade = QualityBatchEntity.QualityGrade.valueOf(gradeStr);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unknown qualityGrade in doc, defaulting to STANDARD: " + gradeStr);
                grade = QualityBatchEntity.QualityGrade.STANDARD;
            }

            QualityBatchEntity batch = new QualityBatchEntity(
                    farmId,
                    blockId,
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

            // ✅ Step 1: RCN/kernel split fields
            String productType = doc.getString("productType");
            if (productType == null || productType.isEmpty()) {
                batch.setProductType(doc.getString("productType"));
            }
            batch.setProductType(productType);

            batch.setMouldyRate(getDoubleSafe(doc, "mouldyRate"));
            batch.setImmatureRate(getDoubleSafe(doc, "immatureRate"));
            batch.setInsectDamagedRate(getDoubleSafe(doc, "insectDamagedRate"));
            batch.setShrivelledRate(getDoubleSafe(doc, "shrivelledRate"));
            batch.setForeignMatter(getDoubleSafe(doc, "foreignMatter"));
            batch.setDryingMethod(doc.getString("dryingMethod"));
            batch.setRcnQualityScore(getDoubleSafe(doc, "rcnQualityScore"));
            batch.setBrokenKernels(getDoubleSafe(doc, "brokenKernels"));
            batch.setAflatoxinPpb(getDoubleSafe(doc, "aflatoxinPpb"));
            batch.setHeavyMetalsPpb(getDoubleSafe(doc, "heavyMetalsPpb"));
            batch.setPesticideResiduePpb(getDoubleSafe(doc, "pesticideResiduePpb"));
            batch.setKernelQualityScore(getDoubleSafe(doc, "kernelQualityScore"));
            batch.setKorGPerKg(getDoubleSafe(doc, "korGPerKg"));
            batch.setKorSampleGrams(getDoubleSafe(doc, "korSampleGrams"));
            batch.setKorKernelGrams(getDoubleSafe(doc, "korKernelGrams"));
            Long inspectionDateLong = getLongSafe(doc, "inspectionDate");
            batch.setInspectionDate(inspectionDateLong != null ? inspectionDateLong : 0L);
            batch.setInspectedBy(doc.getString("inspectedBy"));

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
        map.put("annualVolumeKg", supplier.getAnnualVolumeKg() > 0 ? supplier.getAnnualVolumeKg() : null);
        map.put("averageQualityScore", supplier.getAverageQualityScore() > 0 ? supplier.getAverageQualityScore() : null);
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

        map.put("cooperativeName", supplier.getCooperativeName());
        map.put("cooperativeId", supplier.getCooperativeId());
        map.put("farmerIdNumber", supplier.getFarmerIdNumber());

        map.put("dataConsentGiven", supplier.isDataConsentGiven());
        map.put("dataConsentDate", supplier.getDataConsentDate());
        map.put("dataConsentScope", supplier.getDataConsentScopes());  // Firestore array
        map.put("dataConsentSignedBy", supplier.getDataConsentSignedBy());
        map.put("dataConsentVersion", supplier.getDataConsentVersion());
        map.put("dataConsentText", supplier.getDataConsentText());
        map.put("dataConsentWithdrawnDate", supplier.getDataConsentWithdrawnDate());
        map.put("dataConsentWithdrawnReason", supplier.getDataConsentWithdrawnReason());

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
            Double annualVolumeKgObj = doc.getDouble("annualVolumeKg");
            double annualVolumeKg = annualVolumeKgObj != null ? annualVolumeKgObj : 0;
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

            // ✅ Farmer profile fields
            String photoUrl = doc.getString("photoUrl");
            Long yearsFarmingLong = getLongSafe(doc, "yearsFarming");
            int yearsFarming = yearsFarmingLong != null ? yearsFarmingLong.intValue() : 0;
            String bio = doc.getString("bio");
            String quote = doc.getString("quote");
            Double farmSize = doc.getDouble("farmSize");
            Boolean firebreakProtection = doc.getBoolean("firebreakProtection");
            Boolean sustainableHarvesting = doc.getBoolean("sustainableHarvesting");
            Boolean noChildLabor = doc.getBoolean("noChildLabor");

            // ✅ Cooperative linkage + farmer ID
            String cooperativeName = doc.getString("cooperativeName");
            String cooperativeId = doc.getString("cooperativeId");
            String farmerIdNumber = doc.getString("farmerIdNumber");

            long createdAt = getLongSafe(doc, "createdAt");
            long updatedAt = getLongSafe(doc, "updatedAt");
            boolean isSynced = getBooleanSafe(doc, "isSynced");

            // ✅ Construct supplier FIRST (fixes NPE on consent setters)
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

            // ✅ Farmer profile fields
            supplier.setPhotoUrl(photoUrl);
            supplier.setYearsFarming(yearsFarming);
            supplier.setBio(bio);
            supplier.setQuote(quote);
            supplier.setFarmSize(farmSize != null ? farmSize : 0);
            supplier.setFirebreakProtection(firebreakProtection != null && firebreakProtection);
            supplier.setSustainableHarvesting(sustainableHarvesting != null && sustainableHarvesting);
            supplier.setNoChildLabor(noChildLabor != null && noChildLabor);

            // ✅ Consent fields — applied AFTER construction
            Boolean consentGiven = doc.getBoolean("dataConsentGiven");
            supplier.setDataConsentGiven(consentGiven != null && consentGiven);

            // ✅ Cooperative linkage + farmer ID
            supplier.setCooperativeName(cooperativeName);
            supplier.setCooperativeId(cooperativeId);
            supplier.setFarmerIdNumber(farmerIdNumber);

            Long consentDate = getNullableLong(doc, "dataConsentDate");
            if (consentDate != null) supplier.setDataConsentDate(consentDate);

            List<String> consentScopes = new ArrayList<>();
            Object rawScope = doc.get("dataConsentScope");
            if (rawScope instanceof List) {
                for (Object o : (List<?>) rawScope) {
                    if (o instanceof String) consentScopes.add((String) o);
                }
            } else if (rawScope instanceof String) {
                // fallback for hand-edited JSON strings
                try {
                    Type t = new com.google.gson.reflect.TypeToken<List<String>>() {}.getType();
                    consentScopes = new com.google.gson.Gson().fromJson((String) rawScope, t);
                } catch (Exception ignored) {
                }
            }
            supplier.setDataConsentScopes(consentScopes);

            supplier.setDataConsentSignedBy(doc.getString("dataConsentSignedBy"));
            supplier.setDataConsentVersion(doc.getString("dataConsentVersion"));
            supplier.setDataConsentText(doc.getString("dataConsentText"));
            supplier.setDataConsentWithdrawnDate(doc.getString("dataConsentWithdrawnDate"));
            supplier.setDataConsentWithdrawnReason(doc.getString("dataConsentWithdrawnReason"));

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
// SUPPLIER COMMITMENT MAPPING
// ========================================

    public static Map<String, Object> commitmentToMap(SupplierCommitmentEntity c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("supplierId", c.getSupplierId());
        map.put("farmId", c.getFarmId());
        map.put("contractId", c.getContractId());
        map.put("season", c.getSeason());
        map.put("productType", c.getProductType() != null ? c.getProductType().name() : null);
        map.put("committedKg", c.getCommittedKg());
        map.put("committedDate", c.getCommittedDate());
        map.put("linkedBatchIds", c.getLinkedBatchIds());   // JSON string, Firestore-symmetric
        map.put("status", c.getStatus() != null ? c.getStatus().name() : null);
        map.put("delayReason", c.getDelayReason());
        map.put("buyerRequestedChange", c.isBuyerRequestedChange());
        map.put("revisions", c.getRevisions());              // JSON string
        map.put("isSynced", c.isSynced());
        map.put("validatedAt", c.getValidatedAt());
        map.put("validatedBy", c.getValidatedBy());
        map.put("createdBy", c.getCreatedBy());
        map.put("updatedBy", c.getUpdatedBy());
        map.put("createdAt", c.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static SupplierCommitmentEntity mapToCommitment(DocumentSnapshot doc) {
        try {
            long id = getLongSafe(doc, "id");
            long supplierId = getLongSafe(doc, "supplierId");
            long farmId = getLongSafe(doc, "farmId");
            String contractId = getStringSafe(doc, "contractId");
            String season = getStringSafe(doc, "season");
            double committedKg = getDoubleSafe(doc, "committedKg");
            long committedDate = getLongSafe(doc, "committedDate");

            SupplierCommitmentEntity c = new SupplierCommitmentEntity(
                    supplierId, farmId, contractId, season,
                    parseProductType(doc.getString("productType")),
                    committedKg, committedDate
            );
            c.setId(id);

            // JSON fields — string passthrough (Firestore stores them as strings,
            // same as Room; no array conversion needed at the mapper layer)
            String linked = doc.getString("linkedBatchIds");
            c.setLinkedBatchIds(linked != null ? linked : "[]");
            String revisions = doc.getString("revisions");
            c.setRevisions(revisions != null ? revisions : "[]");

            String status = doc.getString("status");
            if (status != null && !status.isEmpty()) {
                try { c.setStatus(SupplierCommitmentEntity.CommitmentStatus.valueOf(status)); }
                catch (IllegalArgumentException ignored) { }
            }

            c.setDelayReason(doc.getString("delayReason"));

            Boolean buyerChange = doc.getBoolean("buyerRequestedChange");
            c.setBuyerRequestedChange(buyerChange != null && buyerChange);

            Boolean isSynced = doc.getBoolean("isSynced");
            c.setSynced(isSynced != null && isSynced);

            Long validatedAt = getNullableLong(doc, "validatedAt");
            if (validatedAt != null) c.setValidatedAt(validatedAt);

            c.setValidatedBy(doc.getString("validatedBy"));
            c.setCreatedBy(doc.getString("createdBy"));
            c.setUpdatedBy(doc.getString("updatedBy"));

            Long createdAt = getNullableLong(doc, "createdAt");
            if (createdAt != null) c.setCreatedAt(createdAt);
            Long updatedAt = getNullableLong(doc, "updatedAt");
            if (updatedAt != null) c.setUpdatedAt(updatedAt);

            return c;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping commitment document", e);
            return null;
        }
    }

    private static SupplierCommitmentEntity.ProductType parseProductType(String s) {
        if (s == null || s.isEmpty()) return SupplierCommitmentEntity.ProductType.RCN;
        try { return SupplierCommitmentEntity.ProductType.valueOf(s); }
        catch (IllegalArgumentException e) { return SupplierCommitmentEntity.ProductType.RCN; }
    }

    // ========================================
// SUPPLIER INSPECTION MAPPING
// ========================================

    public static Map<String, Object> supplierInspectionToMap(SupplierInspectionEntity i) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", i.getId());
        map.put("supplierId", i.getSupplierId());
        map.put("farmId", i.getFarmId());
        map.put("inspectionType", i.getInspectionType() != null ? i.getInspectionType().name() : null);
        map.put("inspectionDate", i.getInspectionDate());
        map.put("outcome", i.getOutcome() != null ? i.getOutcome().name() : null);
        map.put("severity", i.getSeverity());
        map.put("findings", i.getFindings());
        map.put("correctiveAction", i.getCorrectiveAction());
        map.put("closedDate", i.getClosedDate());
        map.put("status", i.getStatus() != null ? i.getStatus().name() : null);
        map.put("inspector", i.getInspector());
        map.put("evidenceUrls", i.getEvidenceUrls());
        map.put("linkedBatchIds", i.getLinkedBatchIds());
        map.put("isSynced", i.isSynced());
        map.put("validatedAt", i.getValidatedAt());
        map.put("validatedBy", i.getValidatedBy());
        map.put("createdBy", i.getCreatedBy());
        map.put("updatedBy", i.getUpdatedBy());
        map.put("createdAt", i.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static SupplierInspectionEntity mapToSupplierInspection(DocumentSnapshot doc) {
        try {
            long id = getLongSafe(doc, "id");
            long supplierId = getLongSafe(doc, "supplierId");
            long farmId = getLongSafe(doc, "farmId");
            long inspectionDate = getLongSafe(doc, "inspectionDate");
            String findings = doc.getString("findings");
            String inspector = doc.getString("inspector");

            SupplierInspectionEntity i = new SupplierInspectionEntity(
                    supplierId, farmId,
                    parseInspectionType(doc.getString("inspectionType")),
                    inspectionDate,
                    parseOutcome(doc.getString("outcome")),
                    findings, inspector
            );
            i.setId(id);

            String severity = doc.getString("severity");
            if (severity != null && !severity.isEmpty()) i.setSeverity(severity);

            i.setCorrectiveAction(doc.getString("correctiveAction"));
            Long closedDate = getNullableLong(doc, "closedDate");
            if (closedDate != null) i.setClosedDate(closedDate);

            String status = doc.getString("status");
            if (status != null && !status.isEmpty()) {
                try { i.setStatus(SupplierInspectionEntity.InspectionStatus.valueOf(status)); }
                catch (IllegalArgumentException ignored) { }
            }

            String evidence = doc.getString("evidenceUrls");
            i.setEvidenceUrls(evidence != null ? evidence : "[]");
            String linked = doc.getString("linkedBatchIds");
            i.setLinkedBatchIds(linked != null ? linked : "[]");

            Boolean isSynced = doc.getBoolean("isSynced");
            i.setSynced(isSynced != null && isSynced);

            Long validatedAt = getNullableLong(doc, "validatedAt");
            if (validatedAt != null) i.setValidatedAt(validatedAt);

            i.setValidatedBy(doc.getString("validatedBy"));
            i.setCreatedBy(doc.getString("createdBy"));
            i.setUpdatedBy(doc.getString("updatedBy"));

            Long createdAt = getNullableLong(doc, "createdAt");
            if (createdAt != null) i.setCreatedAt(createdAt);
            Long updatedAt = getNullableLong(doc, "updatedAt");
            if (updatedAt != null) i.setUpdatedAt(updatedAt);

            return i;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping inspection document", e);
            return null;
        }
    }

    private static SupplierInspectionEntity.InspectionType parseInspectionType(String s) {
        if (s == null || s.isEmpty()) return SupplierInspectionEntity.InspectionType.OTHER;
        try { return SupplierInspectionEntity.InspectionType.valueOf(s); }
        catch (IllegalArgumentException e) { return SupplierInspectionEntity.InspectionType.OTHER; }
    }

    private static SupplierInspectionEntity.Outcome parseOutcome(String s) {
        if (s == null || s.isEmpty()) return SupplierInspectionEntity.Outcome.PASS;
        try { return SupplierInspectionEntity.Outcome.valueOf(s); }
        catch (IllegalArgumentException e) { return SupplierInspectionEntity.Outcome.PASS; }
    }


    // ========================================
// SUPPLIER DISPUTE MAPPING
// ========================================

    public static Map<String, Object> supplierDisputeToMap(SupplierDisputeEntity d) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", d.getId());
        map.put("supplierId", d.getSupplierId());
        map.put("farmId", d.getFarmId());
        map.put("targetEntity", d.getTargetEntity() != null ? d.getTargetEntity().name() : null);
        // Firestore receives targetId as a number here; the SCORE-portal path writes it
        // as a string on the portal side and never materialises in Room, so no conflict.
        map.put("targetId", d.getTargetId());
        map.put("reason", d.getReason());
        map.put("evidenceUrls", d.getEvidenceUrls());
        map.put("submittedBy", d.getSubmittedBy());
        map.put("submittedDate", d.getSubmittedDate());
        map.put("reviewStatus", d.getReviewStatus() != null ? d.getReviewStatus().name() : null);
        map.put("decision", d.getDecision());
        map.put("reviewedBy", d.getReviewedBy());
        map.put("resolvedDate", d.getResolvedDate());
        map.put("correctionApplied", d.isCorrectionApplied());
        map.put("originalValue", d.getOriginalValue());
        map.put("correctedValue", d.getCorrectedValue());
        map.put("recalculatedSnapshotId", d.getRecalculatedSnapshotId());
        map.put("isSynced", d.isSynced());
        map.put("createdBy", d.getCreatedBy());
        map.put("updatedBy", d.getUpdatedBy());
        map.put("createdAt", d.getCreatedAt());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static SupplierDisputeEntity mapToDispute(DocumentSnapshot doc) {
        try {
            long id = getLongSafe(doc, "id");
            long supplierId = getLongSafe(doc, "supplierId");
            long farmId = getLongSafe(doc, "farmId");

            Object rawTarget = doc.get("targetId");
            long targetId;
            if (rawTarget instanceof Number) {
                targetId = ((Number) rawTarget).longValue();
            } else if (rawTarget instanceof String) {
                try {
                    targetId = Long.parseLong((String) rawTarget);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Non-numeric targetId for dispute " + doc.getId()
                            + " — skipping (portal SCORE dispute, not materialised in Room)");
                    return null;
                }
            } else {
                Log.w(TAG, "Missing targetId for dispute " + doc.getId());
                return null;
            }

            String reason = doc.getString("reason");
            String submittedBy = doc.getString("submittedBy");

            SupplierDisputeEntity d = new SupplierDisputeEntity(
                    supplierId, farmId,
                    parseTargetEntity(doc.getString("targetEntity")),
                    targetId,
                    reason, submittedBy
            );
            d.setId(id);

            String evidence = doc.getString("evidenceUrls");
            d.setEvidenceUrls(evidence != null ? evidence : "[]");

            Long submittedDate = getNullableLong(doc, "submittedDate");
            if (submittedDate != null) d.setSubmittedDate(submittedDate);

            String status = doc.getString("reviewStatus");
            if (status != null && !status.isEmpty()) {
                try { d.setReviewStatus(SupplierDisputeEntity.ReviewStatus.valueOf(status)); }
                catch (IllegalArgumentException ignored) { }
            }

            d.setDecision(doc.getString("decision"));
            d.setReviewedBy(doc.getString("reviewedBy"));

            Long resolvedDate = getNullableLong(doc, "resolvedDate");
            if (resolvedDate != null) d.setResolvedDate(resolvedDate);

            Boolean correctionApplied = doc.getBoolean("correctionApplied");
            d.setCorrectionApplied(correctionApplied != null && correctionApplied);

            d.setOriginalValue(getStringOrNumber(doc, "originalValue"));
            d.setCorrectedValue(getStringOrNumber(doc, "correctedValue"));

            Long snapshotId = getNullableLong(doc, "recalculatedSnapshotId");
            if (snapshotId != null) d.setRecalculatedSnapshotId(snapshotId);

            Boolean isSynced = doc.getBoolean("isSynced");
            d.setSynced(isSynced != null && isSynced);

            d.setCreatedBy(doc.getString("createdBy"));
            d.setUpdatedBy(doc.getString("updatedBy"));

            Long createdAt = getNullableLong(doc, "createdAt");
            if (createdAt != null) d.setCreatedAt(createdAt);
            Long updatedAt = getNullableLong(doc, "updatedAt");
            if (updatedAt != null) d.setUpdatedAt(updatedAt);

            return d;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping dispute document", e);
            return null;
        }
    }

    private static SupplierDisputeEntity.TargetEntity parseTargetEntity(String s) {
        if (s == null || s.isEmpty()) return SupplierDisputeEntity.TargetEntity.COMMITMENT;
        try { return SupplierDisputeEntity.TargetEntity.valueOf(s); }
        catch (IllegalArgumentException e) { return SupplierDisputeEntity.TargetEntity.COMMITMENT; }
    }

    // ========================================
// SUPPLIER SCORE SNAPSHOT MAPPING
// ========================================

    public static Map<String, Object> supplierScoreSnapshotToMap(SupplierScoreSnapshotEntity s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("supplierId", s.getSupplierId());
        map.put("farmId", s.getFarmId());
        map.put("overall", s.getOverall());                 // may be null — intentional
        map.put("quality", s.getQuality());
        map.put("delivery", s.getDelivery());
        map.put("traceability", s.getTraceability());
        map.put("compliance", s.getCompliance());
        map.put("sustainability", s.getSustainability());
        map.put("cooperation", s.getCooperation());
        map.put("confidence", s.getConfidence() != null ? s.getConfidence().name() : null);
        map.put("verifiedTransactions", s.getVerifiedTransactions());
        map.put("scoringVersion", s.getScoringVersion());
        map.put("weightsUsed", s.getWeightsUsed());
        map.put("calculatedAt", s.getCalculatedAt());
        map.put("calculatedBy", s.getCalculatedBy());
        map.put("evidenceRefs", s.getEvidenceRefs());
        map.put("createdAt", s.getCreatedAt());
        return map;
    }

    public static SupplierScoreSnapshotEntity mapToSnapshot(DocumentSnapshot doc) {
        try {
            Long supplierIdLong = getLongSafe(doc, "supplierId");
            long supplierId = supplierIdLong != null ? supplierIdLong : parseLongOrZero(doc.getId());
            Long farmIdLong = getLongSafe(doc, "farmId");
            long farmId = farmIdLong != null ? farmIdLong : 0;
            String scoringVersion = doc.getString("scoringVersion");
            String calculatedBy = doc.getString("calculatedBy");

            SupplierScoreSnapshotEntity s = new SupplierScoreSnapshotEntity(
                    supplierId, farmId, scoringVersion, calculatedBy);

            // Nullable doubles — preserve null (means "no evidence for this dimension")
            Double overall = doc.getDouble("overall");
            s.setOverall(overall);
            Object dimsObj = doc.get("dimensions");
            if (dimsObj instanceof Map) {
                Map<?, ?> dims = (Map<?, ?>) dimsObj;
                s.setQuality(toNullableDouble(dims.get("quality")));
                s.setDelivery(toNullableDouble(dims.get("delivery")));
                s.setTraceability(toNullableDouble(dims.get("traceability")));
                s.setCompliance(toNullableDouble(dims.get("compliance")));
                s.setSustainability(toNullableDouble(dims.get("sustainability")));
                s.setCooperation(toNullableDouble(dims.get("cooperation")));
            }

            String confidence = doc.getString("confidence");
            if (confidence != null && !confidence.isEmpty()) {
                try { s.setConfidence(SupplierScoreSnapshotEntity.Confidence.valueOf(confidence)); }
                catch (IllegalArgumentException ignored) { }
            }

            Long verifiedLong = getNullableLong(doc, "verifiedTransactions");
            if (verifiedLong != null) s.setVerifiedTransactions(verifiedLong.intValue());

            Object weightsObj = doc.get("weightsUsed");
            String weights = "{}";
            if (weightsObj instanceof String) {
                weights = (String) weightsObj;
            } else if (weightsObj instanceof Map) {
                weights = new Gson().toJson(weightsObj);
            }
            s.setWeightsUsed(weights);

            Long calculatedAt = getNullableLong(doc, "calculatedAt");
            if (calculatedAt != null) s.setCalculatedAt(calculatedAt);

            String evidenceRefs = doc.getString("evidenceRefs");
            s.setEvidenceRefs(evidenceRefs != null ? evidenceRefs : "[]");

            Long createdAt = getNullableLong(doc, "createdAt");
            if (createdAt != null) s.setCreatedAt(createdAt);

            return s;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping snapshot document", e);
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
        map.put("revenueId", traceability.getRevenueId());
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
        map.put("masterBatchId", traceability.getMasterBatchId());
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
            traceability.setMasterBatchId(doc.getString("masterBatchId"));
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
        map.put("farmId", master.getFarmId());
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

            MasterBatchTraceabilityEntity master =
                    new MasterBatchTraceabilityEntity(masterBatchNumber, shipmentName);

            master.setId(id);
            master.setShipmentId(doc.getString("shipmentId"));

            Long farmId = doc.getLong("farmId");
            if (farmId != null) master.setFarmId(farmId);

            Double totalWeight = doc.getDouble("totalWeightKg");
            if (totalWeight != null) master.setTotalWeightKg(totalWeight);

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
    // CERTIFICATION MAPPING
    // ========================================

    public static Map<String, Object> certificationToMap(CertificationEntity certification) {
        Map<String, Object> map = new HashMap<>();
        map.put("certificationId", certification.getCertificationId());
        map.put("farmId", certification.getFarmId());
        map.put("certificationName", certification.getCertificationName());
        map.put("certificationBody", certification.getCertificationBody());
        map.put("status", certification.getStatus() != null ? certification.getStatus().name() : null);
        map.put("scope", certification.getScope());
        map.put("applicableStandardsJson", certification.getApplicableStandardsJson());
        map.put("statusHistoryJson", certification.getStatusHistoryJson());
        map.put("targetDate", certification.getTargetDate());
        map.put("approvalDate", certification.getApprovalDate());
        map.put("expiryDate", certification.getExpiryDate());
        map.put("createdAt", certification.getCreatedAt());
        map.put("updatedAt", certification.getUpdatedAt());
        map.put("isSynced", certification.isSynced());
        return map;
    }

    public static CertificationEntity mapToCertification(DocumentSnapshot doc) {
        try {
            String certificationId = doc.getId();
            Long farmId = doc.getLong("farmId");

            if (farmId == null) {
                Log.e(TAG, "Certification missing farmId, skipping");
                return null;
            }

            String certificationName = doc.getString("certificationName");
            String certificationBody = doc.getString("certificationBody");
            String statusStr = doc.getString("status");
            String scope = doc.getString("scope");
            String statusHistoryJson = doc.getString("statusHistoryJson");
            String applicableStandardsJson = doc.getString("applicableStandardsJson");
            Long targetDate = doc.getLong("targetDate");
            Long approvalDate = doc.getLong("approvalDate");
            Long expiryDate = doc.getLong("expiryDate");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");
            Boolean isSynced = doc.getBoolean("isSynced");

            // Parse status
            CertificationEntity.CertificationStatus status = CertificationEntity.CertificationStatus.NOT_STARTED;
            if (statusStr != null) {
                try {
                    status = CertificationEntity.CertificationStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown certification status: " + statusStr);
                }
            }

            // Parse standards from JSON
            List<String> applicableStandards = new ArrayList<>();
            if (applicableStandardsJson != null) {
                try {
                    Type listType = new TypeToken<List<String>>(){}.getType();
                    applicableStandards = new Gson().fromJson(applicableStandardsJson, listType);
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing applicable standards", e);
                }
            }

            CertificationEntity certification = new CertificationEntity(
                    farmId,
                    certificationName,
                    certificationBody,
                    status,
                    scope,
                    applicableStandards,
                    targetDate != null ? targetDate : 0,
                    approvalDate != null ? approvalDate : 0,
                    expiryDate != null ? expiryDate : 0
            );

            certification.setCertificationId(certificationId);
            certification.setStatusHistoryJson(statusHistoryJson);
            certification.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());
            certification.setUpdatedAt(updatedAt != null ? updatedAt : System.currentTimeMillis());
            certification.setSynced(isSynced != null && isSynced);

            return certification;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping certification", e);
            return null;
        }
    }

    public static CertificationEntity fromFirestoreCertification(DocumentSnapshot doc) {
        return mapToCertification(doc);
    }

    // ========================================
    // AUDIT MAPPING
    // ========================================

    public static Map<String, Object> auditToMap(AuditEntity audit) {
        Map<String, Object> map = new HashMap<>();
        map.put("auditId", audit.getAuditId());
        map.put("supersedesId", audit.getSupersedesId());
        map.put("validatedAt", audit.getValidatedAt());
        map.put("validatedBy", audit.getValidatedBy());
        map.put("certificationId", audit.getCertificationId());
        map.put("farmId", audit.getFarmId());
        map.put("auditType", audit.getAuditType() != null ? audit.getAuditType().name() : null);
        map.put("auditDate", audit.getAuditDate());
        map.put("auditorName", audit.getAuditorName());
        map.put("auditorBody", audit.getAuditorBody());
        map.put("scope", audit.getScope());
        map.put("findingsJson", audit.getFindingsJson());
        map.put("status", audit.getStatus() != null ? audit.getStatus().name() : null);
        map.put("reportUrl", audit.getReportUrl());
        map.put("createdAt", audit.getCreatedAt());
        map.put("updatedAt", audit.getUpdatedAt());
        map.put("isSynced", audit.isSynced());
        return map;
    }

    public static AuditEntity mapToAudit(DocumentSnapshot doc) {
        try {
            String auditId = doc.getId();
            String certificationId = doc.getString("certificationId");
            Long farmId = doc.getLong("farmId");

            if (certificationId == null || farmId == null) {
                Log.e(TAG, "Audit missing certificationId or farmId, skipping");
                return null;
            }

            String auditTypeStr = doc.getString("auditType");
            String supersedesId = doc.getString("supersedesId");
            Long validatedAt = doc.getLong("validatedAt");
            String validatedBy = doc.getString("validatedBy");
            Long auditDate = doc.getLong("auditDate");
            String auditorName = doc.getString("auditorName");
            String auditorBody = doc.getString("auditorBody");
            String scope = doc.getString("scope");
            String findingsJson = doc.getString("findingsJson");
            String statusStr = doc.getString("status");
            String reportUrl = doc.getString("reportUrl");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");
            Boolean isSynced = doc.getBoolean("isSynced");

            // Parse audit type
            AuditEntity.AuditType auditType = AuditEntity.AuditType.EXTERNAL;
            if (auditTypeStr != null) {
                try {
                    auditType = AuditEntity.AuditType.valueOf(auditTypeStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown audit type: " + auditTypeStr);
                }
            }

            // Parse status
            AuditEntity.AuditStatus status = AuditEntity.AuditStatus.PASS;
            if (statusStr != null) {
                try {
                    status = AuditEntity.AuditStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown audit status: " + statusStr);
                }
            }

            // Parse findings
            List<AuditEntity.Finding> findings = new ArrayList<>();
            if (findingsJson != null) {
                try {
                    Type listType = new TypeToken<List<AuditEntity.Finding>>(){}.getType();
                    findings = new Gson().fromJson(findingsJson, listType);
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing findings", e);
                }
            }

            AuditEntity audit = new AuditEntity(
                    certificationId,
                    farmId,
                    auditType,
                    auditDate != null ? auditDate : 0,
                    auditorName,
                    auditorBody,
                    scope,
                    findings,
                    status,
                    reportUrl
            );
            audit.setAuditId(auditId);
            audit.setSupersedesId(supersedesId);
            audit.setValidatedAt(validatedAt);
            audit.setValidatedBy(validatedBy);
            audit.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());
            audit.setUpdatedAt(updatedAt != null ? updatedAt : System.currentTimeMillis());
            audit.setSynced(isSynced != null && isSynced);

            return audit;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping audit", e);
            return null;
        }
    }

    // ========================================
    // INSPECTION MAPPING
    // ========================================

    public static Map<String, Object> inspectionToMap(InspectionEntity inspection) {
        Map<String, Object> map = new HashMap<>();
        map.put("inspectionId", inspection.getInspectionId());
        map.put("supersedesId", inspection.getSupersedesId());
        map.put("validatedAt", inspection.getValidatedAt());
        map.put("validatedBy", inspection.getValidatedBy());
        map.put("certificationId", inspection.getCertificationId());
        map.put("certificationName", inspection.getCertificationName());
        map.put("farmId", inspection.getFarmId());
        map.put("requirementCategory", inspection.getRequirementCategory() != null ?
                inspection.getRequirementCategory().name() : null);
        map.put("inspectionType", inspection.getInspectionType() != null ?
                inspection.getInspectionType().name() : null);
        map.put("inspectionDate", inspection.getInspectionDate());
        map.put("inspectorName", inspection.getInspectorName());
        map.put("inspectorBody", inspection.getInspectorBody());
        map.put("status", inspection.getStatus() != null ? inspection.getStatus().name() : null);
        map.put("remarks", inspection.getRemarks());
        map.put("correctiveActionsJson", inspection.getCorrectiveActionsJson());
        map.put("photosJson", inspection.getPhotosJson());
        map.put("reportUrl", inspection.getReportUrl());
        map.put("createdAt", inspection.getCreatedAt());
        map.put("updatedAt", inspection.getUpdatedAt());
        map.put("isSynced", inspection.isSynced());
        return map;
    }

    public static InspectionEntity mapToInspection(DocumentSnapshot doc) {
        try {
            String inspectionId = doc.getId();
            String certificationId = doc.getString("certificationId");
            String certificationName = doc.getString("certificationName");
            Long farmId = doc.getLong("farmId");

            if (certificationId == null || farmId == null) {
                Log.e(TAG, "Inspection missing certificationId or farmId, skipping");
                return null;
            }

            String requirementCategoryStr = doc.getString("requirementCategory");
            String supersedesId = doc.getString("supersedesId");
            Long validatedAt = doc.getLong("validatedAt");
            String validatedBy = doc.getString("validatedBy");
            String inspectionTypeStr = doc.getString("inspectionType");
            Long inspectionDate = doc.getLong("inspectionDate");
            String inspectorName = doc.getString("inspectorName");
            String inspectorBody = doc.getString("inspectorBody");
            String statusStr = doc.getString("status");
            String remarks = doc.getString("remarks");
            String correctiveActionsJson = doc.getString("correctiveActionsJson");
            String photosJson = doc.getString("photosJson");
            String reportUrl = doc.getString("reportUrl");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");
            Boolean isSynced = doc.getBoolean("isSynced");

            // Parse requirement category
            InspectionEntity.RequirementCategory requirementCategory =
                    InspectionEntity.RequirementCategory.FIELD;
            if (requirementCategoryStr != null) {
                try {
                    requirementCategory = InspectionEntity.RequirementCategory.valueOf(requirementCategoryStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown requirement category: " + requirementCategoryStr);
                }
            }

            // Parse inspection type
            InspectionEntity.InspectionType inspectionType =
                    InspectionEntity.InspectionType.EXTERNAL;
            if (inspectionTypeStr != null) {
                try {
                    inspectionType = InspectionEntity.InspectionType.valueOf(inspectionTypeStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown inspection type: " + inspectionTypeStr);
                }
            }

            // Parse status
            InspectionEntity.InspectionStatus status =
                    InspectionEntity.InspectionStatus.PASS;
            if (statusStr != null) {
                try {
                    status = InspectionEntity.InspectionStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown inspection status: " + statusStr);
                }
            }

            // Parse corrective actions
            List<InspectionEntity.CorrectiveAction> correctiveActions = new ArrayList<>();
            if (correctiveActionsJson != null) {
                try {
                    Type listType = new TypeToken<List<InspectionEntity.CorrectiveAction>>(){}.getType();
                    correctiveActions = new Gson().fromJson(correctiveActionsJson, listType);
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing corrective actions", e);
                }
            }

            // Parse photos
            List<String> photos = new ArrayList<>();
            if (photosJson != null) {
                try {
                    Type listType = new TypeToken<List<String>>(){}.getType();
                    photos = new Gson().fromJson(photosJson, listType);
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing photos", e);
                }
            }

            InspectionEntity inspection = new InspectionEntity(
                    certificationId,
                    certificationName,
                    farmId,
                    requirementCategory,
                    inspectionType,
                    inspectionDate != null ? inspectionDate : 0,
                    inspectorName,
                    inspectorBody,
                    status,
                    remarks,
                    correctiveActions,
                    photos,
                    reportUrl
            );
            inspection.setInspectionId(inspectionId);
            inspection.setSupersedesId(supersedesId);
            inspection.setValidatedAt(validatedAt);
            inspection.setValidatedBy(validatedBy);
            inspection.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());
            inspection.setUpdatedAt(updatedAt != null ? updatedAt : System.currentTimeMillis());
            inspection.setSynced(isSynced != null && isSynced);

            return inspection;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping inspection", e);
            return null;
        }
    }

    // ========================================
// VERIFICATION MAPPING
// ========================================

    public static Map<String, Object> verificationToMap(VerificationEntity verification) {
        Map<String, Object> map = new HashMap<>();
        map.put("verificationId", verification.getVerificationId());
        map.put("supersedesId", verification.getSupersedesId());
        map.put("validatedAt", verification.getValidatedAt());
        map.put("validatedBy", verification.getValidatedBy());
        map.put("certificationId", verification.getCertificationId());
        map.put("farmId", verification.getFarmId());
        map.put("verificationType", verification.getVerificationType() != null ?
                verification.getVerificationType().name() : null);
        map.put("verificationDate", verification.getVerificationDate());
        map.put("verifiedBy", verification.getVerifiedBy());
        map.put("status", verification.getStatus() != null ?
                verification.getStatus().name() : null);
        map.put("evidenceJson", verification.getEvidenceJson());
        map.put("comment", verification.getComment());
        map.put("createdAt", verification.getCreatedAt());
        map.put("updatedAt", verification.getUpdatedAt());
        map.put("isSynced", verification.isSynced());
        return map;
    }

    public static VerificationEntity mapToVerification(DocumentSnapshot doc) {
        try {
            String verificationId = doc.getId();
            String certificationId = doc.getString("certificationId");
            Long farmId = doc.getLong("farmId");

            if (certificationId == null || farmId == null) {
                Log.e(TAG, "Verification missing certificationId or farmId, skipping");
                return null;
            }

            String verificationTypeStr = doc.getString("verificationType");
            String supersedesId = doc.getString("supersedesId");
            Long validatedAt = doc.getLong("validatedAt");
            String validatedBy = doc.getString("validatedBy");
            Long verificationDate = doc.getLong("verificationDate");
            String verifiedBy = doc.getString("verifiedBy");
            String statusStr = doc.getString("status");
            String evidenceJson = doc.getString("evidenceJson");
            String comment = doc.getString("comment");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");
            Boolean isSynced = doc.getBoolean("isSynced");

            // Parse verification type
            VerificationEntity.VerificationType verificationType =
                    VerificationEntity.VerificationType.DOCUMENTATION;
            if (verificationTypeStr != null) {
                try {
                    verificationType = VerificationEntity.VerificationType.valueOf(verificationTypeStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown verification type: " + verificationTypeStr);
                }
            }

            // Parse status
            VerificationEntity.VerificationStatus status =
                    VerificationEntity.VerificationStatus.PENDING;
            if (statusStr != null) {
                try {
                    status = VerificationEntity.VerificationStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown verification status: " + statusStr);
                }
            }

            // Parse evidence
            List<String> evidence = new ArrayList<>();
            if (evidenceJson != null) {
                try {
                    Type listType = new TypeToken<List<String>>(){}.getType();
                    evidence = new Gson().fromJson(evidenceJson, listType);
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing evidence", e);
                }
            }

            VerificationEntity verification = new VerificationEntity(
                    certificationId,
                    farmId,
                    verificationType,
                    verificationDate != null ? verificationDate : 0,
                    verifiedBy,
                    status,
                    evidence,
                    comment
            );
            verification.setVerificationId(verificationId);
            verification.setSupersedesId(supersedesId);
            verification.setValidatedAt(validatedAt);
            verification.setValidatedBy(validatedBy);
            verification.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());
            verification.setUpdatedAt(updatedAt != null ? updatedAt : System.currentTimeMillis());
            verification.setSynced(isSynced != null && isSynced);

            return verification;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping verification", e);
            return null;
        }
    }

    // ========================================
// DOCUMENT MAPPING
// ========================================

    public static Map<String, Object> documentToMap(DocumentEntity document) {
        Map<String, Object> map = new HashMap<>();
        map.put("documentId", document.getDocumentId());
        map.put("certificationId", document.getCertificationId());
        map.put("farmId", document.getFarmId());
        map.put("documentType", document.getDocumentType() != null ?
                document.getDocumentType().name() : null);
        map.put("documentName", document.getDocumentName());
        map.put("issueDate", document.getIssueDate());
        map.put("expiryDate", document.getExpiryDate());
        map.put("fileUrl", document.getFileUrl());
        map.put("status", document.getStatus() != null ?
                document.getStatus().name() : null);
        map.put("createdAt", document.getCreatedAt());
        map.put("updatedAt", document.getUpdatedAt());
        map.put("isSynced", document.isSynced());
        return map;
    }

    public static DocumentEntity mapToDocument(DocumentSnapshot doc) {
        try {
            String documentId = doc.getId();
            String certificationId = doc.getString("certificationId");
            Long farmId = doc.getLong("farmId");

            if (certificationId == null || farmId == null) {
                Log.e(TAG, "Document missing certificationId or farmId, skipping");
                return null;
            }

            String documentTypeStr = doc.getString("documentType");
            String documentName = doc.getString("documentName");
            Long issueDate = doc.getLong("issueDate");
            Long expiryDate = doc.getLong("expiryDate");
            String fileUrl = doc.getString("fileUrl");
            String statusStr = doc.getString("status");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");
            Boolean isSynced = doc.getBoolean("isSynced");

            // Parse document type
            DocumentEntity.DocumentType documentType =
                    DocumentEntity.DocumentType.CERTIFICATE;
            if (documentTypeStr != null) {
                try {
                    String normalizedType = documentTypeStr.trim().toUpperCase(java.util.Locale.ROOT)
                            .replace(' ', '_').replace('-', '_');
                    if (normalizedType.equals("SOIL_ANALYSIS")
                            || normalizedType.equals("SOIL_ANALYSIS_REPORT")) {
                        normalizedType = "LAB_REPORT";
                    } else if (normalizedType.equals("LICENCE")) {
                        normalizedType = "LICENSE";
                    }
                    documentType = DocumentEntity.DocumentType.valueOf(normalizedType);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown document type: " + documentTypeStr);
                }
            }

            // Parse status
            DocumentEntity.DocumentStatus status =
                    DocumentEntity.DocumentStatus.DRAFT;
            if (statusStr != null) {
                try {
                    status = DocumentEntity.DocumentStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown document status: " + statusStr);
                }
            }

            DocumentEntity document = new DocumentEntity(
                    certificationId,
                    farmId,
                    documentType,
                    documentName,
                    issueDate != null ? issueDate : 0,
                    expiryDate != null ? expiryDate : 0,
                    fileUrl,
                    status
            );
            document.setDocumentId(documentId);
            document.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());
            document.setUpdatedAt(updatedAt != null ? updatedAt : System.currentTimeMillis());
            document.setSynced(isSynced != null && isSynced);

            return document;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping document", e);
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
