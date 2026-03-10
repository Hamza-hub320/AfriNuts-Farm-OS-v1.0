package com.afrinuts.farmos.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class AuthUtilsTest {

    @Test
    public void testHash() {
        String password = "password123";
        String hash1 = AuthUtils.hash(password);
        String hash2 = AuthUtils.hash(password);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    public void testVerify() {
        String password = "securePass!";
        String hash = AuthUtils.hash(password);

        assertTrue(AuthUtils.verify(password, hash));
        assertFalse(AuthUtils.verify("wrongPass", hash));
    }

    @Test
    public void testIsValidPin() {
        assertTrue(AuthUtils.isValidPin("1234"));
        assertTrue(AuthUtils.isValidPin("123456"));
        assertFalse(AuthUtils.isValidPin("123"));
        assertFalse(AuthUtils.isValidPin("1234567"));
        assertFalse(AuthUtils.isValidPin("12a4"));
        assertFalse(AuthUtils.isValidPin(null));
    }

    @Test
    public void testIsValidEmail() {
        // Valid emails
        assertTrue(AuthUtils.isValidEmail("user@example.com"));
        assertTrue(AuthUtils.isValidEmail("john.doe@afrinuts.ci"));
        assertTrue(AuthUtils.isValidEmail("user+tag@example.com"));
        assertTrue(AuthUtils.isValidEmail("user.name@example.co.uk"));

        // Invalid emails
        assertFalse(AuthUtils.isValidEmail("invalid-email"));
        assertFalse(AuthUtils.isValidEmail("@missingusername.com"));
        assertFalse(AuthUtils.isValidEmail("user@.com"));
        assertFalse(AuthUtils.isValidEmail("user@domain."));
        assertFalse(AuthUtils.isValidEmail("user@domain,com"));
        assertFalse(AuthUtils.isValidEmail(""));
        assertFalse(AuthUtils.isValidEmail(null));
    }

    @Test
    public void testIsValidPhone() {
        // Valid phone numbers
        assertTrue(AuthUtils.isValidPhone("01234567"));
        assertTrue(AuthUtils.isValidPhone("0123456789"));
        assertTrue(AuthUtils.isValidPhone("+22501234567"));
        assertTrue(AuthUtils.isValidPhone("01 23 45 67"));
        assertTrue(AuthUtils.isValidPhone("01-23-45-67"));
        assertTrue(AuthUtils.isValidPhone("+225 01 23 45 67"));
        assertTrue(AuthUtils.isValidPhone("+1234567890123")); // 13 digits with +

        // Invalid phone numbers
        assertFalse(AuthUtils.isValidPhone("123"));
        assertFalse(AuthUtils.isValidPhone("0123456789012345")); // 16 digits
        assertFalse(AuthUtils.isValidPhone("abc123456"));
        assertFalse(AuthUtils.isValidPhone("+abc123456"));
        assertFalse(AuthUtils.isValidPhone(""));
        assertFalse(AuthUtils.isValidPhone(null));
    }

    @Test
    public void testGenerateTempPassword() {
        String pass1 = AuthUtils.generateTempPassword(8);
        String pass2 = AuthUtils.generateTempPassword(8);

        assertNotNull(pass1);
        assertEquals(8, pass1.length());
        assertNotEquals(pass1, pass2);
    }
}