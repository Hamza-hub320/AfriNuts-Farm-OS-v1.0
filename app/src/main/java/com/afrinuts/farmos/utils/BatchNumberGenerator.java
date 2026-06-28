package com.afrinuts.farmos.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchNumberGenerator {

    private static final String PREFIX = "AFN";
    private static final String SEPARATOR = "-";
    private static final AtomicInteger counter = new AtomicInteger(1);

    /**
     * Generate a batch number
     * Format: AFN-YYMMDD-XXX
     * Example: AFN-260625-001
     */
    public static String generateBatchNumber() {
        String date = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date());
        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);
        return PREFIX + SEPARATOR + date + SEPARATOR + sequence;
    }

    /**
     * Generate a batch number with custom prefix
     */
    public static String generateBatchNumber(String customPrefix) {
        String date = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date());
        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);
        return customPrefix + SEPARATOR + date + SEPARATOR + sequence;
    }

    /**
     * Generate a batch number with source indicator
     * Format: AFN-SOURCE-TYPE-YYMMDD-XXX
     * Example: AFN-FARM-ORG-260625-001
     */
    public static String generateBatchNumber(String source, String type) {
        // If source or type is null/empty, fallback to default format
        if (source == null || source.isEmpty() || type == null || type.isEmpty()) {
            return generateBatchNumber();
        }
        String date = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date());
        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);
        return PREFIX + SEPARATOR + source + SEPARATOR + type + SEPARATOR + date + SEPARATOR + sequence;
    }

    /**
     * Reset counter
     */
    public static void resetCounter() {
        counter.set(1);
    }
}