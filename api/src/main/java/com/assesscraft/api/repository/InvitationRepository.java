package com.assesscraft.api.repository;

import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.Invitation;
import com.assesscraft.api.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    // Find invitations by class and status
    List<Invitation> findAllByClassEntityAndStatus(Class classEntity, InvitationStatus status);

    // Find an invitation by email and class
    Optional<Invitation> findByRecipientEmailAndClassEntity(String recipientEmail, Class classEntity);

    // Find invitations by a list of IDs
    List<Invitation> findAllById(Iterable<Long> ids);
}