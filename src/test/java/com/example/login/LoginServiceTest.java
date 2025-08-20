package com.example.login;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginServiceTest {

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService();
    }

    @Test
    void testValidLogin() {
        boolean result = loginService.validateLogin(
                "prasanth",
                "test@example.com",
                "9876543210",
                "Password@123",
                "2000-05-20"
        );
        assertTrue(result);
    }

    @Test
    void testInvalidEmail() {
        boolean result = loginService.validateLogin(
                "prasanth",
                "wrongemail",
                "9876543210",
                "Password@123",
                "2000-05-20"
        );
        assertFalse(result);
    }

    @Test
    void testInvalidPassword() {
        boolean result = loginService.validateLogin(
                "prasanth",
                "test@example.com",
                "9876543210",
                "pass",
                "2000-05-20"
        );
        assertFalse(result);
    }
}
