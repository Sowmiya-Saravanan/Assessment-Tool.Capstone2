package com.project.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.project.api.model.Class;
import com.project.api.config.JwtUtil;
import com.project.api.dto.ClassCreateRequest;
import com.project.api.service.AssessmentService;
import com.project.api.service.ClassService;

@RestController
@RequestMapping("api/educator")
public class ClassController {

    private static final Logger logger = LoggerFactory.getLogger(ClassController.class);
    @Autowired
    private ClassService classService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AssessmentService assessmentService;

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


    @GetMapping("/classes")
    public ResponseEntity<Map<String, Object>> getEducatorClasses(@RequestHeader("Authorization") String authHeader) {
        logger.info("Received request to fetch classes for educator");

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

            List<Class> classes = classService.getEducatorClasses(email);
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

    @GetMapping("/class-details")
    public ResponseEntity<Map<String, Object>> getClassDetails(
            @RequestParam("classId") Long classId,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("Received request to fetch details for class ID: {}", classId);
        logger.debug("Authorization header: {}", authHeader);

        Map<String, Object> response = new HashMap<>();

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("status", "error");
                response.put("message", "Authorization header missing or invalid");
                logger.error("Authorization header missing or invalid");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String token = authHeader.substring(7);
            String educatorEmail = jwtUtil.getEmailFromToken(token);
            if (!jwtUtil.validateToken(token, educatorEmail)) {
                response.put("status", "error");
                response.put("message", "Invalid token");
                logger.error("Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Map<String, Object> classDetails = classService.getClassDetails(classId, educatorEmail);
            response.put("status", "success");
            response.put("classDetails", classDetails);
            logger.info("Class details fetched successfully for class ID: {} by educator: {}", classId, educatorEmail);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            logger.error("Validation error while fetching class details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while fetching class details: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to fetch class details due to an unexpected error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
