package com.example.product.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        if (request == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String principal = request.getEmail() != null && !request.getEmail().isBlank()
            ? request.getEmail().trim()
            : request.getUsername();
        if (principal == null || principal.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Optional<UserAccount> userOpt = userService.authenticate(principal, request.getPassword());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserAccount user = userOpt.get();
        String userId = user.getEmail() != null ? user.getEmail() : user.getUsername();
        String token = jwtService.generateToken(userId, user.getRole());
        return ResponseEntity.ok(new AuthResponse(token, userId, user.getRole()));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody AuthRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null || request.getFullName() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String fullName = request.getFullName().trim();
        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword().trim();
        if (fullName.isEmpty() || email.isEmpty() || !email.contains("@") || password.length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (userService.findByEmail(email).isPresent() || userService.findByUsername(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        try {
            UserAccount created = userService.createUser(fullName, email, password, "USER");
            String token = jwtService.generateToken(created.getEmail(), created.getRole());
            return ResponseEntity.ok(new AuthResponse(token, created.getEmail(), created.getRole()));
        } catch (DataIntegrityViolationException ex) {
            // Handles race conditions/legacy rows where username uniqueness still collides.
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
