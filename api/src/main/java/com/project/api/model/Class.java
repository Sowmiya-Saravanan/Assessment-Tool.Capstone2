package com.project.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classes", indexes = {
    @Index(name = "idx_class_created_by", columnList = "created_by"),
    @Index(name = "idx_class_code", columnList = "class_code")
})
@Data
public class Class {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "class_code", nullable = false, unique = true)
    private String classCode;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonBackReference("user-classes")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "classes")
    @JsonBackReference("assessment-classes")
    private List<Assessment> assessments = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "class_students",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @JsonManagedReference
    private List<Student> students = new ArrayList<>();

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Invitation> invitations = new ArrayList<>();
}