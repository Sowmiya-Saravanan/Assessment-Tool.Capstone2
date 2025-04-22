package com.assesscraft.api.repository;

import com.assesscraft.api.model.Invitation;
import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    /**
     * Checks if an invitation exists for the given email, class, and status.
     *
     * @param email The email of the invited user.
     * @param classEntity The class associated with the invitation.
     * @param status The invitation status to match.
     * @return true if an invitation exists, false otherwise.
     */
    boolean existsByEmailAndClassEntityAndStatus(String email, Class classEntity, InvitationStatus status);

    /**
     * Finds an invitation by email and class for additional flexibility.
     *
     * @param email The email of the invited user.
     * @param classEntity The class associated with the invitation.
     * @return An Optional containing the invitation if found, empty otherwise.
     */
    Optional<Invitation> findByEmailAndClassEntity(String email, Class classEntity);

    /**
     * Finds all invitations for a given class and status.
     *
     * @param classEntity The class associated with the invitations.
     * @param status The invitation status to match.
     * @return A list of matching invitations.
     */
    List<Invitation> findAllByClassEntityAndStatus(Class classEntity, InvitationStatus status);
}