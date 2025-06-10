package com.project.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.mvc.dto.ClassCreateRequest;
import com.project.mvc.service.ClassService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
@Controller
@RequestMapping("/educator")
public class ClassController {


    @Autowired private ClassService classService;

    private static final Logger logger = LoggerFactory.getLogger(ClassController.class);

    @PostMapping("/create-class")
    public String createClass(
            @ModelAttribute("classCreateRequest") ClassCreateRequest request,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes) {
        logger.info("Received request to create class: {}", request.getClassName());
        // Extract JWT token from cookies
        String jwtToken = null;
    
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwtToken".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }
        if (jwtToken == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to create a class");
            logger.warn("JWT token not found in cookies");
            return "redirect:/educator/login";
        }
        String result = classService.createClass(request, jwtToken);
        if (result.contains("successful")) {
            logger.info("Class created successfully: {}", request.getClassName());
            redirectAttributes.addFlashAttribute("success", "Class created successfully!");
        } else {
            logger.error("Error creating class: {}", result);
            redirectAttributes.addFlashAttribute("error", result);
        }
        return "redirect:/educator/dashboard";
    }

}