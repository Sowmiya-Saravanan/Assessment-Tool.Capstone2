package com.assesscraft.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EducatorDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(EducatorDashboardService.class);

    private final RestTemplate restTemplate;
    private final String apiBaseUrl;

    public EducatorDashboardService(RestTemplate restTemplate, @Value("${app.api.base-url:http://localhost:8080}") String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.apiBaseUrl = apiBaseUrl;
    }

    public Map<String, Object> getDashboardData(String token) {
        Map<String, Object> dashboardModel = new HashMap<>();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // Fetch dashboard summary
            logger.debug("Fetching dashboard summary from: {}/api/data/educator-dashboard", apiBaseUrl);
            ResponseEntity<Map<String, Object>> dashboardResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/educator-dashboard",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            dashboardModel.put("dashboardData", dashboardResponse.getBody());

            // Fetch drafted classes
            logger.debug("Fetching drafted classes from: {}/api/data/classes/draft", apiBaseUrl);
            ResponseEntity<List<Map<String, Object>>> draftedResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/classes/draft",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            dashboardModel.put("draftedClasses", draftedResponse.getBody() != null ? draftedResponse.getBody() : new ArrayList<>());

            // Fetch active classes
            logger.debug("Fetching active classes from: {}/api/data/classes/active", apiBaseUrl);
            ResponseEntity<List<Map<String, Object>>> activeResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/classes/active",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            dashboardModel.put("activeClasses", activeResponse.getBody() != null ? activeResponse.getBody() : new ArrayList<>());

            // Fetch classes for assessment (reusing active classes)
            dashboardModel.put("classes", dashboardModel.get("activeClasses"));

            // Fetch assessments
            logger.debug("Fetching assessments from: {}/api/data/assessment", apiBaseUrl);
            ResponseEntity<List<Map<String, Object>>> assessmentsResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/assessment",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            dashboardModel.put("assessments", assessmentsResponse.getBody() != null ? assessmentsResponse.getBody() : new ArrayList<>());

            return dashboardModel;
        } catch (Exception e) {
            logger.error("Error fetching dashboard data: {}", e.getMessage(), e);
            dashboardModel.put("error", "Failed to load dashboard: " + e.getMessage());
            return dashboardModel;
        }
    }
}