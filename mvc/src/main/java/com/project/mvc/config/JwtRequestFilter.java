package com.project.mvc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestTokenHeader = request.getHeader("Authorization");
        String jwtToken = null;

        logger.info("Processing request for URI: {}", request.getRequestURI());

        // Check for token in Authorization header
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            logger.info("Extracted token from Authorization header: {}", jwtToken);
        } else {
            // Check for token in cookies
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwtToken".equals(cookie.getName())) {
                        jwtToken = cookie.getValue();
                        logger.info("Extracted token from cookie: {}", jwtToken);
                        break;
                    }
                }
            } else {
                logger.info("No cookies found in the request");
            }
            if (jwtToken == null) {
                logger.info("No token found in Authorization header or cookies");
            }
        }

        String email = null;
        String role = null;

        if (jwtToken != null) {
            Map<String, String> validationResult = jwtUtil.validateToken(jwtToken);
            if ("valid".equals(validationResult.get("status"))) {
                email = validationResult.get("email");
                role = validationResult.get("role");
                logger.info("Token validated successfully: email={}, role={}", email, role);
            } else {
                logger.warn("Token validation failed: {}", validationResult.get("message"));
            }
        }

        // Set authentication in SecurityContext if token is valid
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
            logger.info("Authentication set for user: {}", email);
        }

        // Proceed with the filter chain, let SecurityConfig handle authorization
        chain.doFilter(request, response);
    }
}