package com.afrinuts.farmos.utils;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Generates cryptographic signatures for traceability records
 * Uses SHA-256 algorithm for tamper-proof verification
 */
public class DigitalSignatureGenerator {

    private static final String TAG = "DigitalSignature";
    private static final String ALGORITHM = "SHA-256";
    private static final String SEPARATOR = "|";
    private static final String PREFIX = "FOS-SIG";

    /**
     * Generate a digital signature for a batch traceability record
     *
     * @param batchNumber The batch number
     * @param dataMap Map of key-value pairs to include in the signature
     * @return The digital signature hash
     */
    public static String generateSignature(String batchNumber, java.util.Map<String, Object> dataMap) {
        try {
            if (batchNumber == null || batchNumber.isEmpty()) {
                Log.w(TAG, "Batch number is null, cannot generate signature");
                return null;
            }

            // 1. Sort keys to ensure consistent ordering
            List<String> sortedKeys = new ArrayList<>(dataMap.keySet());
            Collections.sort(sortedKeys);

            // 2. Build a string from all fields
            StringBuilder dataString = new StringBuilder();
            dataString.append(batchNumber).append(SEPARATOR);
            dataString.append("timestamp").append(SEPARATOR).append(System.currentTimeMillis()).append(SEPARATOR);

            for (String key : sortedKeys) {
                Object value = dataMap.get(key);
                if (value != null) {
                    dataString.append(key).append(SEPARATOR);
                    dataString.append(value.toString()).append(SEPARATOR);
                }
            }

            // 3. Add a secret salt
            String salt = getSaltForBatch(batchNumber);
            dataString.append(salt);

            // 4. Generate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(dataString.toString().getBytes(StandardCharsets.UTF_8));

            // 5. Convert to hex string
            String hash = bytesToHex(hashBytes);

            // 6. Create signature with prefix
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new java.util.Date());
            return PREFIX + "-" + hash.substring(0, 16) + "-" + timestamp;

        } catch (Exception e) {
            Log.e(TAG, "Error generating signature", e);
            return null;
        }
    }

    /**
     * Verify a digital signature against current data
     *
     * @param batchNumber The batch number
     * @param dataMap Map of key-value pairs to verify
     * @param storedSignature The stored signature to compare against
     * @return true if the signature matches
     */
    public static boolean verifySignature(String batchNumber,
                                          java.util.Map<String, Object> dataMap,
                                          String storedSignature) {
        if (storedSignature == null || storedSignature.isEmpty()) {
            return false;
        }

        // Extract the hash part (remove prefix and timestamp)
        String[] parts = storedSignature.split("-");
        if (parts.length < 3) {
            return false;
        }

        // Regenerate signature with current data
        String newSignature = generateSignature(batchNumber, dataMap);

        // Compare the hash parts (ignore timestamp for verification)
        String storedHash = parts[1];
        String newHash = newSignature != null ? newSignature.split("-")[1] : null;

        return storedHash != null && storedHash.equals(newHash);
    }

    /**
     * Generate a verification QR code URL that includes the hash
     */
    public static String getVerificationUrl(String batchNumber, String signature) {
        String encodedSignature = signature != null ? signature.replace("-", "") : "";
        return "https://www.afrinutsexport.com/verify?batch=" + batchNumber +
                "&sig=" + encodedSignature;
    }

    /**
     * Get a salt value for the batch (this prevents rainbow table attacks)
     */
    private static String getSaltForBatch(String batchNumber) {
        // Use a combination of batch number and a fixed secret
        // In production, use a secure key store
        String secret = "AfriNuts-Farm-OS-2024-Secret-Key";
        return batchNumber + secret;
    }

    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Generate a verification code (human-readable)
     */
    public static String generateVerificationCode(String batchNumber) {
        try {
            String input = batchNumber + System.currentTimeMillis() + "AfriNuts-Farm-OS";
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = bytesToHex(hashBytes);
            // Return first 12 characters as a verification code
            return hash.substring(0, 12).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            return "FOS-" + System.currentTimeMillis() % 1000000;
        }
    }
}