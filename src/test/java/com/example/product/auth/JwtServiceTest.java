package com.example.product.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServiceTest {

    @Test
    void generateAndParseTokenPreservesCoreClaims() {
        JwtService jwtService = new JwtService("dev-secret-for-tests", "leninkart", 300);

        String token = jwtService.generateToken("demo-user", "ADMIN");
        Claims claims = jwtService.parseToken(token);

        assertNotNull(token);
        assertEquals("demo-user", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("leninkart", claims.getIssuer());
    }
}
