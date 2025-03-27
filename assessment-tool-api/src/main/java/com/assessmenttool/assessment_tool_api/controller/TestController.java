package com.assessmenttool.assessment_tool_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/public/test")
    public String publicEndpoint() {
        return "Public endpoint - No authentication required";
    }

    @GetMapping("/admin/test")
    public String adminEndpoint() {
        return "Admin endpoint - Requires ROLE_ADMIN";
    }

    @GetMapping("/educator/test")
    public String educatorEndpoint() {
        return "Educator endpoint - Requires ROLE_EDUCATOR";
    }

    @GetMapping("/student/test")
    public String studentEndpoint() {
        return "Student endpoint - Requires ROLE_STUDENT";
    }
}