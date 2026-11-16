package com.afrinuts.farmos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
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

@Entity(tableName = "compliance_verifications",
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
        },
        indices = {
                @Index(value = {"validatedAt"})
        })
public class VerificationEntity {

    @PrimaryKey
    @NonNull
    private String verificationId; // VER-ORG-2026-012
    private Long validatedAt; // null = not yet validated
    private String validatedBy;
    private String supersedesId; // null unless this record corrects another record (its ID)

    private String certificationId;
    private long farmId;

    private VerificationType verificationType; // DOCUMENTATION, FIELD, LABORATORY, SURVEILLANCE
    private long verificationDate;
    private String verifiedBy;
    private VerificationStatus status; // VERIFIED, PENDING, REJECTED
    private String evidenceJson; // JSON array of evidence URLs
    private String comment;

    private long createdAt;
    private long updatedAt;
    private boolean isSynced;

    @Ignore
    private List<String> evidence;

    public enum VerificationType {
        DOCUMENTATION("Documentation", R.drawable.ic_description, "Documentation review"),
        FIELD("Field", R.drawable.ic_grass, "Field inspection and verification"),
        LABORATORY("Laboratory", R.drawable.ic_science, "Laboratory testing and analysis"),
        SURVEILLANCE("Surveillance", R.drawable.ic_visibility, "Surveillance verification");

        private final String displayName;
        private final int iconResId;
        private final String description;

        VerificationType(String displayName, int iconResId, String description) {
            this.displayName = displayName;
            this.iconResId = iconResId;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }
        public String getDescription() { return description; }
    }

    public enum VerificationStatus {
        VERIFIED("Verified", R.drawable.ic_check_circle),
        PENDING("Pending", R.drawable.ic_progress_clock),
        REJECTED("Rejected", R.drawable.ic_cancel_expired);

        private final String displayName;
        private final int iconResId;

        VerificationStatus(String displayName, int iconResId) {
            this.displayName = displayName;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public int getIconResId() { return iconResId; }
    }

    // Default constructor for Room
    public VerificationEntity() {
        // Required by Room
    }

    // Constructor for Room (fields match directly)
    public VerificationEntity(String certificationId, long farmId, VerificationType verificationType,
                              long verificationDate, String verifiedBy, VerificationStatus status,
                              String evidenceJson, String comment) {
        this.certificationId = certificationId;
        this.farmId = farmId;
        this.verificationType = verificationType;
        this.verificationDate = verificationDate;
        this.verifiedBy = verifiedBy;
        this.status = status;
        this.evidenceJson = evidenceJson;
        this.comment = comment;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
    }

    // Convenience constructor with List parameter (for code use) - Ignored by Room
    @Ignore
    public VerificationEntity(String certificationId, long farmId, VerificationType verificationType,
                              long verificationDate, String verifiedBy, VerificationStatus status,
                              List<String> evidence, String comment) {
        this.certificationId = certificationId;
        this.farmId = farmId;
        this.verificationType = verificationType;
        this.verificationDate = verificationDate;
        this.verifiedBy = verifiedBy;
        this.status = status;
        setEvidence(evidence);
        this.comment = comment;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
    }

    // Getters and Setters
    @NonNull
    public String getVerificationId() { return verificationId; }
    public void setVerificationId(@NonNull String verificationId) { this.verificationId = verificationId; }

    public String getCertificationId() { return certificationId; }
    public void setCertificationId(String certificationId) {
        this.certificationId = certificationId;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public VerificationType getVerificationType() { return verificationType; }
    public void setVerificationType(VerificationType verificationType) {
        this.verificationType = verificationType;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getVerificationDate() { return verificationDate; }
    public void setVerificationDate(long verificationDate) {
        this.verificationDate = verificationDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getSupersedesId() { return supersedesId; }
    public void setSupersedesId(String supersedesId) {
        this.supersedesId = supersedesId;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getValidatedAt() { return validatedAt; }
    public void setValidatedAt(Long validatedAt) {
        this.validatedAt = validatedAt;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) {
        this.validatedBy = validatedBy;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
        this.updatedAt = System.currentTimeMillis();
    }

    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getComment() { return comment; }
    public void setComment(String comment) {
        this.comment = comment;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    // Helper methods for JSON serialization
    public List<String> getEvidence() {
        if (evidence == null && evidenceJson != null) {
            Type listType = new TypeToken<List<String>>(){}.getType();
            evidence = new Gson().fromJson(evidenceJson, listType);
        }
        if (evidence == null) {
            evidence = new ArrayList<>();
        }
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
        if (evidence != null && !evidence.isEmpty()) {
            this.evidenceJson = new Gson().toJson(evidence);
        } else {
            this.evidenceJson = null;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    // Helper methods
    public String getFormattedVerificationDate() {
        if (verificationDate <= 0) return "Not set";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(verificationDate));
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
    public String getVerificationTypeDisplay() {
        return verificationType.getDisplayName();
    }

    // ✅ Add this method to get verification type icon resource ID
    public int getVerificationTypeIconResId() {
        return verificationType.getIconResId();
    }

    public int getEvidenceCount() {
        return getEvidence().size();
    }

    public boolean hasEvidence() {
        return !getEvidence().isEmpty();
    }
}