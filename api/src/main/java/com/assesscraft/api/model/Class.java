package com.assesscraft.api.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classes")
@Getter
@Setter
public class Class {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long classId;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educator_id", nullable = false)
    private User educator;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "class", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Assessment> assessments = new ArrayList<>();

    @OneToMany(mappedBy = "class", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Invitation> invitations = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "class_students",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private List<User> students = new ArrayList<>();
}
