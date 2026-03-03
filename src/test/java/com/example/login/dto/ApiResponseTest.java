package com.example.login.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void success_createsSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("test data");

        assertTrue(response.isSuccess());
        assertEquals("Success", response.getMessage());
        assertEquals("test data", response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void success_withCustomMessage() {
        ApiResponse<String> response = ApiResponse.success("Custom message", "data");

        assertTrue(response.isSuccess());
        assertEquals("Custom message", response.getMessage());
        assertEquals("data", response.getData());
    }

    @Test
    void error_createsErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error("Something went wrong");

        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }
}
