package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "questions")
@Data
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(name = "text", nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private QuestionType type; // ENUM('MCQ', 'TF', 'SA', 'ESSAY')

    private Integer points;

    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_method")
    private GradingMethod gradingMethod; // ENUM('MANUAL', 'AUTO')

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<Option> options;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<RubricCriteria> rubricCriteria;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<AutoGradeCriteria> autoGradeCriteria;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<Answer> answers;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<Feedback> feedback;
}

enum QuestionType {
    MCQ, TF, SA, ESSAY
}

enum GradingMethod {
    MANUAL, AUTO
}