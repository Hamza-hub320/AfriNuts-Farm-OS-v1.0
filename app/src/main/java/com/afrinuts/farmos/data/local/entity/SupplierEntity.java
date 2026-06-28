package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Locale;

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

    // Basic Info
    private String name;
    private SupplierType type;
    private String contactPerson;
    private String phone;
    private String email;
    private String location;
    private String region;
    private String country;
    private double latitude;
    private double longitude;
    private boolean hasLocation;
    private String photoUrl;
    private int yearsFarming;
    private String bio;
    private String quote;
    private double farmSize;
    private boolean firebreakProtection;
    private boolean sustainableHarvesting;
    private boolean noChildLabor;


    // Business Details
    private String registrationNumber;
    private String taxId;
    private double annualVolumeKg;
    private double averageQualityScore;
    private int rating; // 1-5

    // Certification
    private String certificationType;
    private String certificationNumber;
    private long certificationExpiryDate;
    private boolean isCertified;

    // Status
    private SupplierStatus status;
    private boolean isActive;
    private String notes;

    // Audit timestamps
    private long createdAt;
    private long updatedAt;

    // Sync support
    private boolean isSynced;

    public enum SupplierType {
        COOPERATIVE("Cooperative", "Farmer cooperative organization"),
        INDEPENDENT_FARMER("Independent Farmer", "Individual smallholder farmer"),
        REGIONAL_SUPPLIER("Regional Supplier", "Regional commercial supplier"),
        COMMERCIAL_AGGREGATOR("Commercial Aggregator", "Large-scale commercial supplier"),
        PROCESSOR("Processor", "Processing facility");

        private final String displayName;
        private final String description;

        SupplierType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public enum SupplierStatus {
        ACTIVE("Active", "Currently supplying"),
        PENDING("Pending", "Awaiting approval"),
        SUSPENDED("Suspended", "Temporarily suspended"),
        INACTIVE("Inactive", "No longer supplying");

        private final String displayName;
        private final String description;

        SupplierStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // Delivery record
    public static class Delivery {
        private long deliveryDate;
        private String batchNumber;
        private double quantityKg;
        private String grade;
        private double pricePerKg;
        private double totalAmount;
        private String notes;

        public Delivery(long deliveryDate, String batchNumber, double quantityKg,
                        String grade, double pricePerKg, String notes) {
            this.deliveryDate = deliveryDate;
            this.batchNumber = batchNumber;
            this.quantityKg = quantityKg;
            this.grade = grade;
            this.pricePerKg = pricePerKg;
            this.totalAmount = quantityKg * pricePerKg;
            this.notes = notes;
        }

        public long getDeliveryDate() { return deliveryDate; }
        public void setDeliveryDate(long deliveryDate) { this.deliveryDate = deliveryDate; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

        public double getQuantityKg() { return quantityKg; }
        public void setQuantityKg(double quantityKg) {
            this.quantityKg = quantityKg;
            this.totalAmount = quantityKg * pricePerKg;
        }

        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }

        public double getPricePerKg() { return pricePerKg; }
        public void setPricePerKg(double pricePerKg) {
            this.pricePerKg = pricePerKg;
            this.totalAmount = quantityKg * pricePerKg;
        }

        public double getTotalAmount() { return totalAmount; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getFormattedDate() {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(deliveryDate));
        }

        public String getFormattedAmount() {
            return String.format(java.util.Locale.getDefault(), "%,.0f XAF", totalAmount);
        }

        public String getFormattedQuantity() {
            return String.format(java.util.Locale.getDefault(), "%,.0f kg", quantityKg);
        }
    }

    // Constructor
    public SupplierEntity(long farmId, String name, SupplierType type,
                          String contactPerson, String phone, String location, String registrationNumber) {
        this.farmId = farmId;
        this.name = name;
        this.type = type;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.location = location;
        this.registrationNumber = registrationNumber;
        this.status = SupplierStatus.PENDING;
        this.isActive = true;
        this.rating = 3;
        this.isSynced = false;

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

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public double getAnnualVolumeKg() { return annualVolumeKg; }
    public void setAnnualVolumeKg(double annualVolumeKg) { this.annualVolumeKg = annualVolumeKg; }

    public double getAverageQualityScore() { return averageQualityScore; }
    public void setAverageQualityScore(double averageQualityScore) { this.averageQualityScore = averageQualityScore; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getCertificationType() { return certificationType; }
    public void setCertificationType(String certificationType) { this.certificationType = certificationType; }

    public String getCertificationNumber() { return certificationNumber; }
    public void setCertificationNumber(String certificationNumber) { this.certificationNumber = certificationNumber; }

    public long getCertificationExpiryDate() { return certificationExpiryDate; }
    public void setCertificationExpiryDate(long certificationExpiryDate) { this.certificationExpiryDate = certificationExpiryDate; }

    public boolean isCertified() { return isCertified; }
    public void setCertified(boolean certified) { isCertified = certified; }

    public SupplierStatus getStatus() { return status; }
    public void setStatus(SupplierStatus status) { this.status = status; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
        this.hasLocation = true;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
        this.hasLocation = true;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public int getYearsFarming() { return yearsFarming; }
    public void setYearsFarming(int yearsFarming) { this.yearsFarming = yearsFarming; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getQuote() { return quote; }
    public void setQuote(String quote) { this.quote = quote; }

    public double getFarmSize() { return farmSize; }
    public void setFarmSize(double farmSize) { this.farmSize = farmSize; }

    public boolean isFirebreakProtection() { return firebreakProtection; }
    public void setFirebreakProtection(boolean firebreakProtection) { this.firebreakProtection = firebreakProtection; }

    public boolean isSustainableHarvesting() { return sustainableHarvesting; }
    public void setSustainableHarvesting(boolean sustainableHarvesting) { this.sustainableHarvesting = sustainableHarvesting; }

    public boolean isNoChildLabor() { return noChildLabor; }
    public void setNoChildLabor(boolean noChildLabor) { this.noChildLabor = noChildLabor; }

    // Helper method to get formatted years farming
    public String getFormattedYearsFarming() {
        if (yearsFarming <= 0) return "New Farmer";
        return yearsFarming + "+ years farming";
    }

    public boolean hasLocation() { return hasLocation; }
    public void setHasLocation(boolean hasLocation) { this.hasLocation = hasLocation; }

    public String getFormattedCoordinates() {
        if (!hasLocation) return "No location data";
        return String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude);
    }

    public String getGoogleMapsLink() {
        if (!hasLocation) return null;
        return "https://www.google.com/maps?q=" + latitude + "," + longitude;
    }

    // Helper methods
    public String getFormattedRating() {
        return "★".repeat(Math.max(0, rating));
    }

    public String getStatusDisplay() {
        return status.getDisplayName();
    }

    public String getTypeDisplay() {
        return type.getDisplayName();
    }

    public boolean isCertificationExpiring() {
        if (certificationExpiryDate <= 0) return false;
        long daysUntilExpiry = (certificationExpiryDate - System.currentTimeMillis()) /
                (24 * 60 * 60 * 1000);
        return daysUntilExpiry < 30 && daysUntilExpiry > 0;
    }

    public boolean isCertificationExpired() {
        if (certificationExpiryDate <= 0) return false;
        return System.currentTimeMillis() > certificationExpiryDate;
    }
}