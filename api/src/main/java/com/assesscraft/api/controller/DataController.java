package com.assesscraft.api.controller;

import com.assesscraft.api.model.*;
import com.assesscraft.api.model.Class;
import com.assesscraft.api.repository.*;
import com.assesscraft.api.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private SubmissionService submissionService;

    @GetMapping("/educator-dashboard")
    public ResponseEntity<Map<String, Object>> getEducatorDashboard(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.getRole().equals(UserRole.EDUCATOR)) {
            throw new RuntimeException("Unauthorized: Only educators can access this endpoint");
        }

        // Fetch classes created by the educator
        List<Class> classes = classRepository.findByCreatedBy(user);
        long assessmentCount = assessmentRepository.countByCreatedBy(user);
        long studentCount = classRepository.countStudentsByCreatedBy(user);
        long submissionCount = submissionService.countByAssessmentCreatedBy(user);
        long pendingGrading = submissionService.countByAssessmentCreatedByAndStatusNotIn(
                user,
                List.of(SubmissionStatus.GRADED) // Exclude GRADED submissions, count SUBMITTED and PENDING
        );
        LocalDateTime now = LocalDateTime.now();
        long upcomingAssessments = assessmentRepository.countByCreatedByAndStartTimeAfter(user, now);

        response.put("assessmentCount", assessmentCount);
        response.put("studentCount", studentCount);
        response.put("submissionCount", submissionCount);
        response.put("pendingGrading", pendingGrading);
        response.put("upcomingAssessments", upcomingAssessments);

        return ResponseEntity.ok(response);
    }
}