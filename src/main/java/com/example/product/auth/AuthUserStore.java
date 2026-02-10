package com.example.product.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthUserStore {
    private final Map<String, AuthUser> users = new HashMap<>();

    public AuthUserStore(@Value("${app.auth.users:leninkart:leninkart123:USER}") String rawUsers) {
        String[] entries = rawUsers.split(",");
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(":");
            if (parts.length < 2) {
                continue;
            }
            String username = parts[0].trim();
            String password = parts[1].trim();
            String role = parts.length >= 3 ? parts[2].trim().toUpperCase() : "USER";
            if (!username.isEmpty()) {
                users.put(username.toLowerCase(), new AuthUser(username, password, role));
            }
        }
    }

    public AuthUser find(String username) {
        if (username == null) {
            return null;
        }
        return users.get(username.trim().toLowerCase());
    }
}
