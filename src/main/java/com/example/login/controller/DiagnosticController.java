package com.example.login.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.login.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic controller for health/status checks.
 */
@RestController
public class DiagnosticController {

    @GetMapping("/diagnostic")
    public ApiResponse<Map<String, Object>> diagnostic() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "UP");
        info.put("application", "CapstoneProject");
        info.put("description", "DevOps Analytics Dashboard - Enterprise");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        return ApiResponse.success("Application is running", info);
    }
}
