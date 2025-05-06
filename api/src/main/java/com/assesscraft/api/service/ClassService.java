package com.assesscraft.api.service;

import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.ClassStatus;
import com.assesscraft.api.model.Invitation;
import com.assesscraft.api.model.User;
import com.assesscraft.api.repository.ClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClassService {

    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);

    @Autowired
    private ClassRepository classRepository;

    @Transactional
    public Class saveClass(Class classEntity) {
        return classRepository.save(classEntity);
    }

    @Transactional(readOnly = true)
    public List<Class> findByEducatorAndStatus(User createdBy, ClassStatus status) {
        return classRepository.findByCreatedByAndStatus(createdBy, status);
    }

    @Transactional(readOnly = true)
    public Optional<Class> findById(Long id) {
        return classRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Invitation> getSentInvitations(Long classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        return classEntity.getInvitations();
    }

    @Transactional
    public void approveStudents(Long classId, List<Long> studentIds) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        // Implement student approval logic
    }

    @Transactional
    public Map<String, Object> bulkAssignStudents(Long classId, MultipartFile file) {
        // Implement bulk assignment logic
        return Map.of("status", "success");
    }

    @Transactional
    public void assignStudents(Long classId, List<Long> studentIds) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        // Implement student assignment logic
    }

    @Transactional
    public void addStudent(Long classId, String email) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        // Implement add student logic
    }

    @Transactional
    public void removeStudent(Long classId, String email) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        // Implement remove student logic
    }

    @Transactional
    public void deleteClass(Long classId) {
        classRepository.deleteById(classId);
    }
}