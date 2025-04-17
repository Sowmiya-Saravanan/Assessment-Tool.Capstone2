package com.assesscraft.api.controller;

import com.assesscraft.api.model.*;
import com.assesscraft.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/educator-dashboard")
    public ResponseEntity<Map<String, Object>> getEducatorDashboard(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.getRole().equals(Role.EDUCATOR)) {
            throw new RuntimeException("Unauthorized: Only educators can access this endpoint");
        }

        // Assessment count created by the educator
        long assessmentCount = assessmentRepository.countByClassEntityIn(user.getClasses());

        // Student count across all classes taught by the educator
        long studentCount = user.getClasses().stream()
                .flatMap(c -> c.getStudents().stream())
                .distinct()
                .count();

        // Submission count for assessments created by the educator
        long submissionCount = submissionRepository.countByAssessmentIn(
                assessmentRepository.findByClassEntityIn(user.getClasses())
        );

        // Pending grading count (submissions not yet graded)
        long pendingGrading = submissionRepository.countByAssessmentInAndStatusNotIn(
                assessmentRepository.findByClassEntityIn(user.getClasses()),
                List.of(SubmissionStatus.GRADED, SubmissionStatus.PUBLISHED)
        );

        // Upcoming assessments (starting after now)
        LocalDateTime now = LocalDateTime.now();
        List<Assessment> upcomingAssessments = assessmentRepository.findByClassEntityInAndStartTimeAfter(user.getClasses(), now);

        response.put("assessmentCount", assessmentCount);
        response.put("studentCount", studentCount);
        response.put("submissionCount", submissionCount);
        response.put("pendingGrading", pendingGrading);
        response.put("upcomingAssessments", upcomingAssessments);

        return ResponseEntity.ok(response);
    }
}