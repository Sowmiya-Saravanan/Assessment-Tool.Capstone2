package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_assignments")
@Data
public class AssessmentAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false)
    private RecipientType recipientType; // ENUM('CLASS', 'INDIVIDUAL')

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId; // Class ID or Student ID

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;
}

enum RecipientType {
    CLASS, INDIVIDUAL
}