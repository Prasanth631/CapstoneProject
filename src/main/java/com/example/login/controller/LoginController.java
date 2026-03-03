package com.example.login.controller;

import com.example.login.service.LoginService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/validate")
    public String validate(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String mobile,
            @RequestParam String password,
            @RequestParam String dob) {

        boolean isValid = loginService.validateLogin(username, email, mobile, password, dob);
        return isValid ? "Login Valid" : "Login Invalid";
    }
}
