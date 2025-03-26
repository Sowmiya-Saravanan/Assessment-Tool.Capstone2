package com.assessmenttool.assessment_tool_api.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.assessmenttool.assessment_tool_api.model.Answer;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findBySubmissionSubmissionId(Long submissionId);
}