package com.afrinuts.farmos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.afrinuts.farmos.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Entity(tableName = "inspections",
        foreignKeys = {
                @ForeignKey(
                        entity = CertificationEntity.class,
                        parentColumns = "certificationId",
                        childColumns = "certificationId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = FarmEntity.class,
                        parentColumns = "id",
                        childColumns = "farmId",
                        onDelete = ForeignKey.CASCADE
                )
        })
public class InspectionEntity {

    @PrimaryKey
    @NonNull
    private String inspectionId; // INSP-ORG-2026-078
    private String supersedesId; // null unless this record corrects another record (its ID)

    private String certificationId;
    private String certificationName; // Denormalized for quick display
    private long farmId;

    private RequirementCategory requirementCategory; // FIELD, PROCESSING, STORAGE, DOCUMENTATION, LABORATORY
    private InspectionType inspectionType; // EXTERNAL, INTERNAL, SURVEILLANCE, RENEWAL
    private long inspectionDate;
    private String inspectorName;
    private String inspectorBody;
    private InspectionStatus status; // PASS, FAIL, CONDITIONAL_PASS
    private String remarks;
    private String correctiveActionsJson; // JSON array of corrective actions
    private String photosJson; // JSON array of photo URLs
    private String reportUrl;

    private long createdAt;
    private long updatedAt;
    private boolean isSynced;

    @Ignore
    private List<CorrectiveAction> correctiveActions;
    @Ignore
    private List<String> photos;

    public enum RequirementCategory {
        FIELD("Field", R.drawable.ic_grass),
        PROCESSING("Processing", R.drawable.ic_factory),
        STORAGE("Storage", R.drawable.ic_warehouse),
        DOCUMENTATION("Documentation", R.drawable.ic_description),
        LABORATORY("Laboratory", R.drawable.ic_science);

        private final String displayName;
        private final int iconResId;

        RequirementCategory(String displayName, int iconResId) {
            this.displayName = displayName;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }
    }

    public enum InspectionType {
        EXTERNAL("External", "External inspection"),
        INTERNAL("Internal", "Internal inspection"),
        SURVEILLANCE("Surveillance", "Surveillance inspection"),
        RENEWAL("Renewal", "Renewal inspection");

        private final String displayName;
        private final String description;

        InspectionType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public enum InspectionStatus {
        PASS("Pass", R.drawable.ic_check_circle),
        FAIL("Fail", R.drawable.ic_cancel_expired),
        CONDITIONAL_PASS("Conditional Pass", R.drawable.ic_warning_suspended);

        private final String displayName;
        private final int iconResId;

        InspectionStatus(String displayName, int iconResId) {
            this.displayName = displayName;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }
    }

    // Corrective Action class
    public static class CorrectiveAction {
        public String description;
        public String assignedTo;
        public String status;
        public long dueDate;

        public CorrectiveAction() {}

        public CorrectiveAction(String description, String assignedTo) {
            this.description = description;
            this.assignedTo = assignedTo;
            this.status = "OPEN";
        }

        public String getFormattedDueDate() {
            if (dueDate <= 0) return "Not set";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(dueDate));
        }
    }

    // Default constructor for Room
    public InspectionEntity() {
        // Required by Room
    }

    // Constructor for creating new inspections - Room will use this
    public InspectionEntity(String certificationId, String certificationName, long farmId,
                            RequirementCategory requirementCategory, InspectionType inspectionType,
                            long inspectionDate, String inspectorName, String inspectorBody,
                            InspectionStatus status, String remarks,
                            String correctiveActionsJson, String photosJson, String reportUrl) {
        this.certificationId = certificationId;
        this.certificationName = certificationName;
        this.farmId = farmId;
        this.requirementCategory = requirementCategory;
        this.inspectionType = inspectionType;
        this.inspectionDate = inspectionDate;
        this.inspectorName = inspectorName;
        this.inspectorBody = inspectorBody;
        this.status = status;
        this.remarks = remarks;
        this.correctiveActionsJson = correctiveActionsJson;
        this.photosJson = photosJson;
        this.reportUrl = reportUrl;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
    }

    // Convenience constructor with List parameters (for code use) - Ignored by Room
    @Ignore
    public InspectionEntity(String certificationId, String certificationName, long farmId,
                            RequirementCategory requirementCategory, InspectionType inspectionType,
                            long inspectionDate, String inspectorName, String inspectorBody,
                            InspectionStatus status, String remarks,
                            List<CorrectiveAction> correctiveActions,
                            List<String> photos, String reportUrl) {
        this.certificationId = certificationId;
        this.certificationName = certificationName;
        this.farmId = farmId;
        this.requirementCategory = requirementCategory;
        this.inspectionType = inspectionType;
        this.inspectionDate = inspectionDate;
        this.inspectorName = inspectorName;
        this.inspectorBody = inspectorBody;
        this.status = status;
        this.remarks = remarks;
        setCorrectiveActions(correctiveActions);
        setPhotos(photos);
        this.reportUrl = reportUrl;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
    }

    // Getters and Setters
    @NonNull
    public String getInspectionId() { return inspectionId; }
    public void setInspectionId(@NonNull String inspectionId) {
        this.inspectionId = inspectionId;
    }

    public String getCertificationId() { return certificationId; }
    public void setCertificationId(String certificationId) {
        this.certificationId = certificationId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCertificationName() { return certificationName; }
    public void setCertificationName(String certificationName) {
        this.certificationName = certificationName;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getSupersedesId() { return supersedesId; }
    public void setSupersedesId(String supersedesId) {
        this.supersedesId = supersedesId;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public RequirementCategory getRequirementCategory() { return requirementCategory; }
    public void setRequirementCategory(RequirementCategory requirementCategory) {
        this.requirementCategory = requirementCategory;
        this.updatedAt = System.currentTimeMillis();
    }

    public InspectionType getInspectionType() { return inspectionType; }
    public void setInspectionType(InspectionType inspectionType) {
        this.inspectionType = inspectionType;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(long inspectionDate) {
        this.inspectionDate = inspectionDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) {
        this.inspectorName = inspectorName;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getInspectorBody() { return inspectorBody; }
    public void setInspectorBody(String inspectorBody) {
        this.inspectorBody = inspectorBody;
        this.updatedAt = System.currentTimeMillis();
    }

    public InspectionStatus getStatus() { return status; }
    public void setStatus(InspectionStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) {
        this.remarks = remarks;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCorrectiveActionsJson() { return correctiveActionsJson; }
    public void setCorrectiveActionsJson(String correctiveActionsJson) {
        this.correctiveActionsJson = correctiveActionsJson;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPhotosJson() { return photosJson; }
    public void setPhotosJson(String photosJson) {
        this.photosJson = photosJson;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getReportUrl() { return reportUrl; }
    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    // Helper methods for JSON serialization
    public List<CorrectiveAction> getCorrectiveActions() {
        if (correctiveActions == null && correctiveActionsJson != null) {
            Type listType = new TypeToken<List<CorrectiveAction>>(){}.getType();
            correctiveActions = new Gson().fromJson(correctiveActionsJson, listType);
        }
        if (correctiveActions == null) {
            correctiveActions = new ArrayList<>();
        }
        return correctiveActions;
    }

    public void setCorrectiveActions(List<CorrectiveAction> correctiveActions) {
        this.correctiveActions = correctiveActions;
        if (correctiveActions != null && !correctiveActions.isEmpty()) {
            this.correctiveActionsJson = new Gson().toJson(correctiveActions);
        } else {
            this.correctiveActionsJson = null;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    public List<String> getPhotos() {
        if (photos == null && photosJson != null) {
            Type listType = new TypeToken<List<String>>(){}.getType();
            photos = new Gson().fromJson(photosJson, listType);
        }
        if (photos == null) {
            photos = new ArrayList<>();
        }
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
        if (photos != null && !photos.isEmpty()) {
            this.photosJson = new Gson().toJson(photos);
        } else {
            this.photosJson = null;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    // Helper methods
    public String getFormattedInspectionDate() {
        if (inspectionDate <= 0) return "Not set";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(inspectionDate));
    }

    // ✅ Fixed: Return just the display name (without icon)
    public String getStatusDisplay() {
        return status.getDisplayName();
    }

    // ✅ Add this method to get status icon resource ID
    public int getStatusIconResId() {
        return status.getIconResId();
    }

    // ✅ Fixed: Return just the display name (without icon)
    public String getRequirementCategoryDisplay() {
        return requirementCategory.getDisplayName();
    }

    // ✅ Add this method to get category icon resource ID
    public int getRequirementCategoryIconResId() {
        return requirementCategory.getIconResId();
    }

    public int getCorrectiveActionsCount() {
        return getCorrectiveActions().size();
    }

    public int getOpenCorrectiveActionsCount() {
        int count = 0;
        for (CorrectiveAction action : getCorrectiveActions()) {
            if ("OPEN".equals(action.status) || action.status == null) {
                count++;
            }
        }
        return count;
    }

    public boolean hasPhotos() {
        return !getPhotos().isEmpty();
    }
}