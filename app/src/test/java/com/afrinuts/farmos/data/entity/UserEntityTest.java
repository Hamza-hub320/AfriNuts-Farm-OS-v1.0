package com.afrinuts.farmos.data.entity;

import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.utils.AuthUtils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserEntityTest {

    private UserEntity passwordUser;
    private UserEntity pinUser;
    private final long TEST_FARM_ID = 1L;

    @Before
    public void setUp() {
        // FIXED: Use the 5-parameter constructor for email/password users
        passwordUser = new UserEntity(
                "John Owner",
                UserEntity.UserRole.OWNER,
                "+22512345678",
                "john@afrinuts.com",
                AuthUtils.hash("password123")
        );
        // Set farmId separately (since email/password constructor doesn't have it)
        passwordUser.setFarmId(TEST_FARM_ID);

        // FIXED: Use the 5-parameter constructor for PIN users with farmId
        pinUser = new UserEntity(
                "Sam Worker",
                UserEntity.UserRole.WORKER,
                "+22587654321",
                AuthUtils.hash("1234"),
                TEST_FARM_ID  // Add farmId parameter
        );
    }

    @Test
    public void testUserCreation() {
        assertNotNull(passwordUser);
        assertEquals("John Owner", passwordUser.getName());
        assertEquals(UserEntity.UserRole.OWNER, passwordUser.getRole());
        assertEquals("john@afrinuts.com", passwordUser.getEmail());
        assertTrue(passwordUser.isActive());
        assertEquals(TEST_FARM_ID, passwordUser.getFarmId());
    }

    @Test
    public void testPinUserCreation() {
        assertNotNull(pinUser);
        assertEquals("Sam Worker", pinUser.getName());
        assertEquals(UserEntity.UserRole.WORKER, pinUser.getRole());
        assertNull(pinUser.getEmail());
        assertEquals(UserEntity.AuthMethod.PIN, pinUser.getAuthMethod());
        assertEquals(TEST_FARM_ID, pinUser.getFarmId());
    }

    @Test
    public void testGetInitials() {
        assertEquals("JO", passwordUser.getInitials());
        assertEquals("SW", pinUser.getInitials());

        // FIXED: Create single name user with email/password constructor + setFarmId
        UserEntity singleName = new UserEntity(
                "Hamza",
                UserEntity.UserRole.MANAGER,
                "+22500000000",
                "hamza@afrinuts.com",
                AuthUtils.hash("pass")
        );
        singleName.setFarmId(TEST_FARM_ID);
        assertEquals("H", singleName.getInitials());
    }

    @Test
    public void testRoleAccessLevels() {
        assertEquals(100, UserEntity.UserRole.OWNER.getAccessLevel());
        assertEquals(80, UserEntity.UserRole.MANAGER.getAccessLevel());
        assertEquals(30, UserEntity.UserRole.WORKER.getAccessLevel());
        assertEquals(10, UserEntity.UserRole.INVESTOR.getAccessLevel());
    }

    @Test
    public void testCanAccess() {
        assertTrue(passwordUser.canAccess(80)); // Owner can access manager level
        assertTrue(passwordUser.canAccess(100)); // Owner can access owner level

        pinUser.setRole(UserEntity.UserRole.MANAGER);
        assertTrue(pinUser.canAccess(80));
        assertFalse(pinUser.canAccess(100));
    }

    @Test
    public void testDisplayRole() {
        assertEquals("👑 Owner", passwordUser.getDisplayRole());

        pinUser.setRole(UserEntity.UserRole.MANAGER);
        assertEquals("📋 Manager", pinUser.getDisplayRole());
    }

    @Test
    public void testUserStatusToggle() {
        assertTrue(passwordUser.isActive());
        passwordUser.setActive(false);
        assertFalse(passwordUser.isActive());
    }

    @Test
    public void testLastLoginUpdate() {
        long before = System.currentTimeMillis() - 1000;
        passwordUser.setLastLoginAt(System.currentTimeMillis());
        assertTrue(passwordUser.getLastLoginAt() >= before);
    }

    @Test
    public void testFarmId() {
        assertEquals(TEST_FARM_ID, passwordUser.getFarmId());
        assertEquals(TEST_FARM_ID, pinUser.getFarmId());
    }
}