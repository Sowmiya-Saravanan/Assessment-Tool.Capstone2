// package com.assessmenttool.assessment_tool_api.controller;


// import com.assessmenttool.assessment_tool_api.model.*;
// import com.assessmenttool.assessment_tool_api.service.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.oauth2.jwt.Jwt;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/api")
// public class AssessmentController {

//     @Autowired
//     private UserService userService;

//     @Autowired
//     private AssessmentService assessmentService;

//     @Autowired
//     private QuestionService questionService;

//     @Autowired
//     private AssessmentAssignmentService assessmentAssignmentService;

//     @Autowired
//     private SubmissionService submissionService;

//     // Public endpoint (no authentication required)
//     @GetMapping("/public/hello")
//     public ResponseEntity<String> helloPublic() {
//         return ResponseEntity.ok("Hello from public endpoint!");
//     }

//     // Logic 1: Educator - Create an Assessment
//     @PostMapping("/educator/assessments")
//     public ResponseEntity<Assessment> createAssessment(
//             @RequestBody Assessment assessment,
//             @AuthenticationPrincipal Jwt jwt) {
//         try {
//             User educator = userService.findOrCreateUser(jwt, Role.EDUCATOR);
//             Assessment savedAssessment = assessmentService.createAssessment(assessment, educator);
//             return ResponseEntity.ok(savedAssessment);
//         } catch (SecurityException e) {
//             return ResponseEntity.status(403).build(); // Forbidden if not an educator
//         }
//     }

//     // Logic 2: Educator - Add a Question to an Assessment
//     @PostMapping("/educator/assessments/{assessmentId}/questions")
//     public ResponseEntity<Question> addQuestion(
//             @PathVariable Long assessmentId,
//             @RequestBody Question question,
//             @AuthenticationPrincipal Jwt jwt) {
//         try {
//             User educator = userService.findOrCreateUser(jwt, Role.EDUCATOR);
//             Assessment assessment = assessmentService.findById(assessmentId)
//                     .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

//             if (!assessment.getEducator().getExternalId().equals(educator.getExternalId())) {
//                 return ResponseEntity.status(403).build(); // Forbidden if not the creator
//             }

//             Question savedQuestion = questionService.addQuestion(assessment, question);
//             return ResponseEntity.ok(savedQuestion);
//         } catch (IllegalArgumentException | IllegalStateException e) {
//             return ResponseEntity.badRequest().body(null);
//         } catch (SecurityException e) {
//             return ResponseEntity.status(403).build();
//         }
//     }

//     // Logic 3: Educator - Assign an Assessment to a Class or Student
//     @PostMapping("/educator/assessments/{assessmentId}/assign")
//     public ResponseEntity<AssessmentAssignment> assignAssessment(
//             @PathVariable Long assessmentId,
//             @RequestBody AssessmentAssignment assignment,
//             @AuthenticationPrincipal Jwt jwt) {
//         try {
//             User educator = userService.findOrCreateUser(jwt, Role.EDUCATOR);
//             Assessment assessment = assessmentService.findById(assessmentId)
//                     .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

//             if (!assessment.getEducator().getExternalId().equals(educator.getExternalId())) {
//                 return ResponseEntity.status(403).build(); // Forbidden if not the creator
//             }

//             AssessmentAssignment savedAssignment = assessmentAssignmentService.assignAssessment(assessment, assignment, educator.getExternalId());
//             return ResponseEntity.ok(savedAssignment);
//         } catch (IllegalArgumentException | IllegalStateException e) {
//             return ResponseEntity.badRequest().body(null);
//         } catch (SecurityException e) {
//             return ResponseEntity.status(403).build();
//         }
//     }

//     // Logic 4: Educator - Get All Assessments Created by the Educator
//     @GetMapping("/educator/assessments")
//     public ResponseEntity<List<Assessment>> getEducatorAssessments(
//             @AuthenticationPrincipal Jwt jwt) {
//         try {
//             User educator = userService.findOrCreateUser(jwt, Role.EDUCATOR);
//             List<Assessment> assessments = assessmentService.findByEducatorExternalId(educator.getExternalId());
//             return ResponseEntity.ok(assessments);
//         } catch (SecurityException e) {
//             return ResponseEntity.status(403).build();
//         }
//     }

//     // Logic 5: Student - Submit an Assessment
//     @PostMapping("/student/submissions")
//     public ResponseEntity<Submission> submitAssessment(
//             @RequestBody Submission submission,
//             @AuthenticationPrincipal Jwt jwt) {
//         try {
//             User student = userService.findOrCreateUser(jwt, Role.STUDENT);
//             Submission savedSubmission = submissionService.submitAssessment(submission, student);
//             return ResponseEntity.ok(savedSubmission);
//         } catch (IllegalStateException e) {
//             return ResponseEntity.badRequest().body(null);
//         } catch (SecurityException e) {
//             return ResponseEntity.status(403).build();
//         }
//     }

//     // Logic 6: Student - Get All Submissions by the Student
//     @GetMapping("/student/submissions")
//     public ResponseEntity<List<Submission>> getStudentSubmissions(
//             @AuthenticationPrincipal Jwt jwt) {
//         try {
//             User student = userService.findOrCreateUser(jwt, Role.STUDENT);
//             List<Submission> submissions = submissionService.findByStudentExternalId(student.getExternalId());
//             return ResponseEntity.ok(submissions);
//         } catch (SecurityException e) {
//             return ResponseEntity.status(403).build();
//         }
//     }

//     // Logic 7: Admin - Get All Submissions
//     @GetMapping("/admin/submissions")
//     public ResponseEntity<List<Submission>> getAllSubmissions(
//             @AuthenticationPrincipal Jwt jwt) {
//         try {
//             userService.findOrCreateUser(jwt, Role.ADMIN);
//             List<Submission> submissions = submissionService.findAll();
//             return ResponseEntity.ok(submissions);
//         } catch (SecurityException e) {
//             return ResponseEntity.status(403).build();
//         }
//     }
// }