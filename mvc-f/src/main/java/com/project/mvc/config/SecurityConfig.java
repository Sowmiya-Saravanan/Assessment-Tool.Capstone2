package com.project.mvc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import jakarta.servlet.http.Cookie;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/educator/register", "/educator/login", 
                                 "/student/register", "/student/login",
                                 "/admin/register", "/admin/login",
                                 "/error", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/educator/**").hasAuthority("ROLE_EDUCATOR") // Require EDUCATOR role for educator endpoints
                .requestMatchers("/student/**").hasAuthority("ROLE_STUDENT") // Require STUDENT role for student endpoints
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN") // Require ADMIN role for admin endpoints
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .formLogin(form -> form.disable())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestUri = request.getRequestURI();
                    if (requestUri.startsWith("/educator")|| requestUri.startsWith("/api/educator")) {
                        response.sendRedirect("/educator/login");
                    } else if (requestUri.startsWith("/student")|| requestUri.startsWith("/api/student")) {
                        response.sendRedirect("/student/login");
                    } else {
                        response.sendRedirect("/admin/login");
                    }
                })
            )
            .logout(logout -> logout
                .logoutRequestMatcher(request ->
                    ("/educator/logout".equals(request.getRequestURI()) ||
                     "/student/logout".equals(request.getRequestURI()) ||
                     "/admin/logout".equals(request.getRequestURI())) &&
                    "POST".equalsIgnoreCase(request.getMethod())
                )
                .logoutSuccessHandler((request, response, authentication) -> {
                    String requestUri = request.getRequestURI();
                    if (requestUri.equals("/student/logout")) {
                        response.sendRedirect("/student/login?logout");
                    } else if (requestUri.equals("/admin/logout")) {
                        response.sendRedirect("/admin/login?logout");
                    } else {
                        response.sendRedirect("/educator/login?logout");
                    }
                })
                .addLogoutHandler((request, response, authentication) -> {
                    Cookie cookie = new Cookie("jwtToken", null);
                    cookie.setPath("/");
                    cookie.setHttpOnly(true);
                    cookie.setMaxAge(0);
                    cookie.setAttribute("SameSite", "Lax");
                    cookie.setDomain("localhost");
                    response.addCookie(cookie);
                })
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}