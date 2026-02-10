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

    public UserAccount createUser(String username, String password, String role) {
        String normalized = username.trim();
        String hash = encoder.encode(password);
        String finalRole = role == null || role.isBlank() ? "USER" : role.toUpperCase();
        return repository.save(new UserAccount(normalized, hash, finalRole));
    }

    public Optional<UserAccount> authenticate(String username, String password) {
        Optional<UserAccount> userOpt = findByUsername(username);
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
            if (repository.existsByUsernameIgnoreCase(username)) {
                continue;
            }
            createUser(username, password, role);
        }
    }
}
