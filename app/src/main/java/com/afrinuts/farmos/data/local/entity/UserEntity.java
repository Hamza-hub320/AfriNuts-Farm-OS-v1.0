package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    // Firebase Auth UID (String)
    private String uid;
    // Local Room auto-increment ID
    @PrimaryKey(autoGenerate = true)
    private long localId;

    private String name;
    private UserRole role;
    private String phone;
    private String email;

    // Authentication fields
    private String passwordHash;
    private String pinHash;
    private AuthMethod authMethod;

    // ✅ New Security & Authentication Fields
    private long lastLoginAt;
    private String lastLoginIp;
    private String lastLoginDevice;
    private boolean emailVerified;
    private long passwordChangedAt;
    private int failedLoginAttempts;
    private long lockedUntil;
    private boolean twoFactorEnabled;
    private String twoFactorSecret;

    // ✅ Account Status Fields
    private long lastUpdatedAt;
    private long deactivatedAt;
    private String deactivatedBy;
    private String deactivationReason;

    // ✅ Profile Fields
    private String bio;
    private String company;
    private String title;
    private String timezone;

    // Existing fields
    private long createdAt;
    private long updatedAt;
    private boolean isActive;
    private long farmId;

    public enum UserRole {
        OWNER("Owner", "Full access to all farm data", 100),
        CEO("CEO", "Executive authority; approves certification status changes", 90),
        COMPLIANCE_OFFICER("Compliance Officer", "Certification compliance; approves status changes and validations", 85),
        MANAGER("Manager", "Manage operations, no deletion", 80),
        FIELD_OFFICER("Field Officer", "Supplier onboarding: profiles, commitments, inspections", 50),
        WORKER("Worker", "View and update assigned tasks", 30),
        INVESTOR("Investor", "View-only reports and analytics", 10);

        private final String displayName;
        private final String description;
        private final int accessLevel;

        UserRole(String displayName, String description, int accessLevel) {
            this.displayName = displayName;
            this.description = description;
            this.accessLevel = accessLevel;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getAccessLevel() { return accessLevel; }
    }

    public enum AuthMethod {
        PIN("PIN", "4-6 digit PIN"),
        PASSWORD("Password", "Email and password");

        private final String displayName;
        private final String description;

        AuthMethod(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // ✅ Full constructor with all fields
    public UserEntity(long localId, String uid, String name, UserRole role, String phone, String email,
                      String passwordHash, String pinHash, AuthMethod authMethod,
                      long lastLoginAt, String lastLoginIp, String lastLoginDevice,
                      boolean emailVerified, long passwordChangedAt, int failedLoginAttempts,
                      long lockedUntil, boolean twoFactorEnabled, String twoFactorSecret,
                      long lastUpdatedAt, long deactivatedAt, String deactivatedBy,
                      String deactivationReason, String bio, String company, String title,
                      String timezone, long createdAt, long updatedAt, boolean isActive, long farmId) {
        this.localId = localId;
        this.uid = uid;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.pinHash = pinHash;
        this.authMethod = authMethod;
        this.lastLoginAt = lastLoginAt;
        this.lastLoginIp = lastLoginIp;
        this.lastLoginDevice = lastLoginDevice;
        this.emailVerified = emailVerified;
        this.passwordChangedAt = passwordChangedAt;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.twoFactorEnabled = twoFactorEnabled;
        this.twoFactorSecret = twoFactorSecret;
        this.lastUpdatedAt = lastUpdatedAt;
        this.deactivatedAt = deactivatedAt;
        this.deactivatedBy = deactivatedBy;
        this.deactivationReason = deactivationReason;
        this.bio = bio;
        this.company = company;
        this.title = title;
        this.timezone = timezone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
        this.farmId = farmId;
    }

    // Constructor for email/password users
    @Ignore
    public UserEntity(String name, UserRole role, String phone, String email, String passwordHash) {
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authMethod = AuthMethod.PASSWORD;
        this.isActive = true;
        this.emailVerified = false;
        this.failedLoginAttempts = 0;
        this.twoFactorEnabled = false;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastUpdatedAt = now;
        this.passwordChangedAt = now;
    }

    // Constructor for Firebase Auth users
    @Ignore
    public UserEntity(String uid, String name, UserRole role, String phone,
                      String email, String passwordHash, long farmId) {
        this.uid = uid;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authMethod = AuthMethod.PASSWORD;
        this.isActive = true;
        this.emailVerified = false;
        this.failedLoginAttempts = 0;
        this.twoFactorEnabled = false;
        this.farmId = farmId;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastUpdatedAt = now;
        this.passwordChangedAt = now;
    }

    // Constructor for PIN users
    @Ignore
    public UserEntity(String name, UserRole role, String phone, String pinHash, long farmId) {
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.pinHash = pinHash;
        this.authMethod = AuthMethod.PIN;
        this.isActive = true;
        this.emailVerified = false;
        this.failedLoginAttempts = 0;
        this.twoFactorEnabled = false;
        this.farmId = farmId;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastUpdatedAt = now;
        this.passwordChangedAt = now;
    }

    // ========================================
    // GETTERS AND SETTERS
    // ========================================

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) {
        this.role = role;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public AuthMethod getAuthMethod() { return authMethod; }
    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    // ✅ New Security & Authentication Getters/Setters
    public long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getLastLoginDevice() { return lastLoginDevice; }
    public void setLastLoginDevice(String lastLoginDevice) {
        this.lastLoginDevice = lastLoginDevice;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public long getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(long passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public long getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(long lockedUntil) {
        this.lockedUntil = lockedUntil;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getTwoFactorSecret() { return twoFactorSecret; }
    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    // ✅ Account Status Getters/Setters
    public long getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(long lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(long deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(String deactivatedBy) {
        this.deactivatedBy = deactivatedBy;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getDeactivationReason() { return deactivationReason; }
    public void setDeactivationReason(String deactivationReason) {
        this.deactivationReason = deactivationReason;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    // ✅ Profile Getters/Setters
    public String getBio() { return bio; }
    public void setBio(String bio) {
        this.bio = bio;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getCompany() { return company; }
    public void setCompany(String company) {
        this.company = company;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    // Existing getters/setters
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) {
        this.farmId = farmId;
        this.updatedAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    // Helper methods
    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    public String getDisplayRole() {
        return role == null ? "" : role.getDisplayName();
    }

    // ✅ Helper: Check if account is locked
    public boolean isLocked() {
        if (lockedUntil <= 0) return false;
        return System.currentTimeMillis() < lockedUntil;
    }

    // ✅ Helper: Get time remaining until lock expires
    public long getLockRemainingMillis() {
        if (lockedUntil <= 0) return 0;
        long remaining = lockedUntil - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    // ✅ Helper: Check if account is deactivated
    public boolean isDeactivated() {
        return deactivatedAt > 0;
    }
}
