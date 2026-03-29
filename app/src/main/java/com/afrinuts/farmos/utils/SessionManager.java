package com.afrinuts.farmos.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.afrinuts.farmos.data.local.entity.UserEntity;

public class SessionManager {
    private static final String PREF_NAME = "auth";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_UID = "user_uid";

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
        editor.putString(KEY_USER_UID, user.getUid());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
        Log.d("SessionManager", "Session created for user: " + user.getName() + " (ID: " + user.getLocalId() + ")");
    }

    public boolean isLoggedIn() {
        boolean loggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false);

        // ✅ Validate that the user ID is valid (not 0 or -1)
        if (loggedIn) {
            long userId = pref.getLong(KEY_USER_ID, -1);
            if (userId <= 0) {
                Log.w("SessionManager", "isLoggedIn true but userId is " + userId + " - clearing session");
                logout();
                return false;
            }
        }

        Log.d("SessionManager", "isLoggedIn: " + loggedIn);
        return loggedIn;
    }

    public long getUserId() {
        long userId = pref.getLong(KEY_USER_ID, -1);
        // ✅ Return -1 for invalid IDs (0 is also invalid)
        return userId > 0 ? userId : -1;
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }

    public String getUserUid() {
        return pref.getString(KEY_USER_UID, null);
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
        return isOwner();
    }

    public boolean canEditData() {
        return isOwner() || isManager();
    }

    public boolean canViewAnalytics() {
        return true;
    }

    public void logout() {
        Log.d("SessionManager", "Session cleared");
        editor.clear();
        editor.apply();
    }
}