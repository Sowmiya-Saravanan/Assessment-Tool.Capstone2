package com.assesscraft.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission_answers")
@Getter
@Setter
public class SubmissionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long submissionAnswerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "JSON", nullable = false)
    private String answer;

    private Boolean isAutoGraded = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionAnswerStatus status = SubmissionAnswerStatus.PENDING;

    private Double score;
    private String feedback;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}