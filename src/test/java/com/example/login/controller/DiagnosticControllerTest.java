package com.example.login.controller;

import com.example.login.dto.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticControllerTest {

    private final DiagnosticController controller = new DiagnosticController();

    @Test
    void diagnostic_returnsSuccessResponse() {
        ApiResponse<Map<String, Object>> response = controller.diagnostic();

        assertTrue(response.isSuccess());
        assertEquals("Application is running", response.getMessage());
        assertNotNull(response.getData());
    }

    @Test
    void diagnostic_containsRequiredFields() {
        ApiResponse<Map<String, Object>> response = controller.diagnostic();
        Map<String, Object> data = response.getData();

        assertEquals("UP", data.get("status"));
        assertEquals("CapstoneProject", data.get("application"));
        assertNotNull(data.get("javaVersion"));
        assertNotNull(data.get("osName"));
    }
}
