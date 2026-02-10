package com.example.product.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final long ttlSeconds;

    public JwtService(
        @Value("${app.jwt.secret:dev-secret-change-me}") String secret,
        @Value("${app.jwt.issuer:leninkart}") String issuer,
        @Value("${app.jwt.ttl-seconds:86400}") long ttlSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(ensureKeyBytes(secret));
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    public String generateToken(String userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttlSeconds)))
            .claim("role", role)
            .signWith(key)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private byte[] ensureKeyBytes(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            return Arrays.copyOf(bytes, 32);
        }
        return bytes;
    }
}
