package com.assesscraft.api.controller;

import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.Invitation;
import com.assesscraft.api.model.InvitationStatus;
import com.assesscraft.api.repository.ClassRepository;
import com.assesscraft.api.repository.InvitationRepository;
import com.assesscraft.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/invitations")
    public ResponseEntity<?> sendInvites(@RequestBody InviteRequest request) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Class classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Class not found"));
        if (!classEntity.getEducator().getUserId().toString().equals(userId) &&
                !classEntity.getEducator().getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).body("Not authorized to invite for this class");
        }

        for (String email : request.getEmails()) {
            Invitation invite = new Invitation();
            invite.setClassEntity(classEntity);
            invite.setEmail(email);
            invite.setClassCode(classEntity.getClassCode());
            invite.setStatus(InvitationStatus.PENDING);
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
            mailSender.send(message);
        }

        return ResponseEntity.ok("Invites sent");
    }
}