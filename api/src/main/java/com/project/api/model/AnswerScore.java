package com.project.api.model;




import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "answer_scores", indexes = {
    @Index(name = "idx_answer_score_submission_answer_id", columnList = "submission_answer_id")
})
@Data
public class AnswerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_score_id")
    private Long answerScoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_answer_id", nullable = false)
    @JsonBackReference
    private SubmissionAnswer submissionAnswer;

    @Column(nullable = false)
    private Double score;

    @Column(name = "comments", length = 1000)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}