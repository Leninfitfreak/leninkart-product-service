package com.example.product.auth;

public class AuthResponse {
    private final String token;
    private final String userId;
    private final String role;

    public AuthResponse(String token, String userId, String role) {
        this.token = token;
        this.userId = userId;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
