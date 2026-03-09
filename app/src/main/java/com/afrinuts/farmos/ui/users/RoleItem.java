package com.afrinuts.farmos.ui.users;

import com.afrinuts.farmos.data.local.entity.UserEntity;

public class RoleItem {
    private UserEntity.UserRole role;
    private String displayName;
    private int iconResId;

    public RoleItem(UserEntity.UserRole role, String displayName, int iconResId) {
        this.role = role;
        this.displayName = displayName;
        this.iconResId = iconResId;
    }

    public UserEntity.UserRole getRole() { return role; }
    public String getDisplayName() { return displayName; }
    public int getIconResId() { return iconResId; }

    @Override
    public String toString() {
        return displayName;
    }
}