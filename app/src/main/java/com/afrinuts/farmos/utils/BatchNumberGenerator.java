package com.afrinuts.farmos.utils;

import android.util.Log;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchNumberGenerator {

    private static final String TAG = "BatchNumberGenerator";
    private static final String PREFIX = "AFN";
    private static final String SEPARATOR = "-";
    private static final AtomicInteger counter = new AtomicInteger(1);
    private static String lastDate = "";
    private static final SecureRandom secureRandom = new SecureRandom();

    // Salt for hashing (keep this secret)
    private static final String SALT = "AfriNuts-Farm-OS-2026-Secret-Salt-3x7v9k2m";

    /**
     * Generate a secure batch number with:
     * - Source and type codes for readability
     * - Date for tracking
     * - Unique 4-character hash for security
     * Format: AFN-SOURCE-TYPE-YYMMDD-XXXX
     * Example: AFN-FARM-ORG-260710-A3F8
     */
    public static synchronized String generateBatchNumber(String source, String type) {
        if (source == null || source.isEmpty() || type == null || type.isEmpty()) {
            return generateBatchNumber();
        }

        String date = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date());

        // Reset counter daily for internal tracking (not visible in batch number)
        if (!date.equals(lastDate)) {
            counter.set(1);
            lastDate = date;
        }

        // ✅ Generate unique 4-character hash
        String uniqueHash = generateUniqueHash(source, type, date);

        return PREFIX + SEPARATOR + source + SEPARATOR + type + SEPARATOR + date + SEPARATOR + uniqueHash;
    }

    /**
     * Generate a secure 4-character hash for the batch number
     */
    private static String generateUniqueHash(String source, String type, String date) {
        try {
            // Combine all elements with salt and timestamp
            String input = source + type + date + System.currentTimeMillis() + SALT;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // ✅ Take first 2 bytes (4 hex characters)
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 2; i++) {  // 2 bytes = 4 hex characters
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();

        } catch (Exception e) {
            Log.e(TAG, "Error generating hash, falling back to random", e);
            return generateRandomCode(4);
        }
    }

    /**
     * Generate random alphanumeric code
     */
    private static String generateRandomCode(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(characters.charAt(secureRandom.nextInt(characters.length())));
        }
        return code.toString();
    }

    /**
     * Fallback batch number generator
     */
    public static synchronized String generateBatchNumber() {
        String date = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date());

        if (!date.equals(lastDate)) {
            counter.set(1);
            lastDate = date;
        }

        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);
        String random = generateRandomCode(4);
        return PREFIX + SEPARATOR + date + SEPARATOR + sequence + random;
    }

    /**
     * Verify a batch number's authenticity
     */
    public static boolean verifyBatchNumber(String batchNumber, String source, String type, String date) {
        try {
            // Extract the hash part
            String[] parts = batchNumber.split(SEPARATOR);
            if (parts.length < 5) return false;

            String hash = parts[parts.length - 1];
            String expectedHash = generateUniqueHash(source, type, date);

            return hash.equals(expectedHash);

        } catch (Exception e) {
            Log.e(TAG, "Error verifying batch number", e);
            return false;
        }
    }

    /**
     * Generate verification code (for document verification)
     */
    public static String generateVerificationCode(String source, String type, String date) {
        try {
            String input = source + type + date + SALT + System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Take 12 characters for verification
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return "VER-" + hexString.toString().toUpperCase();

        } catch (Exception e) {
            Log.e(TAG, "Error generating verification code", e);
            return "VER-" + generateRandomCode(12);
        }
    }

    /**
     * Reset counter manually
     */
    public static void resetCounter() {
        counter.set(1);
        lastDate = "";
    }
}