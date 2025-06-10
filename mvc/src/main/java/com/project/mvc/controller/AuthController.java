package com.project.mvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import com.project.mvc.dto.EducatorRegisterRequest;
import com.project.mvc.dto.StudentLoginRequest;
import com.project.mvc.dto.StudentRegisterRequest;
import com.project.mvc.dto.ClassCreateRequest;
import com.project.mvc.dto.EducatorLoginRequest;
import com.project.mvc.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @GetMapping("/educator/register")
    public String educatorRegister(Model model) {
        model.addAttribute("educatorRegisterForm", new EducatorRegisterRequest());
        return "educator-register";
    }

    @PostMapping("/educator/register")
    public String processEducatorRegister(@ModelAttribute("educatorRegisterForm") EducatorRegisterRequest frontendRequest, Model model) {
        String result = authService.registerEducator(frontendRequest);
        if (result.contains("Registration successful")) {
            return "redirect:/educator/login";
        } else {
            model.addAttribute("errorMessage", result);
            return "educator-register";
        }
    }

   @GetMapping("/educator/login")
    public String educatorLogin(Model model, String error, String logout) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        model.addAttribute("educatorLoginRequest", new EducatorLoginRequest());
        return "educator-login";
    }

        @PostMapping("/educator/login")
        public String educatorLoginSubmit(@ModelAttribute("educatorLoginRequest") EducatorLoginRequest request,
                                        HttpServletResponse response,
                                        RedirectAttributes redirectAttributes) {
            logger.info("Processing educator login for email: {}", request.getEmail());
            String result = authService.loginEducator(request);
            if (result.startsWith("Login successful:")) {
                String token = result.split(":")[1];
                Cookie cookie = new Cookie("jwtToken", token);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setMaxAge(5 * 60 * 60); // 5 hours
                cookie.setAttribute("SameSite", "Lax");
                cookie.setDomain("localhost");
                response.addCookie(cookie);
                logger.info("Set JWT cookie for educator: email={}", request.getEmail());
                redirectAttributes.addFlashAttribute("success", "Login successful!");
                return "redirect:/educator/dashboard";
            } else {
                logger.error("Educator login failed: {}", result);
                redirectAttributes.addFlashAttribute("error", result);
                return "redirect:/educator/login";
            }
        }



    // @PostMapping("/educator/logout")
    // public String educatorLogout(HttpServletResponse response) {
    //     // Clear the JWT cookie
    //     Cookie cookie = new Cookie("jwtToken", null);
    //     cookie.setPath("/");
    //     cookie.setHttpOnly(true);
    //     cookie.setMaxAge(0);
    //     cookie.setAttribute("SameSite", "Lax");
    //     cookie.setDomain("localhost"); // Match the domain used when setting the cookie
    //     response.addCookie(cookie);
    //     logger.info("Cleared JWT cookie during logout: name=jwtToken, path=/, HttpOnly=true, SameSite=Lax, MaxAge=0");

    //     // Clear the SecurityContext
    //     SecurityContextHolder.clearContext();
    //     logger.info("Cleared SecurityContext during logout");

    //     return "redirect:/educator/login?logout";
    // }


    @GetMapping("/student/register")
    public String studentRegister(Model model) {
        model.addAttribute("studentRegisterForm", new StudentRegisterRequest());
        return "student-register";
    }

    @PostMapping("/student/register")
    public String processStudentRegister(@ModelAttribute("studentRegisterForm") StudentRegisterRequest frontendRequest, Model model) {
        String result = authService.registerStudent(frontendRequest);
        if (result.contains("Registration successful")) {
            return "redirect:/student/login";
        } else {
            model.addAttribute("errorMessage", result);
            return "student-register";
        }
    }

@GetMapping("/student/login")
    public String studentLogin(Model model, String error, String logout) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        model.addAttribute("studentLoginRequest", new StudentLoginRequest());
        return "student-login"; // Ensure you have a student-login.html template
    }

    @PostMapping("/student/login")
    public String studentLoginSubmit(@ModelAttribute("studentLoginRequest") StudentLoginRequest request,
                                     HttpServletResponse response,
                                     RedirectAttributes redirectAttributes) {
        logger.info("Processing student login for email: {}", request.getEmail());
        String result = authService.loginStudent(request);
        if (result.startsWith("Login successful:")) {
            String token = result.split(":")[1];
            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(5 * 60 * 60); // 5 hours
            cookie.setAttribute("SameSite", "Lax");
            cookie.setDomain("localhost");
            response.addCookie(cookie);
            logger.info("Set JWT cookie for student: email={}", request.getEmail());
            redirectAttributes.addFlashAttribute("success", "Login successful!");
            return "redirect:/student/dashboard";
        } else {
            logger.error("Student login failed: {}", result);
            redirectAttributes.addFlashAttribute("error", result);
            return "redirect:/student/login";
        }
    }



}