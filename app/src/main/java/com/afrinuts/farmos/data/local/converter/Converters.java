package com.afrinuts.farmos.data.local.converter;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Converters for Room database to handle custom types.
 */
public class Converters {

    // Example converter for Date (you can remove if not needed)
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // We'll add enum converters here as we create them
    // Example pattern (uncomment when BlockStatus enum is created):
    /*
    @TypeConverter
    public static String fromBlockStatus(BlockEntity.BlockStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static BlockEntity.BlockStatus toBlockStatus(String status) {
        return status == null ? null : BlockEntity.BlockStatus.valueOf(status);
    }
    */
}