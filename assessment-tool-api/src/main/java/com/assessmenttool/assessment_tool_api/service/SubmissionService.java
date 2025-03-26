package com.assessmenttool.assessment_tool_api.service;

import com.assessmenttool.assessment_tool_api.model.Submission;
import com.assessmenttool.assessment_tool_api.model.User;
import com.assessmenttool.assessment_tool_api.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    public List<Submission> findAll() {
        return submissionRepository.findAll();
    }

    public Submission submitAssessment(Submission submission, User student) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'submitAssessment'");
    }
}