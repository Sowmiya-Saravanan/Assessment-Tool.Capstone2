package com.assesscraft.api.controller;

import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.Invitation;
import com.assesscraft.api.model.InvitationStatus;
import com.assesscraft.api.repository.ClassRepository;
import com.assesscraft.api.repository.InvitationRepository;
import com.assesscraft.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class InviteController {

    private final JavaMailSender mailSender;
    private final ClassRepository classRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;

    public InviteController(JavaMailSender mailSender, ClassRepository classRepository,
                           InvitationRepository invitationRepository, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.classRepository = classRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
    }

    public static class InviteRequest {
        private Long classId;
        private String[] emails;

        public Long getClassId() { return classId; }
        public void setClassId(Long classId) { this.classId = classId; }
        public String[] getEmails() { return emails; }
        public void setEmails(String[] emails) { this.emails = emails; }
    }

    private static final Logger logger = LoggerFactory.getLogger(InviteController.class);

    @PostMapping("/invitations")
    public ResponseEntity<?> sendInvites(@RequestBody InviteRequest request) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Class classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Class not found"));
        if (!classEntity.getCreatedBy().getUserId().toString().equals(userId) &&
                !classEntity.getCreatedBy().getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).body("Not authorized to invite for this class");
        }

        Map<String, String> failures = new HashMap<>();
        for (String email : request.getEmails()) {
            Invitation invite = new Invitation();
            invite.setClassEntity(classEntity);
            invite.setRecipientEmail(email);
            invite.setStatus(InvitationStatus.PENDING);
            invite.setCreatedBy(classEntity.getCreatedBy());
            invite.setCreatedAt(LocalDateTime.now());
            invitationRepository.save(invite);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Join Class: " + classEntity.getClassName());
            String joinLink = userRepository.findByEmail(email).isPresent()
                    ? "http://localhost:8081/join?code=" + classEntity.getClassCode()
                    : "http://localhost:8081/signup?code=" + classEntity.getClassCode();
            message.setText("You're invited to join " + classEntity.getClassName() +
                    "\nUse code: " + classEntity.getClassCode() +
                    "\nOr join here: " + joinLink);
            try {
                mailSender.send(message);
                invite.setStatus(InvitationStatus.ACCEPTED);
                invitationRepository.save(invite);
                logger.info("Email sent successfully to: {}", email);
            } catch (MailException e) {
                failures.put(email, e.getMessage());
                invite.setStatus(InvitationStatus.REJECTED);
                invitationRepository.save(invite);
                logger.error("Failed to send email to {}: {}", email, e.getMessage());
            }
        }

        if (failures.isEmpty()) {
            return ResponseEntity.ok("Invites sent");
        } else {
            return ResponseEntity.ok(Map.of("message", "Some invites sent with failures", "failures", failures));
        }
    }
}