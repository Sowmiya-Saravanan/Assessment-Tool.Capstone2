package com.assesscraft.api.repository;
import com.assesscraft.api.model.Assessment;
import com.assesscraft.api.model.Class;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    long countByClassEntityIn(List<Class> classes);
    List<Assessment> findByClassEntityIn(List<Class> classes);
    List<Assessment> findByClassEntityInAndStartTimeAfter(List<Class> classes, LocalDateTime startTime);
}