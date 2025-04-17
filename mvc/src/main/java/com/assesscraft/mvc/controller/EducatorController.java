package com.assesscraft.mvc.controller;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.MediaType;
import com.assesscraft.mvc.model.ClassEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/educator")
public class EducatorController {

    private static final Logger logger = LoggerFactory.getLogger(EducatorController.class);

    private final RestTemplate restTemplate;

    @Value("${app.api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    public EducatorController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
            logger.debug("Fetching dashboard data from: {}", apiBaseUrl + "/api/data/educator-dashboard");
            ResponseEntity<Map<String, Object>> dashboardResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/educator-dashboard",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            logger.debug("Dashboard response - Status: {}, Body: {}", dashboardResponse.getStatusCode(), dashboardResponse.getBody());
            model.addAttribute("dashboardData", dashboardResponse.getBody());

            logger.debug("Fetching drafted classes from: {}", apiBaseUrl + "/api/data/classes/draft");
            ResponseEntity<List<Map<String, Object>>> classesResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/classes/draft",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            logger.debug("Drafted classes response - Status: {}, Body: {}", classesResponse.getStatusCode(), classesResponse.getBody());
            model.addAttribute("draftedClasses", classesResponse.getBody());

            model.addAttribute("classEntity", new ClassEntity());
            return "educator-dashboard";
        } catch (Exception e) {
            logger.error("Error fetching dashboard or classes: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load dashboard or classes: " + e.getMessage());
            model.addAttribute("classEntity", new ClassEntity());
            return "educator-dashboard";
        }
    }

    @GetMapping("/dashboard/pending-students")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPendingStudents(HttpSession session, @RequestParam Long classId) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            logger.debug("Fetching pending students for class {} from: {}", classId, apiBaseUrl + "/api/data/class/" + classId + "/pending-students");
            ResponseEntity<List<Map<String, Object>>> pendingResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/class/" + classId + "/pending-students",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            logger.debug("Pending students response - Status: {}, Body: {}", pendingResponse.getStatusCode(), pendingResponse.getBody());
            return pendingResponse;
        } catch (Exception e) {
            logger.error("Error fetching pending students: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/create-class")
    public String createClass(ClassEntity classEntity, Model model, HttpSession session) {
        String token = (String) session.getAttribute("token");
        if (token == null) {
            logger.warn("No token found in session, redirecting to login");
            return "redirect:/educator/login?error=Session expired";
        }

        if (classEntity.getClassName() == null || classEntity.getClassName().trim().isEmpty()) {
            model.addAttribute("error", "Class name is required");
            model.addAttribute("classEntity", classEntity);
            return "educator-dashboard";
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
                return "redirect:/educator/dashboard?success=Class created successfully!";
            } else {
                model.addAttribute("error", "Failed to create class: " + response.getStatusCode());
                model.addAttribute("classEntity", classEntity);
                return "educator-dashboard";
            }
        } catch (Exception e) {
            logger.error("Error creating class: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to create class: " + e.getMessage());
            model.addAttribute("classEntity", classEntity);
            return "educator-dashboard";
        }
    }

    @PostMapping("/class/{classId}/assign-students")
    public String assignStudents(@PathVariable Long classId, @RequestBody List<Long> studentIds, HttpSession session, Model model) {
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
                return "redirect:/educator/dashboard?success=Students assigned successfully!";
            } else {
                model.addAttribute("error", "Failed to assign students: " + response.getStatusCode());
                return "educator-dashboard";
            }
        } catch (Exception e) {
            logger.error("Error assigning students: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to assign students: " + e.getMessage());
            return "educator-dashboard";
        }
    }

    @PostMapping("/class/{classId}/bulk-students")
public String bulkAssignStudents(@PathVariable Long classId, @RequestParam(value = "classId", required = false) Long paramClassId, @RequestParam("file") MultipartFile file, HttpSession session, Model model) {
    String token = (String) session.getAttribute("token");
    if (token == null) {
        logger.warn("No token found in session, redirecting to login");
        return "redirect:/educator/login?error=Session expired";
    }

    // Fallback to parameter if path variable is invalid
    if (classId == null && paramClassId != null) {
        classId = paramClassId;
    }
    if (classId == null) {
        model.addAttribute("error", "Invalid class ID");
        model.addAttribute("classEntity", new ClassEntity());
        return "educator-dashboard";
    }

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    // Construct multipart request
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file.getResource()); // Add the file resource

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
            logger.info("Bulk students assigned successfully to class {}", classId);
            return "redirect:/educator/dashboard?success=Bulk students assigned successfully!";
        } else {
            model.addAttribute("error", "Failed to bulk assign students: " + response.getStatusCode());
            model.addAttribute("classEntity", new ClassEntity());
            return "educator-dashboard";
        }
    } catch (Exception e) {
        logger.error("Error bulk assigning students: {}", e.getMessage(), e);
        model.addAttribute("error", "Failed to bulk assign students: " + e.getMessage());
        model.addAttribute("classEntity", new ClassEntity());
        return "educator-dashboard";
    }
}
    @PostMapping("/class/{classId}/approve-students")
    public String approveStudents(@PathVariable Long classId, @RequestBody List<Long> studentIds, HttpSession session, Model model) {
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
                return "redirect:/educator/dashboard?success=Students approved successfully!";
            } else {
                model.addAttribute("error", "Failed to approve students: " + response.getStatusCode());
                return "educator-dashboard";
            }
        } catch (Exception e) {
            logger.error("Error approving students: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to approve students: " + e.getMessage());
            return "educator-dashboard";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        logger.info("Logging out educator, invalidating session");
        session.invalidate();
        return "redirect:/educator/login";
    }
}