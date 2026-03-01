package com.fooddelivery.customerservice.config;

import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    // Stub method to replace real JWT generation temporarily
    public String generateToken(String username, String role) {
        // Just return a fake token
        return "fake-jwt-for-" + username + "-" + role;
    }
}