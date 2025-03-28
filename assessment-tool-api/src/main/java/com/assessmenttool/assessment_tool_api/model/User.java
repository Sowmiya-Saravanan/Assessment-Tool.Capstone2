package com.assessmenttool.assessment_tool_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId; // Keycloak's sub

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role; // ENUM('ADMIN', 'EDUCATOR', 'STUDENT')

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "educator", cascade = CascadeType.ALL)
    private List<Assessment> assessments;

    @OneToMany(mappedBy = "educator", cascade = CascadeType.ALL)
    private List<Classes> classes;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Submission> submissions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<OAuthToken> oauthTokens;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}