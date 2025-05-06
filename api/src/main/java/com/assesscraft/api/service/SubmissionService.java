package com.assesscraft.api.service;

import com.assesscraft.api.model.Assessment;
import com.assesscraft.api.model.Submission;
import com.assesscraft.api.model.SubmissionStatus;
import com.assesscraft.api.model.User;
import com.assesscraft.api.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Transactional
    public Submission saveSubmission(Submission submission) {
        logger.debug("Saving submission with ID: {}", submission.getSubmissionId());
        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public Optional<Submission> findById(Long id) {
        logger.debug("Fetching submission with ID: {}", id);
        return submissionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public long countByAssessmentIn(List<Assessment> assessments) {
        logger.debug("Counting submissions for {} assessments", assessments.size());
        return submissionRepository.countByAssessmentIn(assessments);
    }

    @Transactional(readOnly = true)
    public long countByAssessmentInAndStatusNotIn(List<Assessment> assessments, List<SubmissionStatus> statuses) {
        logger.debug("Counting submissions for {} assessments with status not in {}", assessments.size(), statuses);
        return submissionRepository.countByAssessmentInAndStatusNotIn(assessments, statuses);
    }

    @Transactional(readOnly = true)
    public long countByAssessmentCreatedBy(User user) {
        logger.debug("Counting submissions for assessments created by user ID: {}", user.getUserId());
        return submissionRepository.countByAssessment_CreatedBy(user);
    }

    @Transactional(readOnly = true)
    public long countByAssessmentCreatedByAndStatusNotIn(User user, List<SubmissionStatus> statuses) {
        logger.debug("Counting submissions for assessments created by user ID: {} with status not in {}", user.getUserId(), statuses);
        return submissionRepository.countByAssessment_CreatedByAndStatusNotIn(user, statuses);
    }
}