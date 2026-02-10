package com.example.product.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Optional<UserAccount> userOpt = userService.authenticate(request.getUsername(), request.getPassword());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserAccount user = userOpt.get();
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole()));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody AuthRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String username = request.getUsername().trim();
        String password = request.getPassword().trim();
        if (username.isEmpty() || password.length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (userService.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        UserAccount created = userService.createUser(username, password, "USER");
        String token = jwtService.generateToken(created.getUsername(), created.getRole());
        return ResponseEntity.ok(new AuthResponse(token, created.getUsername(), created.getRole()));
    }
}
