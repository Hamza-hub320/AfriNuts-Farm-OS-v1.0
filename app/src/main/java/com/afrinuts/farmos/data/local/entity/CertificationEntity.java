package com.afrinuts.farmos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.utils.DateFormatter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "certifications",
        foreignKeys = @ForeignKey(
                entity = FarmEntity.class,
                parentColumns = "id",
                childColumns = "farmId",
                onDelete = ForeignKey.CASCADE
        ))
public class CertificationEntity {

    @PrimaryKey
    @NonNull
    private String certificationId;  // CERT-ORG-2026-001

    private long farmId;

    // Basic Info
    private String certificationName; // Organic (ECOCERT)
    private String certificationBody; // ECOCERT
    private CertificationStatus status; // IN_PROGRESS, CERTIFIED, EXPIRED
    private String scope; // Farm operations, processing facility

    // Store as JSON string in database
    private String applicableStandardsJson; // JSON array of standards
    private String statusHistoryJson;

    // Dates
    private long targetDate; // Planned certification date
    private long approvalDate; // Date approved
    private long expiryDate; // Certification expiry date

    // Audit timestamps
    private long createdAt;
    private long updatedAt;
    private boolean isSynced;

    // Transient field (not stored in database)
    @Ignore
    private List<String> applicableStandards;
    @Ignore
    private List<StatusEvent> statusHistory;

    public enum CertificationStatus {
        NOT_STARTED("Not Started", R.drawable.ic_circle_empty),
        IN_PROGRESS("In Progress", R.drawable.ic_progress_clock),
        AUDIT_READY("Audit Ready", R.drawable.ic_assignment_ready),
        CERTIFIED("Certified", R.drawable.ic_check_circle),
        EXPIRED("Expired", R.drawable.ic_cancel_expired),
        SUSPENDED("Suspended", R.drawable.ic_warning_suspended);

        private final String displayName;
        private final int iconResId;

        CertificationStatus(String displayName, int iconResId) {
            this.displayName = displayName;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }

        public static CertificationStatus fromDisplayName(String displayName) {
            for (CertificationStatus status : values()) {
                if (status.getDisplayName().equals(displayName)) {
                    return status;
                }
            }
            return NOT_STARTED;
        }
    }

    // Add these helper methods to CertificationEntity.java

    public boolean hasApprovalDate() {
        return approvalDate > 0;
    }

    public boolean hasExpiryDate() {
        return expiryDate > 0;
    }

    public boolean isFullyCertified() {
        return status == CertificationStatus.CERTIFIED && hasApprovalDate() && hasExpiryDate();
    }

    public String getApprovalStatus() {
        if (hasApprovalDate()) {
            return "Approved on " + getFormattedApprovalDate();
        }
        return "Not yet approved";
    }

    // Constructor for Room - always generates an ID
    public CertificationEntity(long farmId, String certificationName, String certificationBody,
                               CertificationStatus status, String scope, String applicableStandardsJson,
                               long targetDate, long approvalDate, long expiryDate) {
        // Generate ID if not already set
        this.farmId = farmId;
        this.certificationName = certificationName;
        this.certificationBody = certificationBody;
        this.status = status;
        this.scope = scope;
        this.applicableStandardsJson = applicableStandardsJson;
        this.targetDate = targetDate;
        this.approvalDate = approvalDate;
        this.expiryDate = expiryDate;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
        this.applicableStandards = parseStandardsFromJson(applicableStandardsJson);
    }

    // Convenience constructor for UI
    @Ignore
    public CertificationEntity(long farmId, String certificationName, String certificationBody,
                               CertificationStatus status, String scope, List<String> applicableStandards,
                               long targetDate, long approvalDate, long expiryDate) {

        this.farmId = farmId;
        this.certificationName = certificationName;
        this.certificationBody = certificationBody;
        this.status = status;
        this.scope = scope;
        setApplicableStandards(applicableStandards);
        this.targetDate = targetDate;
        this.approvalDate = approvalDate;
        this.expiryDate = expiryDate;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
    }

    public void setCertificationId(@NonNull String certificationId) {
        this.certificationId = certificationId;
    }

    // Helper method to parse JSON to List
    private List<String> parseStandardsFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<List<String>>(){}.getType();
            return new Gson().fromJson(json, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Helper method to convert List to JSON
    private String standardsListToJson(List<String> standards) {
        if (standards == null || standards.isEmpty()) {
            return null;
        }
        return new Gson().toJson(standards);
    }

    private List<StatusEvent> parseStatusHistoryFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<List<StatusEvent>>(){}.getType();
            return new Gson().fromJson(json, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String statusHistoryListToJson(List<StatusEvent> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        return new Gson().toJson(events);
    }

    public String getStatusHistoryJson() { return statusHistoryJson; }

    public List<StatusEvent> getStatusHistory() {
        if (statusHistory == null && statusHistoryJson != null) {
            statusHistory = parseStatusHistoryFromJson(statusHistoryJson);
        }
        if (statusHistory == null) statusHistory = new ArrayList<>();
        return statusHistory;
    }

    public void appendStatusEvent(String fromStatus, String toStatus,
                                  String actor, String documentId) {
        List<StatusEvent> events = getStatusHistory();
        events.add(new StatusEvent(fromStatus, toStatus, System.currentTimeMillis(),
                actor, documentId));
        this.statusHistory = events;
        this.statusHistoryJson = statusHistoryListToJson(events);
        this.updatedAt = System.currentTimeMillis();
    }

    public void setStatusHistoryJson(String statusHistoryJson) {
        this.statusHistoryJson = statusHistoryJson;
        if (statusHistoryJson != null) {
            this.statusHistory = parseStatusHistoryFromJson(statusHistoryJson);
        }
    }

    // Getters and Setters
    public String getCertificationId() { return certificationId; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public String getCertificationName() { return certificationName; }
    public void setCertificationName(String certificationName) {
        this.certificationName = certificationName;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCertificationBody() { return certificationBody; }
    public void setCertificationBody(String certificationBody) {
        this.certificationBody = certificationBody;
        this.updatedAt = System.currentTimeMillis();
    }

    public CertificationStatus getStatus() { return status; }
    public void setStatus(CertificationStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public static class StatusEvent {
        public String fromStatus;   // enum name; null for the creation event
        public String toStatus;     // enum name
        public long date;
        public String actor;
        public String documentId;   // null in Stage 1

        public StatusEvent() {}

        public StatusEvent(String fromStatus, String toStatus, long date,
                           String actor, String documentId) {
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.date = date;
            this.actor = actor;
            this.documentId = documentId;
        }
    }

    public String getScope() { return scope; }
    public void setScope(String scope) {
        this.scope = scope;
        this.updatedAt = System.currentTimeMillis();
    }

    public List<String> getApplicableStandards() {
        if (applicableStandards == null && applicableStandardsJson != null) {
            applicableStandards = parseStandardsFromJson(applicableStandardsJson);
        }
        if (applicableStandards == null) {
            applicableStandards = new ArrayList<>();
        }
        return applicableStandards;
    }

    public void setApplicableStandards(List<String> applicableStandards) {
        this.applicableStandards = applicableStandards;
        this.applicableStandardsJson = standardsListToJson(applicableStandards);
        this.updatedAt = System.currentTimeMillis();
    }

    public String getApplicableStandardsJson() { return applicableStandardsJson; }
    public void setApplicableStandardsJson(String applicableStandardsJson) {
        this.applicableStandardsJson = applicableStandardsJson;
        // Parse the JSON back to List when set
        if (applicableStandardsJson != null) {
            this.applicableStandards = parseStandardsFromJson(applicableStandardsJson);
        }
    }

    public long getTargetDate() { return targetDate; }
    public void setTargetDate(long targetDate) {
        this.targetDate = targetDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getApprovalDate() { return approvalDate; }
    public void setApprovalDate(long approvalDate) {
        this.approvalDate = approvalDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getExpiryDate() { return expiryDate; }
    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    // Helper methods
    public String getFormattedTargetDate() {
        return DateFormatter.formatDateOrDash(targetDate);
    }

    public String getFormattedApprovalDate() {
        return DateFormatter.formatDateOrDash(approvalDate);
    }

    public String getFormattedExpiryDate() {
        return DateFormatter.formatDateOrDash(expiryDate);
    }

    public String getStatusDisplay() {
        return status.getDisplayName();
    }

    public String getStandardsDisplay() {
        List<String> standards = getApplicableStandards();
        if (standards.isEmpty()) return "No standards set";
        return String.join(", ", standards);
    }

    public boolean isExpired() {
        return status == CertificationStatus.CERTIFIED
                && expiryDate > 0
                && expiryDate < System.currentTimeMillis();
    }

    /**
     * Effective status. EXPIRED is derived: stored CERTIFIED with a real
     * expiry in the past reads as EXPIRED everywhere the status is shown or
     * consumed. The stored value is never rewritten by this.
     */
    public CertificationStatus getEffectiveStatus() {
        if (status == CertificationStatus.CERTIFIED
                && expiryDate > 0
                && expiryDate < System.currentTimeMillis()) {
            return CertificationStatus.EXPIRED;
        }
        return status;
    }

    public String getEffectiveStatusDisplay() {
        return getEffectiveStatus().getDisplayName();
    }

    public boolean isActive() {
        CertificationStatus effective = getEffectiveStatus();
        return effective == CertificationStatus.CERTIFIED
                || effective == CertificationStatus.IN_PROGRESS
                || effective == CertificationStatus.AUDIT_READY;
    }
    public boolean isExpiringSoon() {
        if (!isActive()) return false;
        long daysUntilExpiry = (expiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000);
        return daysUntilExpiry < 90 && daysUntilExpiry > 0;
    }
}