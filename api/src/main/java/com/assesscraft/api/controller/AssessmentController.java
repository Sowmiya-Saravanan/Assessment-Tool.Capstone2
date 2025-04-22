package com.assesscraft.api.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.assesscraft.api.model.Assessment;
import com.assesscraft.api.model.AssessmentStatus;
import com.assesscraft.api.model.AssessmentType;
import com.assesscraft.api.model.Category;
import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.GradingMode;
import com.assesscraft.api.model.Question;
import com.assesscraft.api.model.QuestionType;
import com.assesscraft.api.repository.AssessmentRepository;
import com.assesscraft.api.repository.CategoryRepository;
import com.assesscraft.api.repository.ClassRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data/assessment")
public class AssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
public ResponseEntity<List<Assessment>> getAssessments(@RequestHeader("Authorization") String token) {
    String educatorId = getEducatorIdFromToken(token);
    if (educatorId == null) {
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    // Assuming assessments are filtered by createdBy or class educator
    List<Assessment> assessments = assessmentRepository.findByCreatedBy(educatorId); // Add this method to AssessmentRepository
    return new ResponseEntity<>(assessments, HttpStatus.OK);
}
    @PostMapping
    public ResponseEntity<?> createAssessment(@RequestBody Map<String, Object> assessmentPayload, @RequestHeader("Authorization") String token) {
        logger.debug("Received assessment creation request: {}", assessmentPayload);

        // Validate token (placeholder)
        String educatorId = getEducatorIdFromToken(token);
        if (educatorId == null) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        // Map payload to Assessment entity
        Assessment assessment = new Assessment();
        assessment.setTitle((String) assessmentPayload.get("title"));
        assessment.setDescription((String) assessmentPayload.get("description"));
        assessment.setType(AssessmentType.valueOf((String) assessmentPayload.get("type")));
        assessment.setDurationMinutes((Integer) assessmentPayload.get("durationMinutes"));
        assessment.setStartTime(LocalDateTime.parse((String) assessmentPayload.get("startTime")));
        assessment.setEndTime(LocalDateTime.parse((String) assessmentPayload.get("endTime")));
        assessment.setGradingMode(GradingMode.valueOf((String) assessmentPayload.get("gradingMode")));
        assessment.setStatus(AssessmentStatus.valueOf((String) assessmentPayload.get("status")));
        assessment.setAllowResumption((Boolean) assessmentPayload.getOrDefault("allowResumption", true));
        assessment.setMaxAttempts((Integer) assessmentPayload.getOrDefault("maxAttempts", 1));
        assessment.setResultsPublished((Boolean) assessmentPayload.getOrDefault("resultsPublished", false));

        // Set category
        Long categoryId = ((Number) assessmentPayload.get("categoryId")).longValue();
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        assessment.setCategory(category);

        // Set class (if provided)
        if (assessmentPayload.get("classId") != null) {
            Long classId = ((Number) assessmentPayload.get("classId")).longValue();
            Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));
            assessment.setClassEntity(classEntity);
        }

        // Set createdAt and updatedAt if provided, otherwise use defaults
        if (assessmentPayload.get("createdAt") != null) {
            assessment.setCreatedAt(LocalDateTime.parse((String) assessmentPayload.get("createdAt")));
        }
        if (assessmentPayload.get("updatedAt") != null) {
            assessment.setUpdatedAt(LocalDateTime.parse((String) assessmentPayload.get("updatedAt")));
        }
        assessment.setCreatedBy(educatorId);

        // Handle questions
        List<Map<String, Object>> questionMaps = (List<Map<String, Object>>) assessmentPayload.get("questions");
        if (questionMaps != null) {
            List<Question> questions = questionMaps.stream().map(q -> {
                Question question = new Question();
                question.setContent((String) q.get("content")); // JSON string
                question.setType(QuestionType.valueOf((String) q.get("type")));
                question.setMaxScore(((Number) q.get("maxScore")).doubleValue());
                question.setAssessment(assessment); // Set relationship
                return question;
            }).collect(Collectors.toList());
            assessment.setQuestions(questions);
        }

        // Save to database
        assessmentRepository.save(assessment);
        logger.info("Assessment created with ID: {}", assessment.getAssessmentId());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("assessmentId", assessment.getAssessmentId());
        return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
    }

    private String getEducatorIdFromToken(String token) {
        // Implement JWT token parsing or session validation
        // For now, return a dummy value
        return "educator123"; // Replace with actual logic
    }
}