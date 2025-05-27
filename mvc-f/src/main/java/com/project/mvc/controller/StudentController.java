package com.project.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.project.mvc.dto.ClassCreateRequest;

@Controller
@RequestMapping("/student")
public class StudentController {


   private static final Logger logger = LoggerFactory.getLogger(StudentController.class);


        @GetMapping("/dashboard")
        public String studentDashboard(Model model) {
            logger.info("Rendering student dashboard");
            model.addAttribute("welcomeMessage", "Welcome to the Student Dashboard!");
            return "student-dashboard";
        }


}
