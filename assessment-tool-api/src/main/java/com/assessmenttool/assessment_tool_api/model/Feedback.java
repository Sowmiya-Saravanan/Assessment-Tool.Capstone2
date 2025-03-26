package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "feedback")
@Data
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @ManyToOne
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "comment", nullable = false)
    private String comment;
}