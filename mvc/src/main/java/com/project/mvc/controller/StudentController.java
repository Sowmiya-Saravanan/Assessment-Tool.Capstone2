package com.project.mvc.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    private final RestTemplate restTemplate;
    private final String backendUrl;

    public StudentController(RestTemplate restTemplate, @Value("${api.base.url}") String backendUrl) {
        this.restTemplate = restTemplate;
        this.backendUrl = backendUrl;
    }

@GetMapping("/dashboard")
    public String studentDashboard(Model model, HttpServletRequest request) {
        logger.info("Rendering student dashboard");

        // Extract JWT token from cookie
        String jwtToken = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        // If token is missing, redirect to login
        if (jwtToken == null) {
            logger.warn("JWT token not found in cookies. Redirecting to login.");
            model.addAttribute("error", "Session expired. Please log in again.");
            return "redirect:/student/login";
        }

        // Prepare headers with JWT token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Fetch classes from backend API
        List<Map<String, Object>> classes = new ArrayList<>();
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                backendUrl + "/api/student/classes",
                HttpMethod.GET,
                entity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() != null && response.getBody().containsKey("classes")) {
                classes = (List<Map<String, Object>>) response.getBody().get("classes");
            }
        } catch (Exception e) {
            logger.error("Error fetching classes from backend: {}", e.getMessage());
            model.addAttribute("error", "Failed to load classes. Please try again later.");
        }

        // Add attributes to model
        model.addAttribute("classes", classes);
        model.addAttribute("studentEmail", request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Student");
        model.addAttribute("jwtToken", jwtToken); // Pass token to the template for JavaScript

        return "student-dashboard";
    }
}

