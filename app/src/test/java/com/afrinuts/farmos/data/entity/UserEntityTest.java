package com.afrinuts.farmos.data.entity;

import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.utils.AuthUtils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserEntityTest {

    private UserEntity passwordUser;
    private UserEntity pinUser;

    @Before
    public void setUp() {
        passwordUser = new UserEntity(
                "John Owner",
                UserEntity.UserRole.OWNER,
                "+22512345678",
                "john@afrinuts.com",
                AuthUtils.hash("password123")
        );

        pinUser = new UserEntity(
                "Sam Worker",
                UserEntity.UserRole.WORKER,
                "+22587654321",
                AuthUtils.hash("1234")
        );
    }

    @Test
    public void testUserCreation() {
        assertNotNull(passwordUser);
        assertEquals("John Owner", passwordUser.getName());
        assertEquals(UserEntity.UserRole.OWNER, passwordUser.getRole());
        assertEquals("john@afrinuts.com", passwordUser.getEmail());
        assertTrue(passwordUser.isActive());
    }

    @Test
    public void testPinUserCreation() {
        assertNotNull(pinUser);
        assertEquals("Sam Worker", pinUser.getName());
        assertEquals(UserEntity.UserRole.WORKER, pinUser.getRole());
        assertNull(pinUser.getEmail());
        assertEquals(UserEntity.AuthMethod.PIN, pinUser.getAuthMethod());
    }

    @Test
    public void testGetInitials() {
        assertEquals("JO", passwordUser.getInitials());
        assertEquals("SW", pinUser.getInitials());

        UserEntity singleName = new UserEntity("Hamza", UserEntity.UserRole.MANAGER,
                "+22500000000", "hamza@afrinuts.com", AuthUtils.hash("pass"));
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
}