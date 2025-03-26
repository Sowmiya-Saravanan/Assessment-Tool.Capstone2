package com.assessmenttool.assessment_tool_api.controller;

import com.assessmenttool.assessment_tool_api.model.Analytics;
import com.assessmenttool.assessment_tool_api.model.MetricType;
import com.assessmenttool.assessment_tool_api.model.Role;
import com.assessmenttool.assessment_tool_api.model.Submission;
import com.assessmenttool.assessment_tool_api.model.User;
import com.assessmenttool.assessment_tool_api.service.AnalyticsService;
import com.assessmenttool.assessment_tool_api.service.SubmissionService;
import com.assessmenttool.assessment_tool_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private AnalyticsService analyticsService;

    // Logic 1: Create a User
    @PostMapping("/users")
    public ResponseEntity<User> createUser(
            @RequestBody User user,
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        User createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    // Logic 2: Read a User by ID
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        User user = userService.findById(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    // Logic 3: Read All Users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        List<User> users = userService.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    // Logic 4: Update a User
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User updatedUser,
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        User user = userService.updateUser(id, updatedUser);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    // Logic 5: Delete a User
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Logic 6: View All Submissions
    @GetMapping("/submissions")
    public ResponseEntity<List<Submission>> getAllSubmissions(
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        List<Submission> submissions = submissionService.findAll();
        return new ResponseEntity<>(submissions, HttpStatus.OK);
    }

    // Logic 7: View All Assessment Analytics
    @GetMapping("/analytics")
    public ResponseEntity<List<Analytics>> getAssessmentAnalytics(
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        List<Analytics> analytics = analyticsService.getAssessmentAnalytics();
        return new ResponseEntity<>(analytics, HttpStatus.OK);
    }

    // Logic 8: View Analytics by Assessment
    @GetMapping("/analytics/assessment/{assessmentId}")
    public ResponseEntity<List<Analytics>> getAnalyticsByAssessment(
            @PathVariable Long assessmentId,
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        List<Analytics> analytics = analyticsService.getAnalyticsByAssessment(assessmentId);
        return new ResponseEntity<>(analytics, HttpStatus.OK);
    }

    // Logic 9: View Analytics by Class
    @GetMapping("/analytics/class/{classId}")
    public ResponseEntity<List<Analytics>> getAnalyticsByClass(
            @PathVariable Long classId,
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        List<Analytics> analytics = analyticsService.getAnalyticsByClass(classId);
        return new ResponseEntity<>(analytics, HttpStatus.OK);
    }

    // Logic 10: View Analytics by Metric Type
    @GetMapping("/analytics/metric")
    public ResponseEntity<List<Analytics>> getAnalyticsByMetricType(
            @RequestParam MetricType metricType,
            @AuthenticationPrincipal Jwt jwt) {
        userService.findOrCreateUser(jwt, Role.ADMIN);
        List<Analytics> analytics = analyticsService.getAnalyticsByMetricType(metricType);
        return new ResponseEntity<>(analytics, HttpStatus.OK);
    }
}