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
import com.assesscraft.mvc.model.ClassEntity;
import com.assesscraft.mvc.service.EducatorDashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/educator")
public class EducatorController {

    private static final Logger logger = LoggerFactory.getLogger(EducatorController.class);

    private final EducatorDashboardService dashboardService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Autowired
    public EducatorController(EducatorDashboardService dashboardService, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.dashboardService = dashboardService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/dashboard")
    public String showEducatorDashboard(@RequestParam(value = "fetchPending", required = false) Long classId,
                                       @RequestParam(required = false) String success,
                                       Model model) {
        if (model.containsAttribute("error")) {
            return "educator-dashboard";
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        return "educator-dashboard";
    }

    @ModelAttribute
    public void addDashboardData(HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        if (token != null) {
            Map<String, Object> dashboardData = dashboardService.getDashboardData(token);
            model.addAllAttributes(dashboardData);
            model.addAttribute("assessment", new AssessmentForm());
            model.addAttribute("classEntity", new ClassEntity());
        } else {
            model.addAttribute("error", "Please log in to access the dashboard.");
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
    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
    
        try {
            Map<String, Object> classPayload = new HashMap<>();
            classPayload.put("className", classEntity.getClassName());
            classPayload.put("description", classEntity.getDescription());
            classPayload.put("classCode", UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            classPayload.put("status", "DRAFT");
    
            // Create UserDTO object
            Map<String, Object> userDTO = new HashMap<>();
            userDTO.put("userId", Long.valueOf(session.getAttribute("userId").toString()));
            classPayload.put("createdBy", userDTO);
    
            classPayload.put("createdAt", LocalDateTime.now().toString());
            classPayload.put("updatedAt", LocalDateTime.now().toString());
    
            // Send the Map directly as the body, let RestTemplate serialize it to JSON
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(classPayload, headers);
    
            logger.debug("Sending create class request to API: {}", classPayload);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/create-class",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
    
            if (response.getStatusCode() == HttpStatus.CREATED) {
                redirectAttributes.addFlashAttribute("success", "Class created successfully!");
                return "redirect:/educator/dashboard";
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to create class: " + response.getStatusCode());
                return "redirect:/educator/dashboard";
            }
        } catch (Exception e) {
            logger.error("Failed to create class: {}", e.getMessage(), e);
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
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            logger.debug("Assigning students to class {} with IDs: {}", classId, studentIds);

            // Validate class ownership
            ResponseEntity<Map> classResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            Map classData = classResponse.getBody();
            if (classData == null || classData.get("educator") == null || 
                !((Map)classData.get("educator")).get("userId").equals(session.getAttribute("userId"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid or unauthorized class selection");
                return "redirect:/educator/dashboard";
            }

            HttpEntity<List<Long>> entity = new HttpEntity<>(studentIds, headers); // Fixed: Send List<Long> directly

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

            // Validate class ownership
            ResponseEntity<Map> classResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders(headers)),
                Map.class
            );
            Map classData = classResponse.getBody();
            if (classData == null || classData.get("educator") == null || 
                !((Map)classData.get("educator")).get("userId").equals(session.getAttribute("userId"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid or unauthorized class selection");
                return "redirect:/educator/dashboard";
            }

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
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            logger.debug("Approving students for class {} with IDs: {}", classId, studentIds);

            // Validate class ownership
            ResponseEntity<Map> classResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            Map classData = classResponse.getBody();
            if (classData == null || classData.get("educator") == null || 
                !((Map)classData.get("educator")).get("userId").equals(session.getAttribute("userId"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid or unauthorized class selection");
                return "redirect:/educator/dashboard";
            }

            HttpEntity<List<Long>> entity = new HttpEntity<>(studentIds, headers); // Fixed: Send List<Long> directly

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
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            logger.debug("Adding student {} to class {}", email, classId);

            // Validate class ownership
            ResponseEntity<Map> classResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            Map classData = classResponse.getBody();
            if (classData == null || classData.get("educator") == null || 
                !((Map)classData.get("educator")).get("userId").equals(session.getAttribute("userId"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid or unauthorized class selection");
                return "redirect:/educator/dashboard";
            }

            Map<String, String> payload = new HashMap<>();
            payload.put("email", email);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers); // Fixed: Send Map directly

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
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            logger.debug("Removing student {} from class {}", email, classId);

            // Validate class ownership
            ResponseEntity<Map> classResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            Map classData = classResponse.getBody();
            if (classData == null || classData.get("educator") == null || 
                !((Map)classData.get("educator")).get("userId").equals(session.getAttribute("userId"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid or unauthorized class selection");
                return "redirect:/educator/dashboard";
            }

            Map<String, String> payload = new HashMap<>();
            payload.put("email", email);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers); // Fixed: Send Map directly

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

            // Validate class ownership
            ResponseEntity<Map> classResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            Map classData = classResponse.getBody();
            if (classData == null || classData.get("educator") == null || 
                !((Map)classData.get("educator")).get("userId").equals(session.getAttribute("userId"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid or unauthorized class selection");
                return "redirect:/educator/dashboard";
            }

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

            // Validate assessment ownership
            ResponseEntity<Map> assessmentResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/assessment/" + assessmentId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            Map assessmentData = assessmentResponse.getBody();
            if (assessmentData == null || assessmentData.get("createdBy") == null || 
                !((Map)assessmentData.get("createdBy")).get("userId").equals(session.getAttribute("userId"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid or unauthorized assessment selection");
                return "redirect:/educator/dashboard";
            }

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

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        logger.info("Logging out educator, invalidating session");
        session.invalidate();
        return "redirect:/educator/login";
    }
}