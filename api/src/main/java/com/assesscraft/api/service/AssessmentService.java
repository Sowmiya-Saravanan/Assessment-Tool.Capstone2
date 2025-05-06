package com.assesscraft.api.service;

import com.assesscraft.api.model.Assessment;
import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.User;
import com.assesscraft.api.repository.AssessmentRepository;
import com.assesscraft.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Assessment saveAssessment(Assessment assessment) {
        return assessmentRepository.save(assessment);
    }

    @Transactional(readOnly = true)
    public List<Assessment> findByCreatedBy(Long createdById) {
        logger.debug("Fetching assessments for user ID: {}", createdById);
        User user = userRepository.findById(createdById)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + createdById));
        return assessmentRepository.findByCreatedBy(user);
    }

    @Transactional(readOnly = true)
    public Optional<Assessment> findById(Long id) {
        return assessmentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Assessment> findByClasses(List<Class> classes) {
        return assessmentRepository.findByClassesIn(classes);
    }
}