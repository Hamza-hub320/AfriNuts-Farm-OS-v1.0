package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private UserRole role;
    private String phone;
    private String email;
    private String passwordHash; // For future authentication
    private long createdAt;
    private long updatedAt;
    private boolean isActive;

    public enum UserRole {
        OWNER("Owner", "👑", "Full access to all farm data"),
        MANAGER("Manager", "📋", "Manage operations, no financial deletion"),
        WORKER("Worker", "👤", "View tasks, update status"),
        INVESTOR("Investor", "📈", "View-only access to reports");

        private final String displayName;
        private final String icon;
        private final String description;

        UserRole(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    public UserEntity(String name, UserRole role, String phone, String email) {
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.email = email;
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

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = System.currentTimeMillis();
    }
}