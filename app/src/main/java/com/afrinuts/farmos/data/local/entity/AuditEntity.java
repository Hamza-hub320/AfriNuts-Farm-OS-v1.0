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

@Entity(tableName = "audits",
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
public class AuditEntity {

    @NonNull
    @PrimaryKey
    private String auditId; // AUDIT-ORG-2026-001
    private String supersedesId; // null unless this record corrects another record (its ID)

    private String certificationId; // Links to CertificationEntity
    private long farmId;

    private AuditType auditType; // INTERNAL, EXTERNAL, SURVEILLANCE, RENEWAL
    private long auditDate;
    private String auditorName;
    private String auditorBody;
    private String scope;
    private String findingsJson; // JSON array of findings
    private AuditStatus status; // PASS, PASS_WITH_CONDITIONS, FAIL
    private String reportUrl;

    private long createdAt;
    private long updatedAt;
    private boolean isSynced;

    @Ignore
    private List<Finding> findings;

    public enum AuditType {
        INTERNAL("Internal", "Internal audit by company team"),
        EXTERNAL("External", "External audit by certification body"),
        SURVEILLANCE("Surveillance", "Surveillance audit between certification cycles"),
        RENEWAL("Renewal", "Renewal audit for certification renewal");

        private final String displayName;
        private final String description;

        AuditType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public enum AuditStatus {
        PASS("Pass", R.drawable.ic_check_circle),
        PASS_WITH_CONDITIONS("Pass with Conditions", R.drawable.ic_warning_suspended),
        FAIL("Fail", R.drawable.ic_cancel_expired);

        private final String displayName;
        private final int iconResId;

        AuditStatus(String displayName, int iconResId) {
            this.displayName = displayName;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }
    }

    // Finding class
    public static class Finding {
        public String finding;
        public String correctiveAction;
        public String status; // "OPEN", "IN_PROGRESS", "CLOSED"
        public String assignedTo;
        public long dueDate;

        public Finding() {
            // Default constructor
        }

        public Finding(String finding, String correctiveAction) {
            this.finding = finding;
            this.correctiveAction = correctiveAction;
            this.status = "OPEN";
        }

        public String getFormattedDueDate() {
            if (dueDate <= 0) return "Not set";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(dueDate));
        }
    }

    // Default constructor for Room
    public AuditEntity() {
        // Required for Room
    }

    // Constructor for creating new audit (without findings list)
    public AuditEntity(String certificationId, long farmId, AuditType auditType,
                       long auditDate, String auditorName, String auditorBody,
                       String scope, AuditStatus status, String reportUrl) {
        this.certificationId = certificationId;
        this.farmId = farmId;
        this.auditType = auditType;
        this.auditDate = auditDate;
        this.auditorName = auditorName;
        this.auditorBody = auditorBody;
        this.scope = scope;
        this.status = status;
        this.reportUrl = reportUrl;
        this.findings = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
    }

    // Constructor with findings (for internal use)
    @Ignore
    public AuditEntity(String certificationId, long farmId, AuditType auditType,
                       long auditDate, String auditorName, String auditorBody,
                       String scope, List<Finding> findings, AuditStatus status,
                       String reportUrl) {
        this(certificationId, farmId, auditType, auditDate, auditorName,
                auditorBody, scope, status, reportUrl);
        setFindings(findings);
    }

    // Getters and Setters
    @NonNull
    public String getAuditId() { return auditId; }

    public void setAuditId(@NonNull String auditId) {
        this.auditId = auditId;
    }

    public String getCertificationId() { return certificationId; }
    public void setCertificationId(String certificationId) {
        this.certificationId = certificationId;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public AuditType getAuditType() { return auditType; }
    public void setAuditType(AuditType auditType) {
        this.auditType = auditType;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getSupersedesId() { return supersedesId; }
    public void setSupersedesId(String supersedesId) {
        this.supersedesId = supersedesId;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getAuditDate() { return auditDate; }
    public void setAuditDate(long auditDate) {
        this.auditDate = auditDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAuditorName() { return auditorName; }
    public void setAuditorName(String auditorName) {
        this.auditorName = auditorName;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAuditorBody() { return auditorBody; }
    public void setAuditorBody(String auditorBody) {
        this.auditorBody = auditorBody;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getScope() { return scope; }
    public void setScope(String scope) {
        this.scope = scope;
        this.updatedAt = System.currentTimeMillis();
    }

    public List<Finding> getFindings() {
        if (findings == null && findingsJson != null) {
            Type listType = new TypeToken<List<Finding>>(){}.getType();
            findings = new Gson().fromJson(findingsJson, listType);
        }
        if (findings == null) {
            findings = new ArrayList<>();
        }
        return findings;
    }

    public void setFindings(List<Finding> findings) {
        this.findings = findings;
        if (findings != null && !findings.isEmpty()) {
            this.findingsJson = new Gson().toJson(findings);
        } else {
            this.findingsJson = null;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    public String getFindingsJson() { return findingsJson; }
    public void setFindingsJson(String findingsJson) {
        this.findingsJson = findingsJson;
    }

    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) {
        this.status = status;
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

    // Helper methods
    public String getFormattedAuditDate() {
        if (auditDate <= 0) return "Not set";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(auditDate));
    }

    // ✅ Fixed: Return just the display name (without icon)
    public String getStatusDisplay() {
        return status.getDisplayName();
    }

    // ✅ Add this method to get icon resource ID
    public int getStatusIconResId() {
        return status.getIconResId();
    }

    public String getAuditTypeDisplay() {
        return auditType.getDisplayName();
    }

    public int getFindingsCount() {
        return getFindings().size();
    }

    public int getOpenFindingsCount() {
        int count = 0;
        for (Finding finding : getFindings()) {
            if ("OPEN".equals(finding.status) || finding.status == null) {
                count++;
            }
        }
        return count;
    }

    public boolean hasFindings() {
        return !getFindings().isEmpty();
    }
}