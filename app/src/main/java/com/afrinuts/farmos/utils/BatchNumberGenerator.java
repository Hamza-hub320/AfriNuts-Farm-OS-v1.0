package com.afrinuts.farmos.utils;

import android.util.Log;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique, human-readable batch numbers.
 *
 * Format: AFN-SOURCE-TYPE-YYMMDD-XXXXXX
 * Example: AFN-FARM-ORG-260710-A3F8C1
 *
 * Design notes:
 * - The 6-character suffix provides uniqueness within a (source, type, date)
 *   combination — 16.7M possible values per day/source/type.
 * - This class handles UNIQUENESS ONLY. Security (authenticating that a batch
 *   belongs to a buyer, controlling what data is revealed) is handled on the
 *   server side (Firestore lookup + buyer portal auth + endpoint rate limiting).
 * - The batch ID is designed to be readable, shareable, and unique — not secret.
 *   Anyone with a bag can see it; the security boundary is elsewhere.
 */
public class BatchNumberGenerator {

    private static final String TAG = "BatchNumberGenerator";
    private static final String PREFIX = "AFN";
    private static final String SEPARATOR = "-";

    // 6 hex chars = 24 bits = 16.7 million combos per day/source/type
    private static final int HASH_HEX_CHARS = 6;

    private static final AtomicInteger counter = new AtomicInteger(1);
    private static String lastDate = "";
    private static final SecureRandom secureRandom = new SecureRandom();

    // Salt for hashing (does not provide security — see class doc)
    private static final String SALT = "AfriNuts-Farm-OS-2026-Secret-Salt-3x7v9k2m";

    // In-memory collision guard (catches collisions on this device only).
    // For multi-device safety, pass existingBatchNumbers from the DB layer.
    private static final Set<String> generatedToday = ConcurrentHashMap.newKeySet();

    /**
     * Parsed components of a batch number.
     * Returned by {@link #parseBatchNumber(String)}.
     */
    public static class BatchParts {
        public final String prefix;
        public final String source;
        public final String type;
        public final String date;
        public final String hash;

        public BatchParts(String prefix, String source, String type, String date, String hash) {
            this.prefix = prefix;
            this.source = source;
            this.type = type;
            this.date = date;
            this.hash = hash;
        }

        public String getFullNumber() {
            return prefix + SEPARATOR + source + SEPARATOR + type + SEPARATOR + date + SEPARATOR + hash;
        }
    }

    /**
     * Generate a batch number without DB-level collision checking.
     *
     * ⚠️ This only protects against collisions on the SAME device. If you have
     * multiple devices generating batches, use
     * {@link #generateBatchNumber(String, String, Set)} instead.
     */
    public static synchronized String generateBatchNumber(String source, String type) {
        return generateBatchNumber(source, type, null);
    }

    /**
     * Generate a collision-safe batch number.
     *
     * @param source  Source code (FARM / COOP / INDP)
     * @param type    Type code (ORG / CONV)
     * @param existingBatchNumbers Set of batch numbers that already exist in the
     *                             database for today. Pass the result of a
     *                             Firestore query, or null for in-memory-only
     *                             collision checking.
     * @return A unique batch number in format AFN-SOURCE-TYPE-YYMMDD-XXXXXX
     */
    public static synchronized String generateBatchNumber(
            String source,
            String type,
            Set<String> existingBatchNumbers) {

        if (source == null || source.isEmpty() || type == null || type.isEmpty()) {
            return generateFallbackBatchNumber();
        }

        String date = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date());

        // Reset in-memory tracking at the start of a new day
        if (!date.equals(lastDate)) {
            counter.set(1);
            lastDate = date;
            generatedToday.clear();
        }

        // Retry on collision (10 attempts, then fall back to random)
        for (int attempt = 0; attempt < 10; attempt++) {
            String hash = generateUniqueHash(source, type, date, attempt);
            String candidate = PREFIX + SEPARATOR + source + SEPARATOR + type
                    + SEPARATOR + date + SEPARATOR + hash;

            boolean collision = generatedToday.contains(candidate)
                    || (existingBatchNumbers != null && existingBatchNumbers.contains(candidate));

            if (!collision) {
                generatedToday.add(candidate);
                counter.incrementAndGet();
                Log.d(TAG, "Generated batch number: " + candidate);
                return candidate;
            }

            Log.w(TAG, "Collision detected for " + candidate
                    + " (attempt " + (attempt + 1) + "), retrying...");
        }

        // Last-resort fallback: random suffix (extremely unlikely to reach here
        // since 10 attempts against 16.7M values is effectively guaranteed to
        // find a free slot)
        String fallback = PREFIX + SEPARATOR + source + SEPARATOR + type + SEPARATOR
                + date + SEPARATOR + generateRandomCode(HASH_HEX_CHARS);
        Log.e(TAG, "Falling back to random batch number after 10 collisions: " + fallback);
        generatedToday.add(fallback);
        return fallback;
    }

    /**
     * Generate a hash for the batch number suffix.
     *
     * The attempt counter is mixed into the input so retries produce different
     * values. This is what makes the collision-retry loop work.
     */
    private static String generateUniqueHash(String source, String type, String date, int attempt) {
        try {
            String input = source + "|" + type + "|" + date + "|"
                    + System.nanoTime() + "|" + attempt + "|"
                    + counter.get() + "|" + secureRandom.nextLong() + "|" + SALT;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            int bytesNeeded = (HASH_HEX_CHARS + 1) / 2;
            for (int i = 0; i < bytesNeeded; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.substring(0, HASH_HEX_CHARS).toUpperCase();

        } catch (Exception e) {
            Log.e(TAG, "Error generating hash, falling back to random", e);
            return generateRandomCode(HASH_HEX_CHARS);
        }
    }

    /**
     * Generate random alphanumeric code.
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
     * Parse and structurally validate a batch number.
     *
     * Returns null if the format is invalid. This does NOT verify that the batch
     * actually exists in the database — for that, do a Firestore lookup by
     * batchNumber and check the result.
     *
     * This is the correct security model:
     *   - Format validation is client-side (this method)
     *   - Authenticity validation is server-side (Firestore + buyer auth)
     */
    public static BatchParts parseBatchNumber(String batchNumber) {
        try {
            if (batchNumber == null || batchNumber.isEmpty()) return null;

            String[] parts = batchNumber.split(SEPARATOR);
            if (parts.length != 5) return null;

            if (!PREFIX.equals(parts[0])) return null;

            String hash = parts[4];
            if (hash == null || hash.length() != HASH_HEX_CHARS) return null;

            // Validate the hash contains only hex characters
            if (!hash.matches("[0-9A-F]{" + HASH_HEX_CHARS + "}")) return null;

            return new BatchParts(parts[0], parts[1], parts[2], parts[3], parts[4]);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing batch number", e);
            return null;
        }
    }

    /**
     * Generate a verification code for a batch.
     *
     * This code is used as a secondary credential — it appears on export
     * documents (Bill of Lading, Certificate of Origin) and is used by the
     * buyer portal for authentication.
     *
     * ⚠️ This code must be STORED with the batch (not recomputed). Verification
     * works by comparing the input against the stored value, NOT by regenerating
     * this code (the timestamp makes it non-deterministic).
     */
    public static String generateVerificationCode(String source, String type, String date) {
        try {
            String input = source + "|" + type + "|" + date + "|"
                    + SALT + "|" + System.nanoTime() + "|" + secureRandom.nextLong();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return "VER-" + hexString.toString().toUpperCase();

        } catch (Exception e) {
            Log.e(TAG, "Error generating verification code, falling back to random", e);
            return "VER-" + generateRandomCode(12);
        }
    }

    /**
     * Emergency fallback batch number generator.
     *
     * ⚠️ This produces a DIFFERENT format than the primary generator:
     *   Fallback: AFN-YYMMDD-SSSXXXX  (4 parts)
     *   Primary:  AFN-SOURCE-TYPE-YYMMDD-XXXXXX  (5 parts)
     *
     * Do NOT pass fallback numbers to {@link #parseBatchNumber(String)} — they
     * will be rejected. Use this only when source/type context is unavailable
     * (e.g. legacy data migration or emergency recovery).
     *
     * @deprecated Use {@link #generateBatchNumber(String, String, Set)} in all
     *             normal code paths. This method exists only for the fallback
     *             case where source/type are unknown.
     */
    @Deprecated
    public static synchronized String generateFallbackBatchNumber() {
        String date = new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date());

        if (!date.equals(lastDate)) {
            counter.set(1);
            lastDate = date;
            generatedToday.clear();
        }

        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);
        String random = generateRandomCode(4);
        String candidate = PREFIX + SEPARATOR + date + SEPARATOR + sequence + random;

        Log.w(TAG, "Generated FALLBACK batch number (non-standard format): " + candidate);
        return candidate;
    }

    /**
     * Reset the internal counter and daily tracking.
     * Intended for testing only.
     */
    public static void resetCounter() {
        counter.set(1);
        lastDate = "";
        generatedToday.clear();
    }
}