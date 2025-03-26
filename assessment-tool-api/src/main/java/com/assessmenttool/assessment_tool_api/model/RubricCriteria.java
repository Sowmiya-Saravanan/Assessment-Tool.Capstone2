package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "rubric_criteria")
@Data
public class RubricCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criterion_id")
    private Long criterionId;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "points", nullable = false)
    private Integer points;

    @OneToMany(mappedBy = "criterion", cascade = CascadeType.ALL)
    private List<AnswerScore> answerScores;
}