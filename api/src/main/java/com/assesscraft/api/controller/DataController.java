package com.assesscraft.api.controller;

import com.assesscraft.api.model.*;
import com.assesscraft.api.model.Class;
import com.assesscraft.api.repository.*;
import com.assesscraft.api.security.CustomUserDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
public class DataController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AssessmentRepository assessmentRepository;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private CategoryRepository categoryRepository; // Added

    @GetMapping("/educator-dashboard")
    public ResponseEntity<Map<String, Object>> getEducatorDashboard(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.getRole().equals(Role.EDUCATOR)) {
            throw new RuntimeException("Unauthorized: Only educators can access this endpoint");
        }

        long assessmentCount = assessmentRepository.countByClassEntityIn(user.getClasses());
        long studentCount = user.getClasses().stream()
                .flatMap(c -> c.getStudents().stream())
                .distinct()
                .count();
        long submissionCount = submissionRepository.countByAssessmentIn(
                assessmentRepository.findByClassEntityIn(user.getClasses())
        );
        long pendingGrading = submissionRepository.countByAssessmentInAndStatusNotIn(
                assessmentRepository.findByClassEntityIn(user.getClasses()),
                List.of(SubmissionStatus.GRADED, SubmissionStatus.PUBLISHED)
        );
        LocalDateTime now = LocalDateTime.now();
        List<Assessment> upcomingAssessments = assessmentRepository.findByClassEntityInAndStartTimeAfter(user.getClasses(), now);

        response.put("assessmentCount", assessmentCount);
        response.put("studentCount", studentCount);
        response.put("submissionCount", submissionCount);
        response.put("pendingGrading", pendingGrading);
        response.put("upcomingAssessments", upcomingAssessments.size()); // Return count for simplicity

        return ResponseEntity.ok(response);
    }

    // @PostMapping("/assessment")
    // public ResponseEntity<Assessment> createAssessment(@RequestBody Assessment assessment, Authentication authentication) {
    //     if (authentication == null || authentication.getPrincipal() == null) {
    //         return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    //     }

    //     CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    //     User educator = userDetails.getUser();
    //     assessment.setCreatedAt(LocalDateTime.now());
    //     assessment.setUpdatedAt(LocalDateTime.now());

    //     if (assessment.getClassEntity() != null && assessment.getClassEntity().getClassId() != null) {
    //         Class classEntity = classRepository.findById(assessment.getClassEntity().getClassId())
    //                 .orElseThrow(() -> new RuntimeException("Class not found"));
    //         if (!classEntity.getEducator().getUserId().equals(educator.getUserId())) {
    //             return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    //         }
    //         assessment.setClassEntity(classEntity);
    //     }

    //     Assessment savedAssessment = assessmentRepository.save(assessment);
    //     return new ResponseEntity<>(savedAssessment, HttpStatus.CREATED);
    // }

    @PostMapping("/categories")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody Map<String, Object> categoryPayload, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.getRole().equals(Role.EDUCATOR)) {
            throw new RuntimeException("Unauthorized: Only educators can create categories");
        }

        String name = (String) categoryPayload.get("name");
        if (name == null || name.trim().isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Category name is required"), HttpStatus.BAD_REQUEST);
        }

        Category category = new Category();
        category.setName(name);
        categoryRepository.save(category);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("categoryId", category.getCategoryId());
        return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
    }
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories(Authentication auth) {
        List<Category> categories = categoryRepository.findAll();
        return ResponseEntity.ok(categories);
    }
}