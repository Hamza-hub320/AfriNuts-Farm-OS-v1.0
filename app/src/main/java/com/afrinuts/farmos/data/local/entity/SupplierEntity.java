package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "suppliers",
        foreignKeys = {
                @ForeignKey(
                        entity = FarmEntity.class,
                        parentColumns = "id",
                        childColumns = "farmId",
                        onDelete = ForeignKey.CASCADE
                )
        })
public class SupplierEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;

    // Supplier identification
    private String name;
    private SupplierType type;
    private String contactPerson;
    private String phone;
    private String email;
    private String location;
    private String region;

    // Supplier details
    private String registrationNumber;
    private boolean isCertified;
    private String certificationType; // Organic, GLOBALG.A.P., etc.
    private double annualVolumeKg;
    private double averageQualityScore;

    // Supplier rating
    private int rating; // 1-5 stars
    private String notes;

    // Active status
    private boolean isActive;

    // Audit timestamps
    private long createdAt;
    private long updatedAt;

    public enum SupplierType {
        COOPERATIVE("Cooperative", "🤝", "Farmer cooperative"),
        INDEPENDENT_FARMER("Independent Farmer", "👨‍🌾", "Individual farmer"),
        REGIONAL_SUPPLIER("Regional Supplier", "📦", "Commercial supplier"),
        COMMERCIAL_AGGREGATOR("Commercial Aggregator", "🏢", "Large-scale supplier"),
        PROCESSOR("Processor", "🏭", "Processing facility");

        private final String displayName;
        private final String icon;
        private final String description;

        SupplierType(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    // Constructor
    public SupplierEntity(long farmId, String name, SupplierType type,
                          String contactPerson, String phone, String location) {
        this.farmId = farmId;
        this.name = name;
        this.type = type;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.location = location;
        this.isActive = true;
        this.rating = 3;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SupplierType getType() { return type; }
    public void setType(SupplierType type) { this.type = type; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public boolean isCertified() { return isCertified; }
    public void setCertified(boolean certified) { isCertified = certified; }

    public String getCertificationType() { return certificationType; }
    public void setCertificationType(String certificationType) { this.certificationType = certificationType; }

    public double getAnnualVolumeKg() { return annualVolumeKg; }
    public void setAnnualVolumeKg(double annualVolumeKg) { this.annualVolumeKg = annualVolumeKg; }

    public double getAverageQualityScore() { return averageQualityScore; }
    public void setAverageQualityScore(double averageQualityScore) { this.averageQualityScore = averageQualityScore; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public String getFormattedRating() {
        return "★".repeat(Math.max(0, rating));
    }

    public String getStatusDisplay() {
        return isActive ? "Active" : "Inactive";
    }

    public String getTypeDisplay() {
        return type.getIcon() + " " + type.getDisplayName();
    }
}