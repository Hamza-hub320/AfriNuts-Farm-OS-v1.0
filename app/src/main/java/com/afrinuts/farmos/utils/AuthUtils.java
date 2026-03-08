package com.afrinuts.farmos.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AuthUtils {

    private static final String SHA_256 = "SHA-256";
    private static final int PIN_MIN_LENGTH = 4;
    private static final int PIN_MAX_LENGTH = 6;

    /**
     * Hash a password or PIN using SHA-256
     */
    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verify a password/PIN against its hash
     */
    public static boolean verify(String input, String hash) {
        String inputHash = hash(input);
        return inputHash != null && inputHash.equals(hash);
    }

    /**
     * Validate PIN format (4-6 digits)
     */
    public static boolean isValidPin(String pin) {
        if (pin == null) return false;
        return pin.matches("\\d{" + PIN_MIN_LENGTH + "," + PIN_MAX_LENGTH + "}");
    }

    /**
     * Generate a random temporary password
     */
    public static String generateTempPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Check if email is valid format
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Check if phone is valid (basic Ivorian format)
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        // Basic validation - can be expanded
        return phone.matches("^[0-9\\+\\s-]{8,15}$");
    }
}