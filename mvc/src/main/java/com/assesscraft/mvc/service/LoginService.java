package com.assesscraft.mvc.service;

import com.assesscraft.mvc.model.RegisterForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
public class LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${api.base.url}")
    private String apiBaseUrl;

    public Map<String, Object> attemptLogin(String username, String password) throws Exception {
        logger.debug("Attempting login for email: {}", username);

        Map<String, String> loginPayload = new HashMap<>();
        loginPayload.put("email", username);
        loginPayload.put("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonPayload = objectMapper.writeValueAsString(loginPayload);
        HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/auth/login",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("token") && responseBody.containsKey("userId")) {
                    logger.debug("Login successful for email: {}, received token and userId", username);
                    return responseBody; // Returns Map with token and userId
                } else {
                    logger.warn("Login response missing token or userId for email: {}", username);
                    return null;
                }
            } else {
                logger.warn("Login failed for email: {}, status: {}", username, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("Login request failed for email: {}. Exception: {}", username, e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, String> attemptRegister(RegisterForm registerForm) throws Exception {
        logger.debug("Attempting registration for email: {}", registerForm.getEmail());

        Map<String, String> registerPayload = new HashMap<>();
        registerPayload.put("email", registerForm.getEmail());
        registerPayload.put("password", registerForm.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonPayload = objectMapper.writeValueAsString(registerPayload);
        HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

        try {
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                apiBaseUrl + "/api/auth/register/educator",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, String>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            logger.error("Registration request failed for email: {}. Exception: {}", registerForm.getEmail(), e.getMessage(), e);
            throw e;
        }
    }
}