package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "classes")
@Data
public class Classes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long classId;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "educator_id", nullable = false)
    private User educator;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(
        name = "class_students",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private List<User> students;

    @OneToMany(mappedBy = "recipientId", cascade = CascadeType.ALL)
    private List<AssessmentAssignment> assessmentAssignments;

    @OneToMany(mappedBy = "classId", cascade = CascadeType.ALL)
    private List<Analytics> analytics;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}