package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "options")
@Data
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long optionId;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;
}