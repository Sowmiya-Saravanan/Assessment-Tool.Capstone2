package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "answer_scores")
@Data
public class AnswerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @ManyToOne
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @ManyToOne
    @JoinColumn(name = "criterion_id", nullable = false)
    private RubricCriteria criterion;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "is_auto_graded", nullable = false)
    private Boolean isAutoGraded;
}