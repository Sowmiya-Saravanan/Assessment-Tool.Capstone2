package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "auto_grade_criteria")
@Data
public class AutoGradeCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auto_criterion_id")
    private Long autoCriterionId;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "criterion_name", nullable = false)
    private String criterionName;

    @Column(name = "words", nullable = false)
    private String words; // Keywords for NLP

    @Column(name = "points", nullable = false)
    private Integer points;
}