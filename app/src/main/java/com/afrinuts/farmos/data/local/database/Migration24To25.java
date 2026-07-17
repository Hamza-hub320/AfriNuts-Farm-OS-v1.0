package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration24To25 extends Migration {

    private static final String TAG = "Migration24To25";

    public Migration24To25() {
        super(24, 25);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 24 to 25 - Adding new user fields");

        try {
            // ========================================
            // ✅ Security & Authentication fields
            // ========================================

            // Last login tracking
            database.execSQL("ALTER TABLE users ADD COLUMN lastLoginIp TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN lastLoginDevice TEXT");

            // Email verification
            database.execSQL("ALTER TABLE users ADD COLUMN emailVerified INTEGER NOT NULL DEFAULT 0");

            // Password change tracking
            database.execSQL("ALTER TABLE users ADD COLUMN passwordChangedAt INTEGER");

            // Security settings
            database.execSQL("ALTER TABLE users ADD COLUMN failedLoginAttempts INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE users ADD COLUMN lockedUntil INTEGER");
            database.execSQL("ALTER TABLE users ADD COLUMN twoFactorEnabled INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE users ADD COLUMN twoFactorSecret TEXT");

            Log.d(TAG, "✅ Added security & authentication fields");

            // ========================================
            // ✅ Account Status fields
            // ========================================

            database.execSQL("ALTER TABLE users ADD COLUMN lastUpdatedAt INTEGER");
            database.execSQL("ALTER TABLE users ADD COLUMN deactivatedAt INTEGER");
            database.execSQL("ALTER TABLE users ADD COLUMN deactivatedBy TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN deactivationReason TEXT");

            Log.d(TAG, "✅ Added account status fields");

            // ========================================
            // ✅ Profile fields
            // ========================================

            database.execSQL("ALTER TABLE users ADD COLUMN bio TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN company TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN title TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN timezone TEXT");

            Log.d(TAG, "✅ Added profile fields");

            // ========================================
            // ✅ Set default values for existing users
            // ========================================

            database.execSQL("UPDATE users SET " +
                    "emailVerified = 0, " +
                    "failedLoginAttempts = 0, " +
                    "twoFactorEnabled = 0, " +
                    "lastUpdatedAt = createdAt, " +
                    "passwordChangedAt = createdAt, " +
                    "timezone = 'UTC' " +
                    "WHERE lastUpdatedAt IS NULL");

            Log.d(TAG, "✅ Set default values for existing users");

            // ========================================
            // ✅ Create indexes for better performance
            // ========================================

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_users_role ON users(role)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_users_isActive ON users(isActive)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_users_lastLoginAt ON users(lastLoginAt)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_users_deactivatedAt ON users(deactivatedAt)");

            Log.d(TAG, "✅ Created indexes for users table");

            Log.d(TAG, "✅ Migration 24 -> 25 completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 24 -> 25 failed", e);
            throw e;
        }
    }
}