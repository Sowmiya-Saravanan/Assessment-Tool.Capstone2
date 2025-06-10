package com.project.mvc.controller;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.mvc.dto.AssessmentCreateRequest;
import com.project.mvc.dto.ClassCreateRequest;
import com.project.mvc.service.ClassService;
import com.project.mvc.service.EmailService;
import com.project.mvc.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/educator")
public class EducatorController {

    private static final Logger logger = LoggerFactory.getLogger(EducatorController.class);

    @Autowired
    private ClassService classService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.project.mvc.service.AssessmentService assessmentService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

    @GetMapping("/dashboard")
    public String educatorDashboard(Model model, HttpServletRequest request) {
        logger.info("Rendering educator dashboard");

        String jwtToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwtToken".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        if (jwtToken == null) {
            logger.warn("JWT token not found. Redirecting to login.");
            return "redirect:/educator/login";
        }

        try {
            List<Map<String, Object>> classes = classService.getEducatorClasses(jwtToken);
            model.addAttribute("classes", classes);

            List<Map<String, Object>> assessments = assessmentService.getEducatorAssessments(jwtToken);
            model.addAttribute("assessments", assessments);
        } catch (Exception e) {
            logger.error("Error fetching data for dashboard: {}", e.getMessage());
            model.addAttribute("error", "Failed to load dashboard data: " + e.getMessage());
        }

        if (!model.containsAttribute("classCreateRequest")) {
            model.addAttribute("classCreateRequest", new ClassCreateRequest());
        }
        if (!model.containsAttribute("assessment")) {
            model.addAttribute("assessment", new AssessmentCreateRequest());
        }

        return "educator-dashboard";
    }

    @PostMapping("/send-invitation")
    public String sendClassInvitation(
            @RequestParam("classId") Long classId,
            @RequestParam("studentEmail") String studentEmail,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        logger.info("Sending class invitation to: {} for class ID: {}", studentEmail, classId);

        if (!EMAIL_PATTERN.matcher(studentEmail).matches()) {
            logger.warn("Invalid student email format: {}", studentEmail);
            redirectAttributes.addFlashAttribute("error", "Invalid student email format");
            return "redirect:/educator/dashboard";
        }

        String jwtToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwtToken".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        if (jwtToken == null) {
            logger.warn("JWT token not found. Redirecting to login.");
            redirectAttributes.addFlashAttribute("error", "Session expired. Please log in again.");
            return "redirect:/educator/login";
        }

        try {
            List<Map<String, Object>> classes = classService.getEducatorClasses(jwtToken);
            Map<String, Object> targetClass = classes.stream()
                    .filter(cls -> classId.equals(Long.valueOf(cls.get("classId").toString())))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Class not found with ID: " + classId));

            String className = (String) targetClass.get("className");
            String classCode = (String) targetClass.get("classCode");
            String educatorEmail = authService.getEmailFromToken(jwtToken);

            emailService.sendClassInvitationEmail(studentEmail, className, classCode, educatorEmail);
            redirectAttributes.addFlashAttribute("success", "Invitation email sent to " + studentEmail);
        } catch (Exception e) {
            logger.error("Error sending class invitation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to send invitation: " + e.getMessage());
        }

        return "redirect:/educator/dashboard";
    }

@PostMapping("/assessment/assign")
    public String assignAssessment(
            @RequestParam("assessmentId") Long assessmentId,
            @RequestParam("classIds") List<Long> classIds,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        logger.info("Assigning assessment with ID: {} to classes: {}", assessmentId, classIds);

        String jwtToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwtToken".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        if (jwtToken == null) {
            logger.warn("JWT token not found. Redirecting to login.");
            redirectAttributes.addFlashAttribute("error", "Session expired. Please log in again.");
            return "redirect:/educator/login";
        }

        try {
            String result = assessmentService.assignAssessment(assessmentId, classIds, jwtToken);
            redirectAttributes.addFlashAttribute("success", result);
        } catch (Exception e) {
            logger.error("Error assigning assessment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to assign assessment: " + e.getMessage());
        }

        return "redirect:/educator/dashboard";
    }

     @GetMapping("/class-details")
    @ResponseBody
    public Map<String, Object> getClassDetails(
            @RequestParam("classId") Long classId,
            HttpServletRequest request) {
        String jwtToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwtToken".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        if (jwtToken == null) {
            return Map.of("status", "error", "message", "Session expired");
        }

        try {
            Map<String, Object> classDetails = classService.getClassDetails(classId, jwtToken);
            return Map.of("status", "success", "classDetails", classDetails);
        } catch (Exception e) {
            logger.error("Error fetching class details: {}", e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @GetMapping("/debug-dashboard")
    public String debugDashboard(Model model) {
        model.addAttribute("classCreateRequest", new ClassCreateRequest());
        return "educator-dashboard";
    }
}