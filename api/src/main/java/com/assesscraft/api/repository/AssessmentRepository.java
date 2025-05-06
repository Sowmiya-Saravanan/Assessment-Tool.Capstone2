package com.assesscraft.api.repository;

import com.assesscraft.api.model.Assessment;
import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    // Find assessments by creator (User object)
    List<Assessment> findByCreatedBy(User user);

    // Find an assessment by ID (already provided by JpaRepository, but kept for clarity)
    Optional<Assessment> findById(Long id);

    // Count assessments by creator
    long countByCreatedBy(User user);

    // Count assessments by creator with start time after a specific time
    long countByCreatedByAndStartTimeAfter(User user, LocalDateTime startTime);

    // Find assessments by a list of classes (kept for potential use elsewhere)
    List<Assessment> findByClassesIn(List<Class> classes);

    // Count assessments associated with a list of classes (kept for potential use elsewhere)
    long countByClassesIn(List<Class> classes);

    // Count assessments associated with a list of classes and starting after a specific time (kept for potential use elsewhere)
    long countByClassesInAndStartTimeAfter(List<Class> classes, LocalDateTime startTime);
}