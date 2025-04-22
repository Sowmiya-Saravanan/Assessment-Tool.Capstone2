package com.assesscraft.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final RestTemplate restTemplate;

    @Value("${app.api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    public LoginController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @GetMapping("/educator/login")
    public String showEducatorLogin(Model model) {
        logger.debug("Rendering educator login page");
        // Pass any error from redirect to the model
        String error = (String) model.getAttribute("error");
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "educator-login";
    }

    @PostMapping("/educator/login")
    public String performLogin(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        logger.debug("Processing POST /educator/login - username: '{}', password length: {}", username, (password != null ? password.length() : 0));
        if (username == null || password == null) {
            logger.warn("Missing username or password parameters");
            return "redirect:/educator/login?error=Missing credentials";
        }

        LoginRequest request = new LoginRequest();
        request.setEmail(username);
        request.setPassword(password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        try {
            logger.debug("Attempting API call to: {}, Body: {}", apiBaseUrl + "/api/auth/login", request);
            ResponseEntity<String> response = restTemplate.exchange(
                apiBaseUrl + "/api/auth/login",
                HttpMethod.POST,
                entity,
                String.class
            );
            logger.debug("API response - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            String token = response.getBody();
            if (token == null || token.isEmpty()) {
                logger.warn("No valid token received for email: {}", username);
                return "redirect:/educator/login?error=Invalid credentials or server issue";
            }
            logger.info("Login successful for email: {}, Token: {}", username, token);
            session.setAttribute("token", token);
            logger.debug("Token set in session, redirecting to /educator/dashboard");
            return "redirect:/educator/dashboard";
        } catch (HttpClientErrorException e) {
            logger.error("API error for email: {}. Status: {}, Response: {}", username, e.getStatusCode(), e.getResponseBodyAsString());
            return "redirect:/educator/login?error=" + e.getStatusText();
        } catch (Exception e) {
            logger.error("Unexpected error for email: {}. Exception: {}", username, e.getMessage(), e);
            return "redirect:/educator/login?error=Unexpected error: " + e.getMessage();
        }
    }
}