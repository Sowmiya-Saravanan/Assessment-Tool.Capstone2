package com.project.api.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.api.config.JwtUtil;
import com.project.api.model.Class;
import com.project.api.service.ClassService;



@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private ClassService classService;

    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @GetMapping("/classes")
    public ResponseEntity<Map<String, Object>> getStudentClasses(@RequestHeader("Authorization") String authHeader){
        logger.info("Received request to fetch classes for student");

        Map<String, Object> response = new HashMap<>();
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("status", "error");
                response.put("message", "Authorization header missing or invalid");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            String email = jwtUtil.getEmailFromToken(token);
            if (!jwtUtil.validateToken(token, email)) {
                response.put("status", "error");
                response.put("message", "Invalid token");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            List<Class> classes = classService.getStudentClasses(email);
            response.put("status", "success");
            response.put("classes", classes);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching classes: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to fetch classes due to an unexpected error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/join/class")
public ResponseEntity<Map<String, Object>> joinClass(@RequestHeader("Authorization") String authHeader,
                                                     @RequestBody Map<String, String> requestBody){
logger.info("Received request to join class with code: {}", requestBody.get("classCode"));
    Map<String, Object> response = new HashMap<>();
    try {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("status", "error");
            response.put("message", "Authorization header missing or invalid");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        String email = jwtUtil.getEmailFromToken(token);
        if (!jwtUtil.validateToken(token, email)) {
            response.put("status", "error");
            response.put("message", "Invalid token");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        String classCode = requestBody.get("classCode");
        if (classCode == null || classCode.trim().isEmpty()) {
            response.put("status", "error");
            response.put("message", "Class code is required");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Class joinedClass = classService.joinClass(classCode, email);
        response.put("status", "success");
        response.put("message", "Successfully joined class: " + joinedClass.getClassName());
        return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
        response.put("status", "error");
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
        logger.error("Unexpected error while joining class: {}", e.getMessage(), e);
        response.put("status", "error");
        response.put("message", "Failed to join class due to an unexpected error");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


    //fetching class details for a student
    @GetMapping("/class-details")
public ResponseEntity<Map<String, Object>> getClassDetails(
        @RequestParam("classId") Long classId,
        @RequestHeader("Authorization") String authHeader) {
    logger.info("Received request to fetch details for class ID: {} by student", classId);
    Map<String, Object> response = new HashMap<>();
    try {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("status", "error");
            response.put("message", "Authorization header missing or invalid");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        String token = authHeader.substring(7);
        String email = jwtUtil.getEmailFromToken(token);
        if (!jwtUtil.validateToken(token, email)) {
            response.put("status", "error");
            response.put("message", "Invalid token");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        Map<String, Object> classDetails = classService.getClassDetailsForStudent(classId, email);
        response.put("status", "success");
        response.put("classDetails", classDetails);
        logger.info("Class details fetched successfully for class ID: {} by student: {}", classId, email);
        return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
        response.put("status", "error");
        response.put("message", e.getMessage());
        logger.error("Validation error: {}", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        response.put("status", "error");
        response.put("message", "Failed to fetch class details");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
}