package com.project.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitations", indexes = {
    @Index(name = "idx_invitation_class_id", columnList = "class_id"),
    @Index(name = "idx_invitation_created_by", columnList = "created_by")
})
@Getter
@Setter
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    @JsonBackReference
    private Class classEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "temporary_password")
    private String temporaryPassword;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    // Utility method to set the expiration date
    public void setExpirationDateWithDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Expiration days must be positive");
        }
        this.expirationDate = this.createdAt != null 
            ? this.createdAt.plusDays(days)
            : LocalDateTime.now().plusDays(days);
    }
}
