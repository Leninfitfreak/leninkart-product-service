package com.example.product.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public Optional<UserAccount> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUsernameIgnoreCase(username.trim());
    }

    public Optional<UserAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return repository.findByEmailIgnoreCase(email.trim());
    }

    public UserAccount createUser(String fullName, String email, String password, String role) {
        String normalizedEmail = email.trim().toLowerCase();
        String hash = encoder.encode(password);
        String finalRole = role == null || role.isBlank() ? "USER" : role.toUpperCase();
        return repository.save(new UserAccount(normalizedEmail, normalizedEmail, fullName, hash, finalRole));
    }

    public Optional<UserAccount> authenticate(String principal, String password) {
        Optional<UserAccount> userOpt = findByEmail(principal);
        if (userOpt.isEmpty()) {
            // Backward compatibility for old non-email users.
            userOpt = findByUsername(principal);
        }
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        UserAccount user = userOpt.get();
        if (!encoder.matches(password, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public void seedUsers(String rawUsers) {
        if (rawUsers == null || rawUsers.isBlank()) {
            return;
        }
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
            String role = parts.length >= 3 ? parts[2].trim() : "USER";

            if (username.isEmpty() || password.isEmpty()) {
                continue;
            }
            String normalizedEmail = username.contains("@")
                ? username.toLowerCase()
                : (username.toLowerCase() + "@leninkart.local");
            if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
                continue;
            }
            createUser(username, normalizedEmail, password, role);
        }
    }
}
