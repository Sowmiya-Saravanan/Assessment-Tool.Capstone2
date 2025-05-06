package com.assesscraft.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submission_answers", indexes = {
    @Index(name = "idx_submission_answer_submission_id", columnList = "submission_id"),
    @Index(name = "idx_submission_answer_question_id", columnList = "question_id")
})
@Getter
@Setter
public class SubmissionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_answer_id")
    private Long submissionAnswerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    @JsonBackReference
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonBackReference
    private Question question;

    @Column(name = "answer_text", length = 1000)
    private String answerText;

    @Column(name = "is_auto_graded", nullable = false)
    private Boolean isAutoGraded = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionAnswerStatus status = SubmissionAnswerStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "submissionAnswer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<AnswerScore> scores = new ArrayList<>();
}