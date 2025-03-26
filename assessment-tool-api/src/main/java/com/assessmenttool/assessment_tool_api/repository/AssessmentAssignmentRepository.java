package com.assessmenttool.assessment_tool_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.assessmenttool.assessment_tool_api.model.AssessmentAssignment;

public interface AssessmentAssignmentRepository extends JpaRepository<AssessmentAssignment, Long> {
    List<AssessmentAssignment> findByAssessmentAssessmentId(Long assessmentId);
}