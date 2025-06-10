package com.project.mvc.service;

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

import com.project.mvc.dto.AssessmentCreateRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);

    private final RestTemplate restTemplate;

    private final String backendUrl;

    public AssessmentService(RestTemplate restTemplate, @Value("${api.base.url}") String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendUrl = apiBaseUrl;
    }

    public String createAssessment(AssessmentCreateRequest assessment, String jwtToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);

            HttpEntity<AssessmentCreateRequest> entity = new HttpEntity<>(assessment, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                backendUrl + "/api/educator/assessment/create",
                entity,
                Map.class);

            if (response.getStatusCode() == HttpStatus.OK && "success".equals(response.getBody().get("status"))) {
                String assessmentId = (String) response.getBody().get("assessmentId");
                return "Assessment Creation successful:" + assessmentId;
            } else {
                return "Assessment Creation failed: " + response.getBody().get("message");
            }
        } catch (Exception e) {
            logger.error("Error during Assessment Creation: {}", e.getMessage(), e);
            return "Assessment Creation failed: " + e.getMessage();
        }
    }

    public List<Map<String, Object>> getEducatorAssessments(String jwtToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                backendUrl + "/api/educator/assessment/list",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && "success".equals(response.getBody().get("status"))) {
                return (List<Map<String, Object>>) response.getBody().get("assessments");
            } else {
                logger.error("Failed to fetch educator assessments: {}", response.getBody().get("message"));
                throw new RuntimeException("Failed to fetch assessments: " + response.getBody().get("message"));
            }
        } catch (Exception e) {
            logger.error("Error fetching educator assessments: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching assessments: " + e.getMessage());
        }
    }

    public String assignAssessment(Long assessmentId, List<Long> classIds, String jwtToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("assessmentId", assessmentId);
            requestBody.put("classIds", classIds);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                backendUrl + "/api/educator/assessment/assign",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, String>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && "success".equals(response.getBody().get("status"))) {
                return "Assessment assigned successfully: " + response.getBody().get("assessmentId");
            } else {
                logger.error("Failed to assign assessment: {}", response.getBody().get("message"));
                return "Failed to assign assessment: " + response.getBody().get("message");
            }
        } catch (Exception e) {
            logger.error("Error assigning assessment: {}", e.getMessage(), e);
            return "Error assigning assessment: " + e.getMessage();
        }
        
    }
}