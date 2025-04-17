// package com.assesscraft.api.util;

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.security.Keys;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;

// import javax.crypto.SecretKey;
// import java.nio.charset.StandardCharsets;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.Map;

// @Component
// public class JwtUtil {
//     @Value("${jwt.secret}")
//     private String secret;

//     @Value("${jwt.expiration}")
//     private Long expiration;

//     private SecretKey getSigningKey() {
//         return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
//     }

//     public String generateToken(String email, String role) {
//         Map<String, Object> claims = new HashMap<>();
//         claims.put("role", role);
//         return Jwts.builder()
//                 .claims(claims)
//                 .subject(email)
//                 .issuedAt(new Date())
//                 .expiration(new Date(System.currentTimeMillis() + expiration))
//                 .signWith(getSigningKey())
//                 .compact();
//     }

//     public Claims getClaimsFromToken(String token) {
//         return Jwts.parser()
//                 .verifyWith(getSigningKey())
//                 .build()
//                 .parseSignedClaims(token)
//                 .getPayload();
//     }

//     public String getEmailFromToken(String token) {
//         return getClaimsFromToken(token).getSubject();
//     }

//     public String getRoleFromToken(String token) {
//         return getClaimsFromToken(token).get("role", String.class);
//     }

//     public boolean isTokenValid(String token) {
//         try {
//             Claims claims = getClaimsFromToken(token);
//             return !claims.getExpiration().before(new Date());
//         } catch (Exception e) {
//             return false;
//         }
//     }

//     public void validateToken(String token) {
//         Claims claims = getClaimsFromToken(token);
//         if (claims.getExpiration().before(new Date())) {
//             throw new SecurityException("Token has expired");
//         }
//         // Additional validation (e.g., signature) is handled by parseSignedClaims
//     }
// }