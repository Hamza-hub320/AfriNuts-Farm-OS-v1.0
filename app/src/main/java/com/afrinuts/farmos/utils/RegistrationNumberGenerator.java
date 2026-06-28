package com.afrinuts.farmos.utils;

import com.afrinuts.farmos.data.local.entity.SupplierEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class RegistrationNumberGenerator {

    private static final String PREFIX = "AFR";
    private static final String SEPARATOR = "-";

    // Thread-safe counter for sequential numbers
    private static final AtomicInteger counter = new AtomicInteger(1);

    /**
     * Generate a unique registration number
     * Format: AFR-YYYY-XXXXX
     * Example: AFR-2026-00123
     */
    public static String generateRegistrationNumber() {
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        String sequence = String.format("%05d", counter.getAndIncrement());
        return PREFIX + SEPARATOR + year + SEPARATOR + sequence;
    }

    /**
     * Generate a registration number with custom prefix
     * Format: CUSTOM-YYYY-XXXXX
     */
    public static String generateRegistrationNumber(String customPrefix) {
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        String sequence = String.format("%05d", counter.getAndIncrement());
        return customPrefix + SEPARATOR + year + SEPARATOR + sequence;
    }

    /**
     * Generate a registration number based on supplier type
     */
    public static String generateRegistrationNumber(SupplierEntity.SupplierType type) {
        String prefix;
        switch (type) {
            case COOPERATIVE:
                prefix = "COOP";
                break;
            case INDEPENDENT_FARMER:
                prefix = "FARM";
                break;
            case REGIONAL_SUPPLIER:
                prefix = "SUPP";
                break;
            case COMMERCIAL_AGGREGATOR:
                prefix = "AGGR";
                break;
            case PROCESSOR:
                prefix = "PROC";
                break;
            default:
                prefix = "SUPP";
                break;
        }
        return generateRegistrationNumber(prefix);
    }

    /**
     * Reset counter (useful for testing or when starting fresh)
     */
    public static void resetCounter() {
        counter.set(1);
    }

    /**
     * Set counter to a specific value (useful for migrations)
     */
    public static void setCounter(int value) {
        counter.set(value);
    }
}