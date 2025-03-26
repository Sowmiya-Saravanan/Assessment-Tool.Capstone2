package com.assessmenttool.assessment_tool_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.assessmenttool.assessment_tool_api.model.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStudentExternalId(String externalId);
    List<Submission> findByAssessmentAssessmentId(Long assessmentId);
}