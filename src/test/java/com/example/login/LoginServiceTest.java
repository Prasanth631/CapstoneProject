package com.example.login;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class LoginServiceTest {

    private LoginService loginService;
    
    @Before
    public void setUp() {
        loginService = new LoginService();
    }

    @Test
    public void testValidLogin() {
        assertTrue("Valid login should pass validation", 
            loginService.validateLogin(
                "john", 
                "john@example.com", 
                "9876543210", 
                "Password@123", 
                "1995-08-15"
            )
        );
    }
    
    @Test
    public void testInvalidUsername() {
        assertFalse("Username less than 3 characters should fail", 
            loginService.validateLogin(
                "jo", 
                "john@example.com", 
                "9876543210", 
                "Password@123", 
                "1995-08-15"
            )
        );
    }

    @Test
    public void testInvalidEmail() {
        assertFalse("Invalid email format should fail", 
            loginService.validateLogin(
                "john", 
                "johnemailcom", 
                "9876543210", 
                "Password@123", 
                "1995-08-15"
            )
        );
    }

    @Test
    public void testInvalidMobile() {
        assertFalse("Mobile with less than 10 digits should fail", 
            loginService.validateLogin(
                "john", 
                "john@example.com", 
                "987654", 
                "Password@123", 
                "1995-08-15"
            )
        );
    }
    
    @Test
    public void testInvalidPassword() {
        assertFalse("Simple password should fail", 
            loginService.validateLogin(
                "john", 
                "john@example.com", 
                "9876543210", 
                "simple", 
                "1995-08-15"
            )
        );
    }
    
    @Test
    public void testInvalidDOB() {
        assertFalse("Invalid date format should fail", 
            loginService.validateLogin(
                "john", 
                "john@example.com", 
                "9876543210", 
                "Password@123", 
                "15-08-1995"
            )
        );
    }
    
    @Test
    public void testNullValues() {
        assertFalse("Null username should fail", 
            loginService.validateLogin(
                null, 
                "john@example.com", 
                "9876543210", 
                "Password@123", 
                "1995-08-15"
            )
        );
    }
}