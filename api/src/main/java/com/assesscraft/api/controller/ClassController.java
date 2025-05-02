package com.assesscraft.api.controller;

import com.assesscraft.api.dto.ClassDTO;
import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.ClassStatus;
import com.assesscraft.api.model.Invitation;
import com.assesscraft.api.security.CustomUserDetails;
import com.assesscraft.api.service.ClassService;
import com.assesscraft.api.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
public class ClassController {

    private static final Logger logger = LoggerFactory.getLogger(ClassController.class);

    @Autowired
    private ClassService classService;

    @Autowired
    private UserService userService;

    @PostMapping(value = "/class/create-class", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createClass(@RequestBody ClassDTO classDTO, Authentication authentication) {
        logger.debug("Received request to create class with data: {}", classDTO);
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Unauthorized request: Authentication or principal is null");
            return new ResponseEntity<>(Map.of("error", "Unauthorized: Authentication required"), HttpStatus.UNAUTHORIZED);
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Class classEntity = new Class();
            classEntity.setClassName(classDTO.getClassName());
            classEntity.setDescription(classDTO.getDescription());
            classEntity.setClassCode(classDTO.getClassCode() != null ? classDTO.getClassCode() : UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            classEntity.setStatus(ClassStatus.valueOf(classDTO.getStatus()));
            classEntity.setCreatedBy(userService.findById(classDTO.getCreatedBy().getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found for ID: " + classDTO.getCreatedBy().getUserId())));
            classEntity.setCreatedAt(classDTO.getCreatedAt() != null ? classDTO.getCreatedAt() : LocalDateTime.now());
            classEntity.setUpdatedAt(classDTO.getUpdatedAt() != null ? classDTO.getUpdatedAt() : LocalDateTime.now());

            Class createdClass = classService.saveClass(classEntity);
            logger.info("Class created successfully with ID: {}", createdClass.getClassId());

            // Construct a Map response to match what the MVC expects
            Map<String, Object> response = new HashMap<>();
            response.put("classId", createdClass.getClassId());
            response.put("className", createdClass.getClassName());
            response.put("classCode", createdClass.getClassCode());
            response.put("status", createdClass.getStatus().toString());
            response.put("createdBy", Map.of("userId", createdClass.getCreatedBy().getUserId()));
            response.put("createdAt", createdClass.getCreatedAt().toString());
            response.put("updatedAt", createdClass.getUpdatedAt().toString());

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Failed to create class: {}", e.getMessage(), e);
            return new ResponseEntity<>(Map.of("error", "Failed to create class: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<Map<String, Object>> getClass(@PathVariable Long classId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("classId", classEntity.getClassId());
        response.put("className", classEntity.getClassName());
        response.put("classCode", classEntity.getClassCode());
        response.put("status", classEntity.getStatus());
        response.put("educator", Map.of("userId", classEntity.getCreatedBy().getUserId()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/classes/draft")
    public ResponseEntity<List<Class>> getDraftedClasses(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<Class> draftedClasses = classService.findByEducatorAndStatus(userDetails.getUser(), ClassStatus.DRAFT);
        return new ResponseEntity<>(draftedClasses, HttpStatus.OK);
    }

    @GetMapping("/classes/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveClasses(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<Class> activeClasses = classService.findByEducatorAndStatus(userDetails.getUser(), ClassStatus.ACTIVE);
        List<Map<String, Object>> response = activeClasses.stream().map(cls -> {
            Map<String, Object> map = new HashMap<>();
            map.put("classId", cls.getClassId());
            map.put("className", cls.getClassName());
            map.put("classCode", cls.getClassCode());
            map.put("status", cls.getStatus());
            map.put("studentCount", cls.getStudents() != null ? cls.getStudents().size() : 0);
            return map;
        }).collect(Collectors.toList());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/class/{classId}/sent-invitations")
    public ResponseEntity<List<Invitation>> getSentInvitations(@PathVariable Long classId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        List<Invitation> sentInvitations = classService.getSentInvitations(classId);
        return new ResponseEntity<>(sentInvitations, HttpStatus.OK);
    }

    @PostMapping("/class/{classId}/approve-students")
    public ResponseEntity<?> approveStudents(@PathVariable Long classId, @RequestBody List<Long> studentIds, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        classService.approveStudents(classId, studentIds);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/class/{classId}/bulk-students")
    public ResponseEntity<Map<String, Object>> bulkAssignStudents(@PathVariable Long classId, @RequestParam("file") MultipartFile file, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Map<String, Object> result = classService.bulkAssignStudents(classId, file);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/class/{classId}/students")
    public ResponseEntity<?> assignStudents(@PathVariable Long classId, @RequestBody List<Long> studentIds, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        classService.assignStudents(classId, studentIds);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/class/{classId}/students")
    public ResponseEntity<List<Map<String, Object>>> getStudents(@PathVariable Long classId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        List<Map<String, Object>> students = classEntity.getStudents().stream().map(student -> {
            Map<String, Object> map = new HashMap<>();
            map.put("studentId", student.getStudentId());
            map.put("email", student.getUser().getEmail());
            map.put("status", student.getUser().getRole().name());
            return map;
        }).collect(Collectors.toList());
        return new ResponseEntity<>(students, HttpStatus.OK);
    }

    @PostMapping("/class/{classId}/add-student")
    public ResponseEntity<Map<String, Object>> addStudent(@PathVariable Long classId, @RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Email is required"), HttpStatus.BAD_REQUEST);
        }

        classService.addStudent(classId, email);
        return new ResponseEntity<>(Map.of("message", "Student added successfully"), HttpStatus.OK);
    }

    @PostMapping("/class/{classId}/remove-student")
    public ResponseEntity<Map<String, Object>> removeStudent(@PathVariable Long classId, @RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Email is required"), HttpStatus.BAD_REQUEST);
        }

        classService.removeStudent(classId, email);
        return new ResponseEntity<>(Map.of("message", "Student removed successfully"), HttpStatus.OK);
    }

    @DeleteMapping("/class/{classId}")
    public ResponseEntity<Map<String, Object>> deleteClass(@PathVariable Long classId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        classService.deleteClass(classId);
        return new ResponseEntity<>(Map.of("message", "Class deleted successfully"), HttpStatus.OK);
    }
}