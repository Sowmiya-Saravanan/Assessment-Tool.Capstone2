package com.assesscraft.mvc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class LoginService {

    private final RestTemplate restTemplate;
    private final String apiUrl;

    public LoginService(RestTemplate restTemplate, @Value("${app.api.base-url}/api/login") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    public Map<String, String> attemptLogin(String email, String password) throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);
        requestBody.put("password", password);

        try {
            return restTemplate.postForObject(apiUrl, requestBody, Map.class);
        } catch (HttpClientErrorException e) {
            throw new Exception(e.getResponseBodyAsString().contains("error") 
                ? e.getResponseBodyAsString() 
                : "Authentication failed: " + e.getStatusText());
        } catch (ResourceAccessException e) {
            throw new Exception("Unable to connect to the server. Please try again later.");
        } catch (Exception e) {
            throw new Exception("An unexpected error occurred. Please contact support.");
        }
    }

    public Map<String, Object>[] getEducatorClasses(String email, String token) throws Exception {
        String classesUrl = appApiBaseUrl + "/educator/classes?email=" + email;
        try {
            restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add("Authorization", "Bearer " + token);
                return execution.execute(request, body);
            });
            Map<String, Object>[] classes = restTemplate.getForObject(classesUrl, Map[].class);
            restTemplate.getInterceptors().clear(); // Clear interceptor after use
            return classes;
        } catch (HttpClientErrorException e) {
            throw new Exception(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new Exception("Failed to fetch classes: " + e.getMessage());
        }
    }

    // Add this field to access the base URL without /api/login
    @Value("${app.api.base-url}")
    private String appApiBaseUrl;
}