package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.afrinuts.farmos.data.local.converter.Converters;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private UserRole role;
    private String phone;
    private String email;

    // Authentication fields
    private String passwordHash;     // For email/password login (SHA-256)
    private String pinHash;          // For PIN login (SHA-256)
    private AuthMethod authMethod;   // How they login

    private long lastLoginAt;
    private long createdAt;
    private long updatedAt;
    private boolean isActive;

    public enum UserRole {
        OWNER("Owner", "👑", "Full access to all farm data", 100),
        MANAGER("Manager", "📋", "Manage operations, no deletion", 80),
        WORKER("Worker", "👤", "View and update assigned tasks", 30),
        INVESTOR("Investor", "📈", "View-only reports and analytics", 10);

        private final String displayName;
        private final String icon;
        private final String description;
        private final int accessLevel; // Higher = more access

        UserRole(String displayName, String icon, String description, int accessLevel) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
            this.accessLevel = accessLevel;
        }

        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
        public int getAccessLevel() { return accessLevel; }
    }

    public enum AuthMethod {
        PIN("PIN", "🔢", "4-6 digit PIN"),
        PASSWORD("Password", "🔐", "Email and password");

        private final String displayName;
        private final String icon;
        private final String description;

        AuthMethod(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    // Constructor for email/password users
    public UserEntity(String name, UserRole role, String phone, String email, String passwordHash) {
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authMethod = AuthMethod.PASSWORD;
        this.isActive = true;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Constructor for PIN users (workers)
    public UserEntity(String name, UserRole role, String phone, String pinHash) {
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.pinHash = pinHash;
        this.authMethod = AuthMethod.PIN;
        this.isActive = true;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) {
        this.role = role;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
        this.updatedAt = System.currentTimeMillis();
    }

    public AuthMethod getAuthMethod() { return authMethod; }
    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = System.currentTimeMillis();
    }

    // Helper methods
    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    public boolean canAccess(int requiredAccessLevel) {
        return role.getAccessLevel() >= requiredAccessLevel;
    }

    public String getDisplayRole() {
        return role.getIcon() + " " + role.getDisplayName();
    }
}