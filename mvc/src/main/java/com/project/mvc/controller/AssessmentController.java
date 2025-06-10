package com.project.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.mvc.dto.AssessmentCreateRequest;
import com.project.mvc.service.AssessmentService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/educator/assessment")
public class AssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    private AssessmentService assessmentService;

    @GetMapping("/create")
    public String showCreateAssessmentForm(HttpServletRequest request, RedirectAttributes redirectAttributes) {
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

        return "redirect:/educator/dashboard";
    }

    @PostMapping("/create")
    public String createAssessment(
            @ModelAttribute("assessment") AssessmentCreateRequest assessment,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        logger.info("Received request to create assessment: {}", assessment.getTitle());

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
            String result = assessmentService.createAssessment(assessment, jwtToken);
            if (result.contains("successful")) {
                logger.info("Assessment created successfully: {}", assessment.getTitle());
                redirectAttributes.addFlashAttribute("success", "Assessment created successfully!");
            } else {
                logger.error("Error creating assessment: {}", result);
                redirectAttributes.addFlashAttribute("error", result);
            }
        } catch (Exception e) {
            logger.error("Unexpected error creating assessment: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to create assessment: " + e.getMessage());
        }

        return "redirect:/educator/dashboard";
    }
}