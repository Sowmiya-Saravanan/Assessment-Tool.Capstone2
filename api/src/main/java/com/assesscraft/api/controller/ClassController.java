package com.assesscraft.api.controller;

import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.ClassStatus;
import com.assesscraft.api.model.User;
import com.assesscraft.api.security.CustomUserDetails;
import com.assesscraft.api.service.ClassService;
import com.assesscraft.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/data")
public class ClassController {

    @Autowired
    private ClassService classService;

    @Autowired
    private UserService userService;

    @PostMapping("/class")
    public ResponseEntity<Class> createClass(@RequestBody Class classData, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        classData.setEducator(userDetails.getUser());
        classData.setClassCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        classData.setStatus(ClassStatus.DRAFT);
        classData.setCreatedAt(java.time.LocalDateTime.now());
        classData.setUpdatedAt(java.time.LocalDateTime.now());

        Class createdClass = classService.saveClass(classData);
        return new ResponseEntity<>(createdClass, HttpStatus.CREATED);
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

    @GetMapping("/class/{classId}/pending-students")
    public ResponseEntity<List<User>> getPendingStudents(@PathVariable Long classId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getEducator().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        List<User> pendingStudents = classService.getPendingStudents(classId);
        return new ResponseEntity<>(pendingStudents, HttpStatus.OK);
    }

    @PostMapping("/class/{classId}/approve-students")
    public ResponseEntity<?> approveStudents(@PathVariable Long classId, @RequestBody List<Long> studentIds, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getEducator().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        classService.approveStudents(classId, studentIds);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/class/{classId}/bulk-students")
    public ResponseEntity<?> bulkAssignStudents(@PathVariable Long classId, @RequestParam("file") MultipartFile file, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getEducator().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        classService.bulkAssignStudents(classId, file);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/class/{classId}/students")
    public ResponseEntity<?> assignStudents(@PathVariable Long classId, @RequestBody List<Long> studentIds, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Class classEntity = classService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        if (!classEntity.getEducator().getUserId().equals(userDetails.getUser().getUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        classService.assignStudents(classId, studentIds);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}