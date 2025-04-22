package com.assesscraft.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.assesscraft.mvc.model.AssessmentForm;
import com.assesscraft.mvc.model.CategoryDto;
import com.assesscraft.mvc.model.ClassEntity;
import com.assesscraft.mvc.model.AssessmentDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/educator")
public class EducatorController {

    private static final Logger logger = LoggerFactory.getLogger(EducatorController.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AssessmentController assessmentController;

    @Value("${app.api.base-url:http://localhost:8080}")
    private String apiBaseUrl = "http://localhost:8080";

    @Autowired
    public EducatorController(RestTemplate restTemplate, ObjectMapper objectMapper, AssessmentController assessmentController) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.assessmentController = assessmentController;
    }

    @GetMapping("/dashboard")
    public String showEducatorDashboard(HttpSession session, Model model, @RequestParam(value = "fetchPending", required = false) Long classId) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            fetchDashboardSummary(model, entity);
            fetchDraftedClasses(model, entity);
            fetchActiveClasses(model, entity);
            assessmentController.fetchCategories(model, entity);
            fetchClassesForAssessment(model, entity);
            fetchAssessments(model, entity);
            model.addAttribute("assessment", new AssessmentForm());
            return "educator-dashboard";
        } catch (Exception e) {
            logger.error("Error fetching dashboard data: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load dashboard: " + e.getMessage());
            model.addAttribute("assessment", new AssessmentForm());
            return "educator-dashboard";
        }
    }

    private void fetchDashboardSummary(Model model, HttpEntity<String> entity) {
        logger.debug("Fetching dashboard summary from: {}", apiBaseUrl + "/api/data/educator-dashboard");
        ResponseEntity<Map<String, Object>> dashboardResponse = restTemplate.exchange(
            apiBaseUrl + "/api/data/educator-dashboard",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        model.addAttribute("dashboardData", dashboardResponse.getBody());
    }

    private void fetchDraftedClasses(Model model, HttpEntity<String> entity) {
        logger.debug("Fetching drafted classes from: {}", apiBaseUrl + "/api/data/classes/draft");
        ResponseEntity<List<Map<String, Object>>> classesResponse = restTemplate.exchange(
            apiBaseUrl + "/api/data/classes/draft",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        model.addAttribute("draftedClasses", classesResponse.getBody() != null ? classesResponse.getBody() : new ArrayList<>());
    }

    private void fetchActiveClasses(Model model, HttpEntity<String> entity) {
        logger.debug("Fetching active classes from: {}", apiBaseUrl + "/api/data/classes/active");
        ResponseEntity<List<Map<String, Object>>> activeClassesResponse = restTemplate.exchange(
            apiBaseUrl + "/api/data/classes/active",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        List<Map<String, Object>> activeClasses = activeClassesResponse.getBody();
        logger.debug("Raw active classes from API: {}", activeClasses);
        model.addAttribute("activeClasses", activeClasses != null ? activeClasses : new ArrayList<>());
    }

    private void fetchClassesForAssessment(Model model, HttpEntity<String> entity) {
        logger.debug("Fetching classes for assessment from: {}", apiBaseUrl + "/api/data/classes/active");
        ResponseEntity<List<Map<String, Object>>> classesResponse = restTemplate.exchange(
            apiBaseUrl + "/api/data/classes/active",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        List<Map<String, Object>> classesData = classesResponse.getBody();
        model.addAttribute("classes", classesData != null ? classesData : new ArrayList<>());
    }

    private void fetchAssessments(Model model, HttpEntity<String> entity) {
        logger.debug("Fetching assessments from: {}", apiBaseUrl + "/api/data/assessment");
        ResponseEntity<List<AssessmentDto>> assessmentsResponse = restTemplate.exchange(
            apiBaseUrl + "/api/data/assessment",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<AssessmentDto>>() {}
        );
        if (assessmentsResponse.getStatusCode() == HttpStatus.OK) {
            List<AssessmentDto> assessments = assessmentsResponse.getBody();
            List<Map<String, Object>> assessmentMaps = assessments != null ? assessments.stream().map(assessment -> {
                Map<String, Object> map = new HashMap<>();
                map.put("assessmentId", assessment.getAssessmentId());
                map.put("title", assessment.getTitle());
                return map;
            }).collect(Collectors.toList()) : new ArrayList<>();
            model.addAttribute("assessments", assessmentMaps);
        } else {
            model.addAttribute("error", "Failed to load assessments: " + assessmentsResponse.getStatusCode());
        }
    }

    @GetMapping("/dashboard/sent-invitations")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSentInvitations(HttpSession session, @RequestParam Long classId) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            logger.debug("Fetching sent invitations for class {} from: {}", classId, apiBaseUrl + "/api/data/class/" + classId + "/sent-invitations");
            ResponseEntity<List<Map<String, Object>>> sentResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId + "/sent-invitations",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return sentResponse;
        } catch (Exception e) {
            logger.error("Error fetching sent invitations: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/create-class")
    public String createClass(@ModelAttribute ClassEntity classEntity, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        if (classEntity.getClassName() == null || classEntity.getClassName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Class name is required");
            return "redirect:/educator/dashboard";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<ClassEntity> entity = new HttpEntity<>(classEntity, headers);

        try {
            logger.debug("Creating class with data: {}", classEntity);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/class",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("Class created successfully");
                redirectAttributes.addFlashAttribute("success", "Class created successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to create class: " + response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error creating class: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to create class: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @PostMapping("/class/{classId}/assign-students")
    public String assignStudents(@PathVariable Long classId, @RequestBody List<Long> studentIds, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<List<Long>> entity = new HttpEntity<>(studentIds, headers);

        try {
            logger.debug("Assigning students to class {} with IDs: {}", classId, studentIds);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId + "/students",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Students assigned successfully to class {}", classId);
                redirectAttributes.addFlashAttribute("success", "Students assigned successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to assign students: " + response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error assigning students: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to assign students: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @PostMapping("/class/{classId}/bulk-students")
    public String bulkAssignStudents(@PathVariable Long classId, @RequestParam("file") MultipartFile file, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        if (classId == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid class ID");
            return "redirect:/educator/dashboard";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            logger.debug("Bulk assigning students to class {} with file: {}", classId, file.getOriginalFilename());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId + "/bulk-students",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                logger.info("Bulk students assigned to class {}, response: {}", classId, responseBody);
                if (responseBody != null) {
                    if (responseBody.containsKey("invalidEmails")) {
                        redirectAttributes.addFlashAttribute("warning", "Bulk upload successful, but invalid emails: " + responseBody.get("invalidEmails"));
                    }
                    if (responseBody.containsKey("emailFailures")) {
                        redirectAttributes.addFlashAttribute("warning", "Email sending failed for: " + responseBody.get("emailFailures"));
                    } else {
                        redirectAttributes.addFlashAttribute("success", "Bulk students assigned successfully!");
                    }
                } else {
                    redirectAttributes.addFlashAttribute("success", "Bulk students assigned successfully!");
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to bulk assign students: " + response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error bulk assigning students: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to bulk assign students: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @PostMapping("/class/{classId}/approve-students")
    public String approveStudents(@PathVariable Long classId, @RequestBody List<Long> studentIds, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<List<Long>> entity = new HttpEntity<>(studentIds, headers);

        try {
            logger.debug("Approving students for class {} with IDs: {}", classId, studentIds);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId + "/approve-students",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Students approved successfully for class {}", classId);
                redirectAttributes.addFlashAttribute("success", "Students approved successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to approve students: " + response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error approving students: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to approve students: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @PostMapping("/class/{classId}/add-student")
    public String addStudent(@PathVariable Long classId, @RequestParam String email, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(email, headers);

        try {
            logger.debug("Adding student {} to class {}", email, classId);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId + "/add-student",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Student added successfully to class {}", classId);
                redirectAttributes.addFlashAttribute("success", "Student added successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to add student: " + response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error adding student: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to add student: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @PostMapping("/class/{classId}/remove-student")
    public String removeStudent(@PathVariable Long classId, @RequestParam String email, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(email, headers);

        try {
            logger.debug("Removing student {} from class {}", email, classId);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId + "/remove-student",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Student removed successfully from class {}", classId);
                redirectAttributes.addFlashAttribute("success", "Student removed successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to remove student: " + response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error removing student: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to remove student: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @GetMapping("/class/{classId}/students")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getStudents(@PathVariable Long classId, HttpSession session) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            logger.debug("Fetching students for class {} from: {}", classId, apiBaseUrl + "/api/data/class/" + classId + "/students");
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId + "/students",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response;
        } catch (Exception e) {
            logger.error("Error fetching students: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/class/{classId}")
    public String deleteClass(@PathVariable Long classId, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            logger.debug("Deleting class {}", classId);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId,
                HttpMethod.DELETE,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Class deleted successfully: {}", classId);
                redirectAttributes.addFlashAttribute("success", "Class deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to delete class: " + response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error deleting class: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete class: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @DeleteMapping("/assessment/{assessmentId}")
    public String deleteAssessment(@PathVariable Long assessmentId, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            logger.debug("Deleting assessment {}", assessmentId);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/assessment/" + assessmentId,
                HttpMethod.DELETE,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Assessment deleted successfully: {}", assessmentId);
                redirectAttributes.addFlashAttribute("success", "Assessment deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to delete assessment: " + response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error deleting assessment: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete assessment: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @PostMapping("/create-category")
    public String createCategory(@RequestParam String name, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> categoryPayload = new HashMap<>();
        categoryPayload.put("name", name);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(categoryPayload, headers);

        try {
            logger.debug("Creating category with name: {}", name);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/categories",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("Category created successfully");
                redirectAttributes.addFlashAttribute("success", "Category created successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to create category: " + response.getStatusCode());
            }
            return "redirect:/educator/dashboard";
        } catch (Exception e) {
            logger.error("Error creating category: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to create category: " + e.getMessage());
            return "redirect:/educator/dashboard";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        logger.info("Logging out educator, invalidating session");
        session.invalidate();
        return "redirect:/educator/login";
    }
}