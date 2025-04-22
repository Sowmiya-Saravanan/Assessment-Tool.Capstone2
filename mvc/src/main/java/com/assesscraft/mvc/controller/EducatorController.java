package com.assesscraft.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.assesscraft.mvc.model.ClassEntity;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/educator")
public class EducatorController {

    private static final Logger logger = LoggerFactory.getLogger(EducatorController.class);

    private final RestTemplate restTemplate;

    @Value("${app.api.base-url:http://localhost:8080}")
    private String apiBaseUrl = "http://localhost:8080"; // Hardcoded fallback

    public EducatorController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Displays the educator dashboard with dashboard data, drafted classes, and active classes.
     * @param session HTTP session to retrieve the authentication token
     * @param model Model to pass data to the view
     * @param classId Optional class ID to fetch pending students
     * @return View name or redirect URL
     */
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
            model.addAttribute("dashboardData", dashboardResponse.getBody());

            logger.debug("Fetching drafted classes from: {}", apiBaseUrl + "/api/data/classes/draft");
            ResponseEntity<List<Map<String, Object>>> classesResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/classes/draft",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            model.addAttribute("draftedClasses", classesResponse.getBody());

            logger.debug("Fetching active classes from: {}", apiBaseUrl + "/api/data/classes/active");
            ResponseEntity<List<Map<String, Object>>> activeClassesResponse = restTemplate.exchange(
                apiBaseUrl + "/api/data/classes/active",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            model.addAttribute("activeClasses", activeClassesResponse.getBody());

            model.addAttribute("classEntity", new ClassEntity());
            return "educator-dashboard";
        } catch (Exception e) {
            logger.error("Error fetching dashboard or classes: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load dashboard or classes: " + e.getMessage());
            model.addAttribute("classEntity", new ClassEntity());
            return "educator-dashboard";
        }
    }

    /**
     * Retrieves pending students for a specific class.
     * @param session HTTP session to retrieve the authentication token
     * @param classId ID of the class to fetch pending students for
     * @return List of pending students or error response
     */
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
            return pendingResponse;
        } catch (Exception e) {
            logger.error("Error fetching pending students: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates a new class based on the provided class entity.
     * @param classEntity Class entity with name and description
     * @param session HTTP session to retrieve the authentication token
     * @param redirectAttributes Redirect attributes for flash messages
     * @return Redirect URL
     */
    @PostMapping("/create-class")
    public String createClass(ClassEntity classEntity, HttpSession session, RedirectAttributes redirectAttributes) {
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

    /**
     * Assigns students to a class.
     * @param classId ID of the class to assign students to
     * @param studentIds List of student IDs to assign
     * @param session HTTP session to retrieve the authentication token
     * @param redirectAttributes Redirect attributes for flash messages
     * @return Redirect URL
     */
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

    /**
     * Bulk assigns students to a class using an Excel file.
     * @param classId ID of the class to assign students to
     * @param file Excel file containing student emails
     * @param session HTTP session to retrieve the authentication token
     * @param redirectAttributes Redirect attributes for flash messages
     * @return Redirect URL
     */
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

    /**
     * Approves pending students for a class.
     * @param classId ID of the class to approve students for
     * @param studentIds List of student IDs to approve
     * @param session HTTP session to retrieve the authentication token
     * @param redirectAttributes Redirect attributes for flash messages
     * @return Redirect URL
     */
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

    /**
     * Adds a student to a class.
     * @param classId ID of the class to add the student to
     * @param email Email of the student to add
     * @param session HTTP session to retrieve the authentication token
     * @param redirectAttributes Redirect attributes for flash messages
     * @return Redirect URL
     */
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

    /**
     * Removes a student from a class.
     * @param classId ID of the class to remove the student from
     * @param email Email of the student to remove
     * @param session HTTP session to retrieve the authentication token
     * @param redirectAttributes Redirect attributes for flash messages
     * @return Redirect URL
     */
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

    /**
     * Retrieves the list of students for a specific class.
     * @param classId ID of the class to fetch students for
     * @param session HTTP session to retrieve the authentication token
     * @return List of students or error response
     */
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

    /**
     * Deletes a class.
     * @param classId ID of the class to delete
     * @param session HTTP session to retrieve the authentication token
     * @param redirectAttributes Redirect attributes for flash messages
     * @return Redirect URL
     */
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

    /**
     * Logs out the educator and invalidates the session.
     * @param session HTTP session to invalidate
     * @return Redirect URL
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        logger.info("Logging out educator, invalidating session");
        session.invalidate();
        return "redirect:/educator/login";
    }
}