package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "submissions")
@Data
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "total_score")
    private Integer totalScore;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL)
    private List<Answer> answers;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL)
    private List<Feedback> feedback;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}