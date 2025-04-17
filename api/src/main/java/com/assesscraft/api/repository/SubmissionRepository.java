package com.assesscraft.api.repository;
import com.assesscraft.api.model.Assessment;
import com.assesscraft.api.model.Submission;
import com.assesscraft.api.model.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    long countByAssessmentIn(List<Assessment> assessments);
    long countByAssessmentInAndStatusNotIn(List<Assessment> assessments, List<SubmissionStatus> statuses);
}