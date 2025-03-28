package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "assessments")
@Data
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_id")
    private Long assessmentId;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AssessmentType type; // ENUM('QUIZ', 'TEST', 'EXAM', 'SURVEY')

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_type")
    private DurationType durationType; // ENUM('TOTAL', 'PER_QUESTION')

    private Integer duration;

    private String instructions;

    @ManyToOne
    @JoinColumn(name = "educator_id", nullable = false)
    private User educator;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category; // Added previously

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AssessmentStatus status; // ENUM('DRAFT', 'ASSIGNED', 'ONGOING', 'FINISHED', 'CANCELED')

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL)
    private List<Question> questions;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL)
    private List<AssessmentAssignment> assessmentAssignments;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL)
    private List<Submission> submissions;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL)
    private List<Analytics> analytics;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = AssessmentStatus.DRAFT;
        }
    }
}
