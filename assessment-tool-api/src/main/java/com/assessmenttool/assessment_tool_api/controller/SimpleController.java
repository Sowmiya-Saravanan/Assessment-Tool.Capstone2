package com.assessmenttool.assessment_tool_api.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SimpleController {

    @GetMapping("/message")
    public String getMessage() {
        return "Hello from the Assessment Tool API!";
    }
}