package com.project.api.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;


    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String requestTokenHeader = request.getHeader("Authorization");

        String email = null;
        String jwtToken = null;
    
        final String[] role = {null};

        // Check if the Authorization header is present and starts with "Bearer "
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7); // Remove "Bearer " prefix
            try {
                email = jwtUtil.getEmailFromToken(jwtToken);
                role[0] = jwtUtil.getRoleFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.warn("Unable to parse JWT token");
            } catch (ExpiredJwtException e) {
                logger.warn("JWT token has expired");
            } catch (MalformedJwtException e) {
                logger.warn("Invalid JWT token");
            }
        } else {
            logger.warn("Authorization header missing or does not start with Bearer");
        }

        // Validate the token and set the authentication context
        if (email != null && role[0] != null && jwtUtil.validateToken(jwtToken, email) && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = new User(email, "", Collections.singletonList(() -> "ROLE_" + role[0]));
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
           
            logger.info("Set authentication for user: {} with role: {}", email, role[0]);
           
            chain.doFilter(request, response);
        } else if (request.getRequestURI().equals("/api/educator/login") ||
                   request.getRequestURI().equals("/api/educator/register") ||
                   request.getRequestURI().equals("/api/educator/validate") ||
                   request.getRequestURI().equals("/api/student/login") ||
                   request.getRequestURI().equals("/api/student/register")) {
            // Unauthenticated endpoints, proceed without validation
            chain.doFilter(request, response);
        } else {
            // Token is invalid and endpoint is protected, return 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"Unauthorized: Invalid or missing JWT token\"}");
            return; // Stop further processing
        }
    }
}