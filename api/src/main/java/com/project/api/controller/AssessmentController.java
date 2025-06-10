package com.project.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.api.config.JwtUtil;
import com.project.api.model.Assessment;
import com.project.api.service.AssessmentService;
import com.project.api.dto.AssessmentCreateRequest;

@RestController
@RequestMapping("/api/educator/assessment")
public class AssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private JwtUtil jwtUtil;

    // api endpoint to create a new assessment
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createAssessment(
            @RequestBody AssessmentCreateRequest assessmentRequest,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("Received request to create assessment: {}", assessmentRequest.getTitle());
        logger.debug("Authorization header: {}", authHeader);

        Map<String, String> response = new HashMap<>();
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("status", "error");
                response.put("message", "Authorization header missing or invalid");
                logger.error("Authorization header missing or invalid");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            String educatorEmail = jwtUtil.getEmailFromToken(token);
            if (!jwtUtil.validateToken(token, educatorEmail)) {
                response.put("status", "error");
                response.put("message", "Invalid token");
                logger.error("Invalid token");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            Assessment createdAssessment = assessmentService.createAssessment(assessmentRequest, educatorEmail);
            response.put("status", "success");
            response.put("message", "Assessment created successfully");
            response.put("assessmentId", createdAssessment.getAssessmentId().toString());
            logger.info("Assessment created successfully with ID: {} by educator: {}", createdAssessment.getAssessmentId(), educatorEmail);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            logger.error("Validation error while creating assessment: {}", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while creating assessment: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to create assessment due to an unexpected error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //api endpoint to list all assessments created by the educator
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getEducatorAssessments(
            @RequestHeader("Authorization") String authHeader) {
        logger.info("Received request to fetch assessments for educator");

        Map<String, Object> response = new HashMap<>();
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("status", "error");
                response.put("message", "Authorization header missing or invalid");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            String email = jwtUtil.getEmailFromToken(token);
            if (!jwtUtil.validateToken(token, email)) {
                response.put("status", "error");
                response.put("message", "Invalid token");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            List<Assessment> assessments = assessmentService.getEducatorAssessments(email);
            response.put("status", "success");
            response.put("assessments", assessments);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching assessments: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to fetch assessments due to an unexpected error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

   @PostMapping("/assign")
    public ResponseEntity<Map<String, String>> assignAssessment(
            @RequestBody Map<String, Object> requestBody,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("Received request to assign assessment with ID: {}", requestBody.get("assessmentId"));

        Map<String, String> response = new HashMap<>();
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("status", "error");
                response.put("message", "Authorization header missing or invalid");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            String email = jwtUtil.getEmailFromToken(token);
            if (!jwtUtil.validateToken(token, email)) {
                response.put("status", "error");
                response.put("message", "Invalid token");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            Long assessmentId = Long.valueOf(requestBody.get("assessmentId").toString());
            @SuppressWarnings("unchecked")
            List<Long> classIds = (List<Long>) requestBody.get("classIds");
            if (classIds == null || classIds.isEmpty()) {
                response.put("status", "error");
                response.put("message", "At least one class must be selected");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            Assessment updatedAssessment = assessmentService.assignAssessment(assessmentId, email, classIds);
            response.put("status", "success");
            response.put("message", "Assessment assigned successfully to selected classes");
            response.put("assessmentId", updatedAssessment.getAssessmentId().toString());
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException | IllegalStateException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Unexpected error while assigning assessment: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to assign assessment due to an unexpected error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}