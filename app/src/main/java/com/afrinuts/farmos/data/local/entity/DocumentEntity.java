package com.afrinuts.farmos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.afrinuts.farmos.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "compliance_documents",
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
public class DocumentEntity {

    @PrimaryKey
    @NonNull
    private String documentId; // DOC-ORG-2026-001

    private String certificationId;
    private long farmId;

    private DocumentType documentType;
    private String documentName;
    private long issueDate;
    private long expiryDate;
    private String fileUrl;
    private DocumentStatus status;

    private long createdAt;
    private long updatedAt;
    private boolean isSynced;

    public enum DocumentType {
        CERTIFICATE("Certificate", R.drawable.ic_certificate, "Certification document"),
        AUDIT_REPORT("Audit Report", R.drawable.ic_description, "Audit report document"),
        INPUT_CERTIFICATE("Input Certificate", R.drawable.ic_input, "Input certificate document"),
        INSPECTION_REPORT("Inspection Report", R.drawable.ic_visibility, "Inspection report document"),
        VERIFICATION("Verification", R.drawable.ic_verified, "Verification document"),
        APPLICATION("Application", R.drawable.ic_edit_document, "Application document");

        private final String displayName;
        private final int iconResId;
        private final String description;

        DocumentType(String displayName, int iconResId, String description) {
            this.displayName = displayName;
            this.iconResId = iconResId;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }
        public String getDescription() { return description; }
    }

    public enum DocumentStatus {
        DRAFT("Draft", R.drawable.ic_draft, "Document is in draft"),
        PENDING("Pending", R.drawable.ic_pending, "Document is pending approval"),
        APPROVED("Approved", R.drawable.ic_check_circle, "Document is approved");

        private final String displayName;
        private final int iconResId;
        private final String description;

        DocumentStatus(String displayName, int iconResId, String description) {
            this.displayName = displayName;
            this.iconResId = iconResId;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }
        public String getDescription() { return description; }
    }

    // Default constructor for Room
    public DocumentEntity() {
        // Required by Room
    }

    // Constructor
    public DocumentEntity(String certificationId, long farmId, DocumentType documentType,
                          String documentName, long issueDate, long expiryDate,
                          String fileUrl, DocumentStatus status) {
        this.certificationId = certificationId;
        this.farmId = farmId;
        this.documentType = documentType;
        this.documentName = documentName;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.fileUrl = fileUrl;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
    }

    // Getters and Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getCertificationId() { return certificationId; }
    public void setCertificationId(String certificationId) {
        this.certificationId = certificationId;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) {
        this.documentName = documentName;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getIssueDate() { return issueDate; }
    public void setIssueDate(long issueDate) {
        this.issueDate = issueDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getExpiryDate() { return expiryDate; }
    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
        this.updatedAt = System.currentTimeMillis();
    }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    // Helper methods
    public String getFormattedIssueDate() {
        if (issueDate <= 0) return "Not set";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(issueDate));
    }

    public String getFormattedExpiryDate() {
        if (expiryDate <= 0) return "Not set";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(expiryDate));
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
    public String getDocumentTypeDisplay() {
        return documentType.getDisplayName();
    }

    // ✅ Add this method to get document type icon resource ID
    public int getDocumentTypeIconResId() {
        return documentType.getIconResId();
    }

    public boolean isExpired() {
        if (expiryDate <= 0) return false;
        return System.currentTimeMillis() > expiryDate;
    }

    public boolean isExpiringSoon() {
        if (expiryDate <= 0) return false;
        long daysUntilExpiry = (expiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000);
        return daysUntilExpiry < 90 && daysUntilExpiry > 0;
    }

    public boolean isActive() {
        return status == DocumentStatus.APPROVED && !isExpired();
    }
}