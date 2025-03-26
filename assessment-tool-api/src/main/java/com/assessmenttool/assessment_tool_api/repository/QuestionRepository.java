package com.assessmenttool.assessment_tool_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.assessmenttool.assessment_tool_api.model.Question;

public interface QuestionRepository extends JpaRepository<Question,Long>{
    List<Question> findByAssessmentAssessmentId(Long assessmentId);

}
