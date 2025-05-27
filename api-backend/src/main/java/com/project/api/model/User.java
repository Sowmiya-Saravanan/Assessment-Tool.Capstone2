package com.project.api.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="users",indexes = {
@Index(name = "idx_user_email",columnList = "email")
})
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    
    @Column(nullable = false)
    private String name;
    
     @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

     @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Assessment> createdAssessments = new ArrayList<>();

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Class> createdClasses = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Student student;


    
}
