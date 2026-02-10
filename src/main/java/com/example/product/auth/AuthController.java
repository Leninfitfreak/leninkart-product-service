package com.example.product.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthUserStore userStore;
    private final JwtService jwtService;

    public AuthController(AuthUserStore userStore, JwtService jwtService) {
        this.userStore = userStore;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        AuthUser user = userStore.find(request.getUsername());
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole()));
    }
}
