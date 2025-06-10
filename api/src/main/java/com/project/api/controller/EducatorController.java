package com.project.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.api.config.JwtUtil;

@RestController
@RequestMapping("/api/educator")
public class EducatorController {

    
    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger logger = LoggerFactory.getLogger(EducatorController.class);



    @PostMapping("/validate")
    public ResponseEntity<Map<String, String>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        Map<String, String> response = new HashMap<>();

        try {
            String email = jwtUtil.getEmailFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            if (jwtUtil.validateToken(token, email)) {
                response.put("status", "valid");
                response.put("email", email);
                response.put("role", role);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("status", "invalid");
                response.put("message", "Token is invalid or expired");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "Token validation failed: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}
