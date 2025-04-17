package com.assesscraft.mvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/student")
public class StudentController {

    @GetMapping("/register")
    public String showRegistrationForm(@RequestParam String email, @RequestParam String code, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("code", code);
        return "student-register"; // Create this Thymeleaf template next
    }
}