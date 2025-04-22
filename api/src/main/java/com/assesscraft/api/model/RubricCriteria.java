package com.assesscraft.api.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rubric_criteria")
@Getter
@Setter
public class RubricCriteria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long criterionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double points;

    private String description;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
