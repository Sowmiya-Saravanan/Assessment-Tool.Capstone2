package com.assesscraft.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.assesscraft.mvc.model.AssessmentForm;
import com.assesscraft.mvc.model.QuestionForm;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/educator/assessment")
public class AssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentController.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    public AssessmentController(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/create")
    public String createAssessment(@Valid @ModelAttribute("assessment") AssessmentForm assessmentForm,
                                  BindingResult result,
                                  @RequestParam(required = false) Long classId,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (result.hasErrors()) {
            return "educator-dashboard"; // Re-render the form with errors
        }

        try {
            // Prepare payload
            Map<String, Object> assessmentPayload = new HashMap<>();
            assessmentPayload.put("title", assessmentForm.getTitle());
            assessmentPayload.put("description", assessmentForm.getDescription());
            assessmentPayload.put("type", assessmentForm.getType() != null ? assessmentForm.getType() : "QUIZ");
            assessmentPayload.put("classId", classId); // Use classId from request param if provided
            assessmentPayload.put("durationMinutes", assessmentForm.getDurationMinutes());
            assessmentPayload.put("startTime", assessmentForm.getStartTime().toString());
            assessmentPayload.put("endTime", assessmentForm.getEndTime().toString());
            assessmentPayload.put("gradingMode", assessmentForm.getGradingMode());
            assessmentPayload.put("status", classId != null ? "ASSIGNED" : "DRAFT");
            assessmentPayload.put("allowResumption", true);
            assessmentPayload.put("maxAttempts", 1);
            assessmentPayload.put("resultsPublished", false);
            assessmentPayload.put("createdAt", assessmentForm.getCreatedAt().toString());
            assessmentPayload.put("updatedAt", assessmentForm.getUpdatedAt().toString());
            assessmentPayload.put("createdBy", session.getAttribute("userId"));

            // Map questions
            List<Map<String, Object>> questionsPayload = new ArrayList<>();
            if (assessmentForm.getQuestions() != null && !assessmentForm.getQuestions().isEmpty()) {
                for (QuestionForm q : assessmentForm.getQuestions()) {
                    Map<String, Object> questionMap = new HashMap<>();
                    questionMap.put("content", q.getContent());
                    questionMap.put("type", q.getType());
                    questionMap.put("maxScore", q.getMaxScore() != null ? q.getMaxScore().doubleValue() : 0.0);

                    // Handle MCQ options
                    if ("MCQ".equals(q.getType()) && q.getOptions() != null) {
                        List<Map<String, Object>> options = q.getOptions().stream().map(opt -> {
                            Map<String, Object> optionMap = new HashMap<>();
                            optionMap.put("text", opt.getText());
                            optionMap.put("isCorrect", opt.isCorrect());
                            return optionMap;
                        }).collect(Collectors.toList());
                        questionMap.put("options", options);
                    }

                    // Handle TRUE_FALSE
                    if ("TRUE_FALSE".equals(q.getType()) && q.getTrueFalseAnswer() != null) {
                        questionMap.put("trueFalseAnswer", q.getTrueFalseAnswer());
                    }

                    // Handle SHORT_ANSWER/ESSAY keywords
                    if (("SHORT_ANSWER".equals(q.getType()) || "ESSAY".equals(q.getType())) && q.getKeywords() != null) {
                        List<Map<String, Object>> keywords = q.getKeywords().stream().map(kw -> {
                            Map<String, Object> keywordMap = new HashMap<>();
                            keywordMap.put("keyword", kw.getKeyword());
                            keywordMap.put("weight", kw.getWeight());
                            return keywordMap;
                        }).collect(Collectors.toList());
                        questionMap.put("keywords", keywords);
                    }

                    questionsPayload.add(questionMap);
                }
            }
            assessmentPayload.put("questions", questionsPayload);

            // Validate classId if provided
            if (classId != null) {
                ResponseEntity<Map> classResponse = restTemplate.exchange(
                    apiBaseUrl + "/api/data/class/" + classId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );
                Map classData = classResponse.getBody();
                if (classData == null || classData.get("educator") == null || 
                    !((Map)classData.get("educator")).get("userId").equals(session.getAttribute("userId"))) {
                    redirectAttributes.addFlashAttribute("error", "Invalid or unauthorized class selection");
                    return "redirect:/educator/dashboard";
                }
            }

            String jsonPayload = objectMapper.writeValueAsString(assessmentPayload);
            logger.debug("Creating assessment with payload: {}", jsonPayload);

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/assessment",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.CREATED) {
                Long assessmentId = ((Number) response.getBody().get("assessmentId")).longValue();
                logger.info("Assessment created successfully with ID: {}", assessmentId);
                redirectAttributes.addFlashAttribute("success", "Assessment created successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to create assessment: " + response.getStatusCode());
                logger.warn("API response status: {}", response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error creating assessment: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to create assessment: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @GetMapping("/{assessmentId}")
    public String viewAssessment(@PathVariable Long assessmentId, HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            logger.debug("Fetching assessment details for ID: {}", assessmentId);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/assessment/" + assessmentId,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                model.addAttribute("assessment", response.getBody());
                return "assessment-details"; // Ensure this view exists
            } else {
                model.addAttribute("error", "Failed to load assessment: " + response.getStatusCode());
                return "redirect:/educator/dashboard";
            }
        } catch (Exception e) {
            logger.error("Error fetching assessment details: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load assessment: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    // New endpoint to assign an existing assessment to a class
    @PostMapping("/{assessmentId}/assign-to-class")
    public String assignAssessmentToClass(@PathVariable Long assessmentId, @RequestParam Long classId,
                                          HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // Validate classId
            ResponseEntity<Map> classResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            Map classData = classResponse.getBody();
            if (classData == null || classData.get("educator") == null || 
                !((Map)classData.get("educator")).get("userId").equals(session.getAttribute("userId"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid or unauthorized class selection");
                return "redirect:/educator/dashboard";
            }

            // Fetch assessment to ensure it exists
            ResponseEntity<Map> assessmentResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/assessment/" + assessmentId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            if (assessmentResponse.getStatusCode() != HttpStatus.OK || assessmentResponse.getBody() == null) {
                redirectAttributes.addFlashAttribute("error", "Assessment not found");
                return "redirect:/educator/dashboard";
            }

            // Prepare payload to assign assessment to class
            Map<String, Object> payload = new HashMap<>();
            payload.put("classId", classId);
            payload.put("status", "ASSIGNED");
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/assessment/" + assessmentId + "/assign",
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Assessment {} assigned to class {} successfully", assessmentId, classId);
                redirectAttributes.addFlashAttribute("success", "Assessment assigned to class successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to assign assessment: " + response.getStatusCode());
                logger.warn("API response status: {}", response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error assigning assessment to class: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to assign assessment: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }
}