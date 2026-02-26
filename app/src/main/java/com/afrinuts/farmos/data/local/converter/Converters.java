package com.afrinuts.farmos.data.local.converter;

import androidx.room.TypeConverter;

import com.afrinuts.farmos.data.local.entity.BlockEntity; // We'll create this next

/**
 * Converters for Room database to handle custom types.
 */
public class Converters {

    @TypeConverter
    public static String fromBlockStatus(BlockEntity.BlockStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static BlockEntity.BlockStatus toBlockStatus(String status) {
        return status == null ? null : BlockEntity.BlockStatus.valueOf(status);
    }

    // Add more converters as we create enum types
}