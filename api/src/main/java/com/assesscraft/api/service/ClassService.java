package com.assesscraft.api.service;

import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.ClassStatus;
import com.assesscraft.api.model.Invitation;
import com.assesscraft.api.model.InvitationStatus;
import com.assesscraft.api.model.Role;
import com.assesscraft.api.model.User;
import com.assesscraft.api.repository.ClassRepository;
import com.assesscraft.api.repository.InvitationRepository;
import com.assesscraft.api.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ClassService {

    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private InvitationRepository invitationRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$",
        Pattern.CASE_INSENSITIVE
    );

    public Class saveClass(Class classEntity) {
        return classRepository.save(classEntity);
    }

    @Transactional(readOnly = true)
    public List<Class> findByEducatorAndStatus(User educator, ClassStatus status) {
        return classRepository.findByEducatorAndStatus(educator, status);
    }

    public Optional<Class> findById(Long classId) {
        return classRepository.findById(classId);
    }

    @Transactional
    public void assignStudents(Long classId, List<Long> studentIds) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        List<User> students = userRepository.findAllById(studentIds)
                .stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .toList();
        classEntity.getStudents().addAll(students);
        updateClassStatusIfNeeded(classEntity, students);
        classRepository.save(classEntity);
    }

    @Transactional(readOnly = true)
    public List<Invitation> getSentInvitations(Long classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        return invitationRepository.findAllByClassEntityAndStatus(classEntity, InvitationStatus.SENT);
    }

    @Transactional
    public void approveStudents(Long classId, List<Long> invitationIds) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        List<Invitation> invitations = invitationRepository.findAllById(invitationIds)
                .stream()
                .filter(inv -> inv.getClassEntity().getClassId().equals(classId))
                .toList();

        if (invitations.isEmpty()) {
            logger.warn("No valid invitations found for class {}", classId);
            return;
        }

        List<User> students = new ArrayList<>();
        for (Invitation invitation : invitations) {
            Optional<User> userOpt = userRepository.findByEmail(invitation.getEmail());
            userOpt.ifPresent(user -> {
                if (user.getRole() == Role.STUDENT) {
                    students.add(user);
                } else {
                    logger.warn("User not a student for invitation: {}", invitation.getEmail());
                }
            });
        }

        classEntity.getStudents().addAll(students);
        updateClassStatusIfNeeded(classEntity, students);
        classRepository.save(classEntity);
    }
    
    @Transactional
    public Map<String, Object> bulkAssignStudents(Long classId, MultipartFile file) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
    
        if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
            throw new RuntimeException("Only Excel files (.xlsx or .xls) are allowed");
        }
    
        Map<String, Object> result = new HashMap<>();
        List<String> invalidEmails = new ArrayList<>();
        List<String> emailFailures = new ArrayList<>();
        List<User> addedStudents = new ArrayList<>();
    
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> emails = new ArrayList<>();
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header
                String email = row.getCell(0).getStringCellValue().trim();
                if (!email.isEmpty() && EMAIL_PATTERN.matcher(email).matches()) {
                    emails.add(email);
                } else {
                    invalidEmails.add(email);
                    logger.warn("Invalid email format skipped: {}", email);
                }
            }
            emails = emails.stream().distinct().collect(Collectors.toList());
    
            // Process students and invitations
            for (String email : emails) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                User user;
                if (userOpt.isPresent()) {
                    user = userOpt.get();
                    if (user.getRole() != Role.STUDENT) {
                        invalidEmails.add(email);
                        continue;
                    }
                } else {
                    user = new User();
                    user.setEmail(email);
                    user.setRole(Role.STUDENT);
                    user.setPassword(UUID.randomUUID().toString().substring(0, 8)); // To be hashed
                    user = userRepository.save(user);
                    logger.info("Created new student: {} with temp password", email);
                }
    
                // Check and handle invitation
                Invitation existingInvite = invitationRepository.findByEmailAndClassEntity(email, classEntity)
                        .orElse(null);
                if (existingInvite == null) {
                    Invitation invite = new Invitation();
                    invite.setClassEntity(classEntity);
                    invite.setEmail(email);
                    invite.setClassCode(classEntity.getClassCode());
                    invite.setStatus(InvitationStatus.SENT);
                    invite.setTemporaryPassword(UUID.randomUUID().toString().substring(0, 8));
                    invite.setCreatedAt(LocalDateTime.now());
                    invite.setExpirationDate(LocalDateTime.now().plusDays(7));
                    try {
                        invitationRepository.save(invite);
                        classEntity.getInvitations().add(invite);
                        logger.info("Created new invitation for: {}", email);
                    } catch (DataIntegrityViolationException e) {
                        logger.warn("Duplicate invitation detected for {} in class {}, skipping", email, classId);
                        continue; // Skip to next email if duplicate
                    }
                } else {
                    logger.info("Existing invitation found for {} in class {}, reusing", email, classId);
                }
    
                if (!classEntity.getStudents().contains(user)) {
                    classEntity.getStudents().add(user);
                    addedStudents.add(user);
                    logger.debug("Added student {} to class {}", email, classId);
                } else {
                    logger.info("Student {} already in class {}, skipping addition", email, classId);
                }
            }
    
            // Update status and save
            updateClassStatusIfNeeded(classEntity, addedStudents);
            classRepository.save(classEntity);
            logger.debug("Class {} status after save: {}", classId, classEntity.getStatus());
    
            // Send emails (non-transactional)
            for (String email : emails) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent() && userOpt.get().getRole() != Role.STUDENT) continue;
    
                Invitation invite = invitationRepository.findByEmailAndClassEntity(email, classEntity)
                        .orElseThrow(() -> new RuntimeException("Invitation not found for " + email));
                sendInvitationEmail(classEntity, userOpt.orElse(null), invite.getTemporaryPassword());
            }
    
            if (!invalidEmails.isEmpty()) result.put("invalidEmails", invalidEmails);
            if (!emailFailures.isEmpty()) result.put("emailFailures", emailFailures);
            if (!addedStudents.isEmpty()) result.put("addedStudents", addedStudents.size());
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage(), e);
        }
    }
    @Transactional
    public void addStudent(Long classId, String email) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;
        String tempPassword = null;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (user.getRole() != Role.STUDENT) {
                throw new RuntimeException("User is not a student: " + email);
            }
        } else {
            user = new User();
            user.setEmail(email);
            user.setRole(Role.STUDENT);
            tempPassword = UUID.randomUUID().toString().substring(0, 8);
            user.setPassword(tempPassword); // Should be hashed in UserService
            user = userRepository.save(user);
            logger.info("Created new student: {} with password: {}", email, tempPassword);

            Invitation existingInvite = invitationRepository.findByEmailAndClassEntity(email, classEntity)
                    .orElse(null);
            if (existingInvite == null) {
                Invitation invite = new Invitation();
                invite.setClassEntity(classEntity);
                invite.setEmail(email);
                invite.setClassCode(classEntity.getClassCode());
                invite.setStatus(InvitationStatus.SENT);
                invite.setTemporaryPassword(tempPassword);
                invite.setCreatedAt(LocalDateTime.now());
                invite.setExpirationDate(LocalDateTime.now().plusDays(7));
                classEntity.getInvitations().add(invite);
                saveInvitation(invite);
                logger.info("Created invitation for unregistered student: {}", email);
            }
        }

        if (!classEntity.getStudents().contains(user)) {
            classEntity.getStudents().add(user);
            updateClassStatusIfNeeded(classEntity, List.of(user));
            classRepository.save(classEntity);
            logger.info("Added student {} to class {}", email, classId);

            sendInvitationEmail(classEntity, user, tempPassword);
        } else {
            logger.info("Student {} already associated with class {}, skipping addition", email, classId);
        }
    }

    @Transactional
    public void removeStudent(Long classId, String email) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (classEntity.getStudents().contains(user)) {
                classEntity.getStudents().remove(user);
                classRepository.save(classEntity);
                logger.info("Removed student {} from class {}", email, classId);
            } else {
                logger.warn("Student {} not found in class {}", email, classId);
            }
        } else {
            Invitation invitation = invitationRepository.findByEmailAndClassEntity(email, classEntity)
                    .orElse(null);
            if (invitation != null) {
                invitationRepository.delete(invitation);
                classEntity.getInvitations().remove(invitation);
                classRepository.save(classEntity);
                logger.info("Removed invitation for {} from class {}", email, classId);
            } else {
                logger.warn("No invitation or student found for email {} in class {}", email, classId);
            }
        }
    }

    @Transactional
    public void deleteClass(Long classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        classRepository.delete(classEntity);
        logger.info("Deleted class {}", classId);
    }

    @Transactional(readOnly = true)
    public List<User> getStudents(Long classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        return new ArrayList<>(classEntity.getStudents());
    }

    @Transactional
    public Invitation saveInvitation(Invitation invitation) {
        return invitationRepository.save(invitation);
    }

    private void updateClassStatusIfNeeded(Class classEntity, List<User> students) {
        if (classEntity.getStatus() == ClassStatus.DRAFT && !students.isEmpty()) {
            classEntity.setStatus(ClassStatus.ACTIVE);
            classEntity.setUpdatedAt(LocalDateTime.now());
            logger.info("Class {} status updated to ACTIVE after adding students", classEntity.getClassId());
        }
    }

    private void sendInvitationEmail(Class classEntity, User user, String tempPassword) {
        String email = user.getEmail();
        String classCode = classEntity.getClassCode();
        String subject = "Class Invitation for " + classEntity.getClassName();
        String registrationUrl = "http://localhost:8081/student/register?email=" + email + "&code=" + classCode;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        try {
            Invitation invite = classEntity.getInvitations().stream()
                    .filter(i -> i.getEmail().equals(email))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Invitation not found for " + email));
            String passwordToSend = (user.getRole() == Role.STUDENT && userRepository.findByEmail(email).isPresent()) ? null : tempPassword;
            if (passwordToSend != null) {
                message.setText("Hello,\n\nYou have been invited to join " + classEntity.getClassName() + ".\n" +
                        "Please register using this link: " + registrationUrl + "\n" +
                        "Temporary password: " + passwordToSend + "\n" +
                        "You can change it after logging in.\n\nBest,\nAssessCraft Team");
            } else {
                message.setText("Hello,\n\nYour class code for " + classEntity.getClassName() + " is: " + classCode + "\n\nJoin using this code on the student portal.\n\nBest,\nAssessCraft Team");
            }
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", email);
        } catch (MailException e) {
            logger.error("Failed to send email to {}: {}", email, e.getMessage());
            Invitation failedInvite = invitationRepository.findByEmailAndClassEntity(email, classEntity)
                    .orElseThrow(() -> new RuntimeException("Invitation not found"));
            failedInvite.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(failedInvite);
            throw new RuntimeException("Failed to send invitation email: " + e.getMessage());
        }
    }
}