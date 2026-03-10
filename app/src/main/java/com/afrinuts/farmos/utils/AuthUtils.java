package com.afrinuts.farmos.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.regex.Pattern;

public class AuthUtils {

    private static final String SHA_256 = "SHA-256";
    private static final int PIN_MIN_LENGTH = 4;
    private static final int PIN_MAX_LENGTH = 6;

    // Email validation pattern (simple but effective)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Hash a password or PIN using SHA-256
     */
    public static String hash(String input) {
        if (input == null) return null;

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
        if (input == null || hash == null) return false;
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
     * Check if email is valid format (works in unit tests)
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Check if phone is valid (basic validation - only digits, +, spaces, dashes)
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) return false;

        // Remove spaces and dashes for length check
        String cleanPhone = phone.replaceAll("[\\s-]", "");

        // Check if it contains only digits and optional + at start
        if (!cleanPhone.matches("^\\+?[0-9]+$")) {
            return false;
        }

        // Check length (8-15 characters including optional +)
        return cleanPhone.length() >= 8 && cleanPhone.length() <= 15;
    }
}