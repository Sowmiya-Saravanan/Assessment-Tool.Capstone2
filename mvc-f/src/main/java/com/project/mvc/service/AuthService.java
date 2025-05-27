package com.project.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.project.mvc.dto.EducatorLoginRequest;
import com.project.mvc.dto.EducatorRegisterRequest;
import com.project.mvc.dto.StudentLoginRequest;
import com.project.mvc.dto.StudentRegisterRequest;

import java.util.Map;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final RestTemplate restTemplate;

    private final String backendUrl;

    public AuthService(RestTemplate restTemplate, @Value("${api.base.url}") String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendUrl = apiBaseUrl;
    }

    public String registerEducator(EducatorRegisterRequest frontendRequest) {
        if (!frontendRequest.getPassword().equals(frontendRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        try {
            EducatorRegisterRequest backendRequest = new EducatorRegisterRequest();
            backendRequest.setEmail(frontendRequest.getEmail());
            backendRequest.setPassword(frontendRequest.getPassword());
            backendRequest.setName(frontendRequest.getName());
            backendRequest.setConfirmPassword(frontendRequest.getConfirmPassword());
            ResponseEntity<String> response = restTemplate.postForEntity(
                backendUrl + "/api/educator/register",
                backendRequest,
                String.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                return "Registration successful";
            } else {
                return "Registration failed: " + response.getBody();
            }
        } catch (Exception e) {
            logger.error("Error during educator registration: {}", e.getMessage(), e);
            return "Registration failed: " + e.getMessage();
        }
    }

    public String loginEducator(EducatorLoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            return "Email and password must not be null";
        }
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                backendUrl + "/api/educator/login",
                request,
                Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK && "success".equals(response.getBody().get("status"))) {
                String token = (String) response.getBody().get("token");
                return "Login successful:" + token;
            } else {
                return "Login failed: " + response.getBody().get("message");
            }
        } catch (Exception e) {
            logger.error("Error during login: {}", e.getMessage(), e);
            return "Login failed: " + e.getMessage();
        }
    }



    public String registerStudent(StudentRegisterRequest frontendRequest) {
        if (!frontendRequest.getPassword().equals(frontendRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        try {
            StudentRegisterRequest backendRequest = new StudentRegisterRequest();
            backendRequest.setEmail(frontendRequest.getEmail());
            backendRequest.setPassword(frontendRequest.getPassword());
            backendRequest.setName(frontendRequest.getName());
            backendRequest.setConfirmPassword(frontendRequest.getConfirmPassword());
            ResponseEntity<String> response = restTemplate.postForEntity(
                backendUrl + "/api/student/register",
                backendRequest,
                String.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                return "Registration successful";
            } else {
                return "Registration failed: " + response.getBody();
            }
        } catch (Exception e) {
            logger.error("Error during student registration: {}", e.getMessage(), e);
            return "Registration failed: " + e.getMessage();
        }
    }

    public String loginStudent(StudentLoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            return "Email and password must not be null";
        }
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                backendUrl + "/api/student/login",
                request,
                Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK && "success".equals(response.getBody().get("status"))) {
                String token = (String) response.getBody().get("token");
                return "Login successful:" + token;
            } else {
                return "Login failed: " + response.getBody().get("message");
            }
        } catch (Exception e) {
            logger.error("Error during login: {}", e.getMessage(), e);
            return "Login failed: " + e.getMessage();
        }
    }
}