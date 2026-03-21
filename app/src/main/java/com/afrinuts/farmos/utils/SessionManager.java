package com.afrinuts.farmos.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.afrinuts.farmos.data.local.entity.UserEntity;

public class SessionManager {
    private static final String PREF_NAME = "auth";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(UserEntity user) {
        editor.putLong(KEY_USER_ID, user.getLocalId());
        editor.putString(KEY_USER_NAME, user.getName());
        editor.putString(KEY_USER_ROLE, user.getRole().name());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public long getUserId() {
        return pref.getLong(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }

    public UserEntity.UserRole getUserRole() {
        String role = pref.getString(KEY_USER_ROLE, null);
        if (role == null) return null;
        return UserEntity.UserRole.valueOf(role);
    }

    public boolean isOwner() {
        return getUserRole() == UserEntity.UserRole.OWNER;
    }

    public boolean isManager() {
        return getUserRole() == UserEntity.UserRole.MANAGER;
    }

    public boolean isInvestor() {
        return getUserRole() == UserEntity.UserRole.INVESTOR;
    }

    public boolean canManageUsers() {
        return isOwner(); // Only owners can manage users
    }

    public boolean canEditData() {
        return isOwner() || isManager(); // Owners and Managers can edit
    }

    public boolean canViewAnalytics() {
        return true; // Everyone can view analytics
    }

    public void logout() {
        editor.clear();
        editor.commit();
    }
}