package com.example.login;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
public class LoginService {
    public boolean validateLogin(String username, String email, String mobile, String password, String dob) {
        return isValidUsername(username) &&
               isValidEmail(email) &&
               isValidMobile(mobile) &&
               isValidPassword(password) &&
               isValidDOB(dob);
    }
    public boolean isValidUsername(String username) {
        return username != null && username.length() >= 3;
    }
    public boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }
    public boolean isValidMobile(String mobile) {
        return mobile != null && mobile.matches("^\\d{10}$");
    }

    public boolean isValidPassword(String password) {
        return password != null && 
               password.length() >= 8 && 
               password.matches(".*\\d.*") && // at least one digit
               password.matches(".*[a-z].*") && // at least one lowercase
               password.matches(".*[A-Z].*") && // at least one uppercase
               password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"); // at least one special char
    }
    public boolean isValidDOB(String dob) {
        if (dob == null) return false;
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate.parse(dob, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}