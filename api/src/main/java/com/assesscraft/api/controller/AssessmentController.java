package com.assesscraft.api.controller;

import com.assesscraft.api.model.*;
import com.assesscraft.api.model.Class;
import com.assesscraft.api.repository.*;
import com.assesscraft.api.service.AssessmentService;
import com.assesscraft.api.service.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private ClassRepository classRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository optionRepository;

    @Autowired
    private QuestionKeywordRepository keywordRepository;

    @Autowired
    private RubricCriteriaRepository rubricCriteriaRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private AssessmentService assessmentService; // Add AssessmentService

    @GetMapping
    public ResponseEntity<List<Assessment>> getAssessments(@RequestHeader("Authorization") String token) {
        String educatorId = getEducatorIdFromToken(token);
        if (educatorId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try {
            Long educatorIdLong = Long.valueOf(educatorId);
            List<Assessment> assessments = assessmentService.findByCreatedBy(educatorIdLong);
            return new ResponseEntity<>(assessments, HttpStatus.OK);
        } catch (NumberFormatException e) {
            logger.error("Invalid educatorId format: {}", educatorId, e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping
    public ResponseEntity<?> createAssessment(@RequestBody Map<String, Object> assessmentPayload, @RequestHeader("Authorization") String token) {
        logger.debug("Received assessment creation request: {}", assessmentPayload);

        String educatorId = getEducatorIdFromToken(token);
        if (educatorId == null) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        Assessment assessment = new Assessment();
        assessment.setTitle((String) assessmentPayload.get("title"));
        assessment.setDescription((String) assessmentPayload.get("description"));
        assessment.setType(AssessmentType.valueOf((String) assessmentPayload.get("type")));
        assessment.setDurationMinutes((Integer) assessmentPayload.get("durationMinutes"));
        assessment.setStartTime(LocalDateTime.parse((String) assessmentPayload.get("startTime")));
        assessment.setEndTime(LocalDateTime.parse((String) assessmentPayload.get("endTime")));
        assessment.setStatus(AssessmentStatus.valueOf((String) assessmentPayload.get("status")));
        assessment.setGradingMode(GradingMode.valueOf((String) assessmentPayload.get("gradingMode")));
        assessment.setCreatedAt(LocalDateTime.now());
        assessment.setUpdatedAt(LocalDateTime.now());

        User educator = userRepository.findById(Long.parseLong(educatorId))
                .orElseThrow(() -> new RuntimeException("Educator not found"));
        assessment.setCreatedBy(educator);

        // Handle classes
        if (assessmentPayload.get("classIds") != null) {
            List<Long> classIds = ((List<Number>) assessmentPayload.get("classIds")).stream()
                    .map(Number::longValue)
                    .collect(Collectors.toList());
            List<Class> classes = classRepository.findAllById(classIds);
            assessment.getClasses().addAll(classes);
        }

        // Handle questions
        List<Map<String, Object>> questionMaps = (List<Map<String, Object>>) assessmentPayload.get("questions");
        if (questionMaps != null) {
            List<Question> questions = questionMaps.stream().map(q -> {
                Question question = new Question();
                question.setText((String) q.get("text"));
                question.setType(QuestionType.valueOf((String) q.get("type")));
                question.setMaxScore(q.get("maxScore") != null ? ((Number) q.get("maxScore")).doubleValue() : 0.0);
                question.setCorrectAnswer((String) q.get("correctAnswer"));
                question.setInstructions((String) q.get("instructions"));
                question.setCreatedAt(LocalDateTime.now());
                question.setUpdatedAt(LocalDateTime.now());
                question.setAssessment(assessment);

                // Handle options for MCQ
                if ("MCQ".equals(q.get("type")) && q.get("options") != null) {
                    List<Map<String, Object>> options = (List<Map<String, Object>>) q.get("options");
                    question.setOptions(options.stream().map(opt -> {
                        QuestionOption option = new QuestionOption();
                        option.setOptionText((String) opt.get("optionText"));
                        option.setIsCorrect((Boolean) opt.get("isCorrect"));
                        option.setCreatedAt(LocalDateTime.now());
                        option.setUpdatedAt(LocalDateTime.now());
                        option.setQuestion(question);
                        return option;
                    }).collect(Collectors.toList()));
                }

                // Handle keywords for SHORT_ANSWER/ESSAY
                if (("SHORT_ANSWER".equals(q.get("type")) || "ESSAY".equals(q.get("type"))) && q.get("keywords") != null) {
                    List<Map<String, Object>> keywords = (List<Map<String, Object>>) q.get("keywords");
                    question.setKeywords(keywords.stream().map(kw -> {
                        QuestionKeyword keyword = new QuestionKeyword();
                        keyword.setKeyword((String) kw.get("keyword"));
                        keyword.setWeight(((Number) kw.get("weight")).doubleValue());
                        keyword.setCreatedAt(LocalDateTime.now());
                        keyword.setUpdatedAt(LocalDateTime.now());
                        keyword.setQuestion(question);
                        return keyword;
                    }).collect(Collectors.toList()));
                }

                // Handle rubric criteria
                if (q.get("rubricCriteria") != null) {
                    List<Map<String, Object>> criteria = (List<Map<String, Object>>) q.get("rubricCriteria");
                    question.setRubricCriteria(criteria.stream().map(c -> {
                        RubricCriteria rc = new RubricCriteria();
                        rc.setName((String) c.get("name"));
                        rc.setPoints(((Number) c.get("points")).doubleValue());
                        rc.setCreatedAt(LocalDateTime.now());
                        rc.setUpdatedAt(LocalDateTime.now());
                        rc.setQuestion(question);
                        return rc;
                    }).collect(Collectors.toList()));
                }

                return question;
            }).collect(Collectors.toList());
            assessment.setQuestions(questions);
        }

        assessmentRepository.save(assessment);
        logger.info("Assessment created with ID: {}", assessment.getAssessmentId());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("assessmentId", assessment.getAssessmentId());
        return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
    }

    @GetMapping("/{assessmentId}")
    public ResponseEntity<Assessment> getAssessment(@PathVariable Long assessmentId, @RequestHeader("Authorization") String token) {
        String educatorId = getEducatorIdFromToken(token);
        if (educatorId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
        if (!assessment.getCreatedBy().getUserId().toString().equals(educatorId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(assessment, HttpStatus.OK);
    }

    private String getEducatorIdFromToken(String token) {
        try {
            String email = jwtTokenService.getUsernameFromToken(token.replace("Bearer ", ""));
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found for token"));
            if (!user.getRole().equals(UserRole.EDUCATOR)) {
                throw new RuntimeException("Unauthorized: Only educators can access this endpoint");
            }
            return user.getUserId().toString();
        } catch (Exception e) {
            logger.error("Failed to extract educator ID from token: {}", e.getMessage(), e);
            return null;
        }
    }
}