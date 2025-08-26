package com.example.login;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DiagnosticController {

    @GetMapping("/diagnostic")
    public String diagnostic() {
        return "diagnostic"; // This will serve the diagnostic.html from static folder
    }
}