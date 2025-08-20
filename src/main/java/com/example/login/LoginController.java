package com.example.login;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    private final LoginService loginService = new LoginService();

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
