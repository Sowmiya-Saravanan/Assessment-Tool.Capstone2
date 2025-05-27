package com.project.mvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String educatorLogin(Model model, @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            model.addAttribute("errorMessage", "Incorrect email or password");
        }
        model.addAttribute("educatorLoginForm", new EducatorLoginRequest());
        return "educator-login";
    }

    @PostMapping("/educator/login")
    public String processEducatorLogin(@ModelAttribute("educatorLoginForm") EducatorLoginRequest frontendRequest, Model model, HttpServletResponse response) {
        String result = authService.loginEducator(frontendRequest);
        if (result.startsWith("Login successful")) {
            String token = result.split(":")[1];
            logger.info("Setting JWT token in cookie: {}", token);
            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(5 * 60 * 60);
            cookie.setAttribute("SameSite", "Lax");
            cookie.setDomain("localhost"); // Explicitly set domain
            response.addCookie(cookie);
            logger.info("Cookie set with attributes: name=jwtToken, path=/, HttpOnly=true, SameSite=Lax, MaxAge={}", cookie.getMaxAge());
            logger.info("Redirecting to /educator/dashboard after setting cookie");
            return "redirect:/educator/dashboard";
        } else {
            model.addAttribute("errorMessage", result);
            return "educator-login";
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
    public String studentLogin(Model model, @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            model.addAttribute("errorMessage", "Incorrect email or password");
        }
        model.addAttribute("studentLoginForm", new StudentLoginRequest());
        return "student-login";
    }

    @PostMapping("/student/login")
    public String processStudentLogin(@ModelAttribute("studentLoginForm") StudentLoginRequest frontendRequest, Model model, HttpServletResponse response) {
        String result = authService.loginStudent(frontendRequest);
        if (result.startsWith("Login successful")) {
            String token = result.split(":")[1];
            logger.info("Setting JWT token in cookie: {}", token);
            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(5 * 60 * 60);
            cookie.setAttribute("SameSite", "Lax");
            cookie.setDomain("localhost"); // Explicitly set domain
            response.addCookie(cookie);
            logger.info("Cookie set with attributes: name=jwtToken, path=/, HttpOnly=true, SameSite=Lax, MaxAge={}", cookie.getMaxAge());
            logger.info("Redirecting to /student/dashboard after setting cookie");
            return "redirect:/student/dashboard";
        } else {
            model.addAttribute("errorMessage", result);
            return "student-login";
        }
    }



}