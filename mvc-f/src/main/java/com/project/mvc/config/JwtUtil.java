package com.project.mvc.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private SecretKey signingKey;

    private final long JWT_TOKEN_VALIDITY = 5 * 60 * 60 * 1000; // 5 hours in milliseconds

    @PostConstruct
    public void init() {
        if (SECRET_KEY == null || SECRET_KEY.length() < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 characters) long");
        }
        this.signingKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return doGenerateToken(claims, email);
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(signingKey)
                .compact();
    }

    public Map<String, String> validateToken(String token) {
        Map<String, String> result = new HashMap<>();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String email = claims.getSubject(); // Subject is the email
            String role = claims.get("role", String.class);
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                result.put("status", "invalid");
                result.put("message", "Token has expired");
            } else {
                result.put("status", "valid");
                result.put("email", email);
                result.put("role", role);
            }
        } catch (JwtException | IllegalArgumentException e) {
            result.put("status", "invalid");
            result.put("message", "Invalid token: " + e.getMessage());
        }
        return result;
    }
}