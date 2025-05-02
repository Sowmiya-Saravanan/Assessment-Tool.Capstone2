package com.assesscraft.api.repository;

import com.assesscraft.api.model.Assessment;
import com.assesscraft.api.model.Submission;
import com.assesscraft.api.model.SubmissionStatus;
import com.assesscraft.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    // Count submissions for assessments created by the given user
    long countByAssessment_CreatedBy(User user);

    // Count submissions for assessments created by the given user where status is not in the list
    long countByAssessment_CreatedByAndStatusNotIn(User user, List<SubmissionStatus> statuses);

    // Count submissions for a list of assessments
    long countByAssessmentIn(List<Assessment> assessments);

    // Count submissions for a list of assessments where status is not in the list
    long countByAssessmentInAndStatusNotIn(List<Assessment> assessments, List<SubmissionStatus> statuses);
}