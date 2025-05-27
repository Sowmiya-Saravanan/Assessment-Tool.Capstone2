package com.project.api.controller;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.api.model.Class;
import com.project.api.config.JwtUtil;
import com.project.api.dto.ClassCreateRequest;
import com.project.api.service.ClassService;

@RestController
@RequestMapping("api/educator")
public class ClassController {

    private static final Logger logger = LoggerFactory.getLogger(ClassController.class);
    @Autowired
    private ClassService classService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/create/class")
    public ResponseEntity<Map<String,String>> createClass(@RequestBody ClassCreateRequest classRequest
    , @RequestHeader("Authorization") String authHeader) {

        logger.info("Received request to create class: {}", classRequest.getClassName());
        logger.debug("Authorization header: {}", authHeader);
    
        Map<String,String> response = new HashMap<>();
        try{

            if(authHeader == null || !authHeader.startsWith("Bearer ")){

                response.put("status","error");
                response.put("message","Authorization header missing or invalid");
                logger.error("Authorization header missing or invalid");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);

            }
       
            String token = authHeader.substring(7);
            String educatorEmail = jwtUtil.getEmailFromToken(token);
            if(!jwtUtil.validateToken(token, educatorEmail)){

                response.put("status","error");
                response.put("message","Invalid token");
                logger.error("Invalid token");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);

            }

            Class createdClass = classService.createClass(classRequest, educatorEmail);
            response.put("status", "success");
            response.put("message", "Class created successfully");
            response.put("classId", createdClass.getClassId().toString());
            logger.info("Class created successfully with ID: {}  and educator: {}", createdClass.getClassId(), educatorEmail);
            return new ResponseEntity<>(response,HttpStatus.OK);


      } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            logger.error("Validation error while creating class: {}", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while creating class: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to create class due to an unexpected error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
