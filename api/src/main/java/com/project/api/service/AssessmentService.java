package com.project.api.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.project.api.model.Class;
import com.project.api.model.Assessment;
import com.project.api.model.AssessmentStatus;
import com.project.api.model.AssessmentType;
import com.project.api.model.GradingMode;
import com.project.api.model.Question;
import com.project.api.model.QuestionKeyword;
import com.project.api.model.QuestionOption;
import com.project.api.model.QuestionType;
import com.project.api.model.User;
import com.project.api.model.UserRole;
import com.project.api.repository.AssessmentRepository;
import com.project.api.repository.ClassRepository;
import com.project.api.repository.UserRepository;
import com.project.api.dto.AssessmentCreateRequest;

@Service
public class AssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired private ClassRepository classRepository;
    @Transactional
    public Assessment createAssessment(AssessmentCreateRequest request, String educatorEmail) {
        logger.info("Creating assessment for educator: {}", educatorEmail);

        Optional<User> educatorOpt = userRepository.findByEmail(educatorEmail);
        if (educatorOpt.isEmpty() || educatorOpt.get().getRole() != UserRole.EDUCATOR) {
            logger.error("User {} is not an educator or does not exist", educatorEmail);
            throw new IllegalArgumentException("Only educators can create assessments");
        }
        User educator = educatorOpt.get();

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Assessment title is required");
        }

        AssessmentType type;
        try {
            type = AssessmentType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid assessment type: " + request.getType());
        }

        if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0");
        }

        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Start and end times are required");
        }

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        GradingMode gradingMode;
        try {
            gradingMode = GradingMode.valueOf(request.getGradingMode().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid grading mode: " + request.getGradingMode());
        }

        Assessment assessment = new Assessment();
        assessment.setTitle(request.getTitle());
        assessment.setDescription(request.getDescription());
        assessment.setType(type);
        assessment.setDurationMinutes(request.getDurationMinutes());
        assessment.setStartTime(request.getStartTime());
        assessment.setEndTime(request.getEndTime());
        assessment.setGradingMode(gradingMode);
        assessment.setCreatedBy(educator);
        assessment.setStatus(AssessmentStatus.DRAFT);
        assessment.setCreatedAt(LocalDateTime.now());
        assessment.setUpdatedAt(LocalDateTime.now());

        if (request.getQuestions() != null) {
            List<Question> questions = new ArrayList<>();
            for (AssessmentCreateRequest.Question questionRequest : request.getQuestions()) {
                Question question = new Question();
                question.setText(questionRequest.getText());
                question.setType(QuestionType.valueOf(questionRequest.getType().toUpperCase()));
                question.setMaxScore(questionRequest.getMaxScore());
                question.setCorrectAnswer(questionRequest.getCorrectAnswer());
                question.setAssessment(assessment);
                question.setCreatedAt(LocalDateTime.now());
                question.setUpdatedAt(LocalDateTime.now());

                if (question.getType() == QuestionType.MCQ) {
                    if (questionRequest.getCorrectAnswer() == null) {
                        throw new IllegalArgumentException("Correct option must be specified for MCQ question: " + questionRequest.getText());
                    }
                    int correctOptionIndex = Integer.parseInt(questionRequest.getCorrectAnswer());
                    if (questionRequest.getOptions() == null || questionRequest.getOptions().size() <= correctOptionIndex) {
                        throw new IllegalArgumentException("Invalid correct option index for MCQ question: " + questionRequest.getText());
                    }
                    List<QuestionOption> options = new ArrayList<>();
                    for (int i = 0; i < questionRequest.getOptions().size(); i++) {
                        AssessmentCreateRequest.Option optionRequest = questionRequest.getOptions().get(i);
                        QuestionOption option = new QuestionOption();
                        option.setOptionText(optionRequest.getText());
                        option.setIsCorrect(i == correctOptionIndex);
                        option.setQuestion(question);
                        option.setCreatedAt(LocalDateTime.now());
                        option.setUpdatedAt(LocalDateTime.now());
                        options.add(option);
                    }
                    question.setOptions(options);
                }

                if (question.getType() == QuestionType.TRUE_FALSE) {
                    if (questionRequest.getCorrectAnswer() == null || (!questionRequest.getCorrectAnswer().equals("true") && !questionRequest.getCorrectAnswer().equals("false"))) {
                        throw new IllegalArgumentException("Correct answer must be 'true' or 'false' for True/False question: " + questionRequest.getText());
                    }
                }

                if (question.getType() == QuestionType.SHORT_ANSWER || question.getType() == QuestionType.ESSAY) {
                    if (questionRequest.getKeywords() == null || questionRequest.getKeywords().isEmpty()) {
                        throw new IllegalArgumentException("At least one keyword is required for Short Answer or Essay question: " + questionRequest.getText());
                    }
                    double totalPoints = 0;
                    List<QuestionKeyword> keywords = new ArrayList<>();
                    for (AssessmentCreateRequest.Keyword keywordRequest : request.getQuestions().get(0).getKeywords()) {
                        QuestionKeyword keyword = new QuestionKeyword();
                        keyword.setKeyword(keywordRequest.getKeyword());
                        keyword.setWeight(keywordRequest.getWeight());
                        keyword.setQuestion(question);
                        keyword.setCreatedAt(LocalDateTime.now());
                        keyword.setUpdatedAt(LocalDateTime.now());
                        totalPoints += keyword.getWeight();
                        keywords.add(keyword);
                    }
                    question.setMaxScore(totalPoints);
                    question.setKeywords(keywords);
                }

                questions.add(question);
            }
            assessment.setQuestions(questions);
        }

        Assessment savedAssessment = assessmentRepository.save(assessment);
        logger.info("Assessment created successfully: assessmentId={}, title={}", savedAssessment.getAssessmentId(), savedAssessment.getTitle());
        return savedAssessment;
    }

    public List<Assessment> getEducatorAssessments(String educatorEmail) {
        logger.info("Fetching assessments for educator: {}", educatorEmail);

        Optional<User> userOpt = userRepository.findByEmail(educatorEmail);
        if (userOpt.isEmpty() || userOpt.get().getRole() != UserRole.EDUCATOR) {
            logger.error("User {} is not an educator or does not exist", educatorEmail);
            throw new IllegalArgumentException("Only educators can view their assessments");
        }
        User educator = userOpt.get();

        List<Assessment> assessments = assessmentRepository.findByCreatedBy(educator);
        logger.info("Found {} assessments for educator: {}", assessments.size(), educatorEmail);
        return assessments;
    }

   @Transactional
    public Assessment assignAssessment(Long assessmentId, String educatorEmail, List<Long> classIds) {
        logger.info("Assigning assessment with ID: {} to classes: {} by educator: {}", 
                    assessmentId, classIds, educatorEmail);

        Optional<User> userOpt = userRepository.findByEmail(educatorEmail);
        if (userOpt.isEmpty() || userOpt.get().getRole() != UserRole.EDUCATOR) {
            logger.error("User {} is not an educator or does not exist", educatorEmail);
            throw new IllegalArgumentException("Only educators can assign assessments");
        }
        User educator = userOpt.get();

        Optional<Assessment> assessmentOpt = assessmentRepository.findById(assessmentId);
        if (assessmentOpt.isEmpty()) {
            logger.error("Assessment with ID {} not found", assessmentId);
            throw new IllegalArgumentException("Assessment not found");
        }
        Assessment assessment = assessmentOpt.get();

        if (!assessment.getCreatedBy().getEmail().equals(educatorEmail)) {
            logger.error("Educator {} is not authorized to assign assessment {}", educatorEmail, assessmentId);
            throw new IllegalArgumentException("You are not authorized to assign this assessment");
        }

        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            logger.error("Assessment {} is not in DRAFT status, current status: {}", 
                         assessmentId, assessment.getStatus());
            throw new IllegalStateException("Only DRAFT assessments can be assigned");
        }

        List<Class> classes = classRepository.findAllById(classIds);
        if (classes.size() != classIds.size()) {
            logger.error("One or more class IDs are invalid: {}", classIds);
            throw new IllegalArgumentException("One or more class IDs are invalid");
        }

        for (Class cls : classes) {
            if (!cls.getCreatedBy().getEmail().equals(educatorEmail)) {
                logger.error("Class {} not created by educator {}", cls.getClassId(), educatorEmail);
                throw new IllegalArgumentException("You are not authorized to assign to class: " + cls.getClassId());
            }
        }

        assessment.setClasses(classes);
        assessment.setStatus(AssessmentStatus.ASSIGNED);
        assessment.setUpdatedAt(LocalDateTime.now());
        Assessment updatedAssessment = assessmentRepository.save(assessment);
        logger.info("Assessment {} successfully assigned to classes: {}", assessmentId, classIds);
        return updatedAssessment;
    }

    @Scheduled(cron = "0 * * * * *") // Runs every minute
    public void updateAssessmentStatuses() {
        logger.info("Running scheduled task to update assessment statuses");

        LocalDateTime now = LocalDateTime.now();
        List<Assessment> assessments = assessmentRepository.findAll();

        for (Assessment assessment : assessments) {
            if (assessment.getStatus() == AssessmentStatus.ASSIGNED 
                && assessment.getStartTime() != null 
                && now.isAfter(assessment.getStartTime()) 
                && now.isBefore(assessment.getEndTime())) {
                assessment.setStatus(AssessmentStatus.ACTIVE);
                assessment.setUpdatedAt(now);
                logger.info("Assessment {} transitioned to ACTIVE", assessment.getAssessmentId());
            } else if (assessment.getStatus() == AssessmentStatus.ACTIVE 
                       && assessment.getEndTime() != null 
                       && now.isAfter(assessment.getEndTime())) {
                assessment.setStatus(AssessmentStatus.COMPLETED);
                assessment.setUpdatedAt(now);
                logger.info("Assessment {} transitioned to COMPLETED", assessment.getAssessmentId());
            }
        }
        assessmentRepository.saveAll(assessments);
    }
}