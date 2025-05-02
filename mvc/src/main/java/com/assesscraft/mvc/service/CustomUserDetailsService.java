package com.assesscraft.mvc.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.warn("loadUserByUsername called for: {}, but this method is not supported", username);
        throw new UsernameNotFoundException("This method should not be called directly. Use session-based JWT instead.");
    }

    public UserDetails loadUserFromSession(HttpSession session) {
        String token = (String) session.getAttribute("token");
        logger.debug("Loading user from session, Token present: {}", token != null);
        if (token == null) {
            logger.error("No JWT token found in session");
            throw new UsernameNotFoundException("No JWT token found in session");
        }

        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
            logger.debug("Parsed claims - Subject: {}, Role: {}", claims.getSubject(), claims.get("role"));

            String email = claims.getSubject();
            String role = "ROLE_" + claims.get("role", String.class).toUpperCase();
            logger.info("Loaded user - Email: {}, Role: {}", email, role);
            return new User(email, "", AuthorityUtils.createAuthorityList(role));
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage(), e);
            throw new UsernameNotFoundException("Invalid JWT token: " + e.getMessage());
        }
    }
}