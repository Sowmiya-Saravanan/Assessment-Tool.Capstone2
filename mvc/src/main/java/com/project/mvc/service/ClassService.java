package com.project.mvc.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.project.mvc.dto.ClassCreateRequest;

@Service
public class ClassService {

    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);

    private final RestTemplate restTemplate;

    private final String backendUrl;

    public ClassService(RestTemplate restTemplate, @Value("${api.base.url}") String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendUrl = apiBaseUrl;
    }

    public String createClass(ClassCreateRequest request, String jwtToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);

            HttpEntity<ClassCreateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                backendUrl + "/api/educator/create/class",
                entity,
                Map.class);

            if (response.getStatusCode() == HttpStatus.OK && "success".equals(response.getBody().get("status"))) {
                String classId = (String) response.getBody().get("classId");
                return "Class Creation successful:" + classId;
            } else {
                return "Class Creation failed: " + response.getBody().get("message");
            }
        } catch (Exception e) {
            logger.error("Error during Class Creation: {}", e.getMessage(), e);
            return "Class Creation failed: " + e.getMessage();
        }
    }

    public List<Map<String, Object>> getEducatorClasses(String jwtToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                backendUrl + "/api/educator/classes",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && "success".equals(response.getBody().get("status"))) {
                return (List<Map<String, Object>>) response.getBody().get("classes");
            } else {
                logger.error("Failed to fetch educator classes: {}", response.getBody().get("message"));
                throw new RuntimeException("Failed to fetch classes: " + response.getBody().get("message"));
            }
        } catch (Exception e) {
            logger.error("Error fetching educator classes: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching classes: " + e.getMessage());
        }
    }

    public Map<String, Object> getClassDetails(Long classId, String jwtToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                backendUrl + "/api/educator/class-details?classId=" + classId,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && "success".equals(response.getBody().get("status"))) {
                return (Map<String, Object>) response.getBody().get("classDetails");
            } else {
                logger.error("Failed to fetch class details: {}", response.getBody().get("message"));
                throw new RuntimeException("Failed to fetch class details: " + response.getBody().get("message"));
            }
        } catch (Exception e) {
            logger.error("Error fetching class details: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching class details: " + e.getMessage());
        }
    }
}