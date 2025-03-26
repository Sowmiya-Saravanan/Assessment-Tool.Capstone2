package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "analytics")
@Data
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analytic_id")
    private Long analyticId;

    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private Classes classId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType; // ENUM('AVERAGE_SCORE', 'QUESTION_DIFFICULTY', 'COMPLETION_RATE')

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}

