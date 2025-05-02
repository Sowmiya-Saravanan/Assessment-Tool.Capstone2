package com.assesscraft.mvc.config;

import com.assesscraft.mvc.service.CustomUserDetailsService;
import com.assesscraft.mvc.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("Configuring SecurityFilterChain with patterns: /educator/login, /educator/register, /perform_login (permitAll), /educator/** (EDUCATOR), anyRequest (authenticated)");
        
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            System.out.println("JWT secret is not configured in application.properties");
            throw new IllegalStateException("JWT secret is not configured in application.properties");
        }
        System.out.println("Loaded JWT secret length: " + jwtSecret.length());

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/educator/login", "/educator/register", "/perform_login").permitAll()
                .requestMatchers("/educator/**").hasRole("EDUCATOR")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .formLogin(form -> form.disable())
            .exceptionHandling(exception -> exception.accessDeniedPage("/educator/login?error=Access%20Denied"))
            .addFilterBefore(new JwtSessionFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private class JwtSessionFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            HttpSession session = request.getSession(false);
            String requestUri = request.getRequestURI();
            String sessionId = session != null ? session.getId() : "null";
            System.out.println("Processing request: " + requestUri + ", Session ID: " + sessionId);

            if (session != null) {
                String token = (String) session.getAttribute("token");
                System.out.println("Token present in session: " + (token != null));

                if (token != null) {
                    try {
                        if (jwtTokenService.validateToken(token)) {
                            String username = jwtTokenService.getUsernameFromToken(token);
                            UserDetails userDetails = customUserDetailsService.loadUserFromSession(session);
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            System.out.println("Security context set for user: " + userDetails.getUsername() + ", roles: " + userDetails.getAuthorities());
                        } else {
                            System.out.println("Invalid JWT token for request: " + requestUri);
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to set security context for request: " + requestUri + ". Error: " + e.getMessage());
                    }
                } else {
                    System.out.println("No token found in session for request: " + requestUri);
                }
            } else {
                System.out.println("No session found for request: " + requestUri);
            }

            filterChain.doFilter(request, response);
        }
    }
}