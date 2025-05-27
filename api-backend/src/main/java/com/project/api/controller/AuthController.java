package com.project.api.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.api.config.JwtUtil;
import com.project.api.dto.EducatorLoginRequest;
import com.project.api.dto.EducatorRegisterRequest;
import com.project.api.dto.StudentLoginRequest;
import com.project.api.dto.StudentRegisterRequest;
import com.project.api.exception.UserAlreadyExistsException;
import com.project.api.model.User;
import com.project.api.service.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

        @PostMapping("/educator/register")
    public ResponseEntity<String> registerEducator(@RequestBody EducatorRegisterRequest registerRequest){
        logger.info("Educator registration attempt for username: {}", registerRequest.getName());

        try {
            userService.registerEducator(registerRequest);
            logger.info("Educator registration successful for username: {}", registerRequest.getName());
            return new ResponseEntity<>("Educator registration successful", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("Educator registration failed for username: {}. Reason: {}", registerRequest.getName(), e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (UserAlreadyExistsException e) {
            logger.error("Educator registration failed for username: {}. Reason: {}", registerRequest.getName(), e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Unexpected error during educator registration for username: {}", registerRequest.getName(), e);
            return new ResponseEntity<>("Educator registration failed due to an unexpected error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/educator/login")
    public ResponseEntity<Map<String, String>> loginEducator(@RequestBody EducatorLoginRequest educatorLoginRequest) {
        logger.info("Educator login attempt for email: {}", educatorLoginRequest.getEmail());

        Optional<User> user = userService.loginEducator(educatorLoginRequest);

        Map<String, String> response = new HashMap<>();
        
        
        if (user.isPresent()) {
            String token = jwtUtil.generateToken(user.get().getEmail(), user.get().getRole().name());
            logger.info("Educator login successful for email: {}", educatorLoginRequest.getEmail());
            response.put("status","success");
            response.put("token", token);
            response.put("role",user.get().getRole().name());
            logger.info("Educator login successful for email: {}", educatorLoginRequest.getEmail());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("status", "error");
            response.put("message", "Invalid email or password");
            logger.error("Educator login failed for email: {}", educatorLoginRequest.getEmail());
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }



    @PostMapping("/student/register")
    public ResponseEntity<String> registerStudent(@RequestBody StudentRegisterRequest registerRequest){
        logger.info("Student registration attempt for username: {}", registerRequest.getName());

        try {
            userService.registerStudent(registerRequest);
            logger.info("Student registration successful for username: {}", registerRequest.getName());
            return new ResponseEntity<>("Student registration successful", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("Student registration failed for username: {}. Reason: {}", registerRequest.getName(), e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (UserAlreadyExistsException e) {
            logger.error("Student registration failed for username: {}. Reason: {}", registerRequest.getName(), e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Unexpected error during student registration for username: {}", registerRequest.getName(), e);
            return new ResponseEntity<>("Student registration failed due to an unexpected error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/student/login")
    public ResponseEntity<Map<String, String>> loginStudent(@RequestBody StudentLoginRequest studentLoginRequest) {
        logger.info("Student login attempt for email: {}", studentLoginRequest.getEmail());

        Optional<User> user = userService.loginStudent(studentLoginRequest);

        Map<String, String> response = new HashMap<>();
        
        
        if (user.isPresent()) {
            String token = jwtUtil.generateToken(user.get().getEmail(), user.get().getRole().name());
            logger.info("Student login successful for email: {}", studentLoginRequest.getEmail());
            response.put("status","success");
            response.put("token", token);
            response.put("role",user.get().getRole().name());
            logger.info("Student login successful for email: {}", studentLoginRequest.getEmail());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("status", "error");
            response.put("message", "Invalid email or password");
            logger.error("Student login failed for email: {}", studentLoginRequest.getEmail());
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }


}

