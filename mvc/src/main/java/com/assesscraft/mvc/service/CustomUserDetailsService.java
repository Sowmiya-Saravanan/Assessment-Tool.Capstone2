package com.assesscraft.mvc.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("This method should not be called directly. Use session-based JWT instead.");
    }

    public UserDetails loadUserFromSession(HttpSession session) {
        String token = (String) session.getAttribute("token");
        System.out.println("CustomUserDetailsService - Loading user from session, Token present: " + (token != null));
        if (token == null) {
            throw new UsernameNotFoundException("No JWT token found in session");
        }

        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
            System.out.println("CustomUserDetailsService - Parsed claims - Subject: " + claims.getSubject() + ", Role: " + claims.get("role"));

            String email = claims.getSubject();
            String role = "ROLE_" + claims.get("role", String.class).toUpperCase();
            System.out.println("CustomUserDetailsService - Loaded user - Email: " + email + ", Role: " + role);
            return new User(email, "", AuthorityUtils.createAuthorityList(role));
        } catch (Exception e) {
            throw new UsernameNotFoundException("Invalid JWT token: " + e.getMessage());
        }
    }
}