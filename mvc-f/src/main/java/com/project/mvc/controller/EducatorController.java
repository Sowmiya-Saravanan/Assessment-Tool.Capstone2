package com.project.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.project.mvc.dto.ClassCreateRequest;

@Controller
@RequestMapping("/educator")
public class EducatorController {


    private static final Logger logger = LoggerFactory.getLogger(EducatorController.class);


        @GetMapping("/dashboard")
        public String educatorDashboard(Model model) {
            logger.info("Rendering educator dashboard");
            model.addAttribute("classCreateRequest", new ClassCreateRequest());
            return "educator-dashboard";
        }

        //For debugging purpose
        @GetMapping("/debug-dashboard")
            public String debugDashboard(Model model) {
                model.addAttribute("classCreateRequest", new ClassCreateRequest());
                return "educator-dashboard";
        }
}
