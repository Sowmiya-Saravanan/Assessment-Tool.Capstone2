package com.assessmenttool.assessment_tool_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.assessmenttool.assessment_tool_api.model.Assessment;

public interface AssessmentRepository extends JpaRepository<Assessment,Long>{
    List<Assessment> findByEducatorExternalId(String externalId);
}
