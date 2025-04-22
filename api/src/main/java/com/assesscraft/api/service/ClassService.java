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
        if (classEntity.getStatus() == ClassStatus.DRAFT && !students.isEmpty()) {
            classEntity.setStatus(ClassStatus.ACTIVE);
            classEntity.setUpdatedAt(LocalDateTime.now());
        }
        classRepository.save(classEntity);
    }

    @Transactional(readOnly = true)
    public List<Invitation> getPendingStudents(Long classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        return classEntity.getInvitations().stream()
                .filter(inv -> inv.getStatus() == InvitationStatus.PENDING)
                .toList();
    }

    @Transactional
    public void approveStudents(Long classId, List<Long> invitationIds) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        List<Invitation> invitations = invitationRepository.findAllById(invitationIds)
                .stream()
                .filter(inv -> inv.getClassEntity().getClassId().equals(classId) && inv.getStatus() == InvitationStatus.PENDING)
                .toList();

        if (invitations.isEmpty()) {
            logger.warn("No valid pending invitations found for class {}", classId);
            return;
        }

        List<User> students = new ArrayList<>();
        for (Invitation invitation : invitations) {
            Optional<User> userOpt = userRepository.findByEmail(invitation.getEmail());
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.STUDENT) {
                students.add(userOpt.get());
                invitation.setStatus(InvitationStatus.ACCEPTED);
                invitationRepository.save(invitation);
            } else {
                logger.warn("User not found or not a student for invitation: {}", invitation.getEmail());
            }
        }

        classEntity.getStudents().addAll(students);
        if (classEntity.getStatus() == ClassStatus.DRAFT && !students.isEmpty()) {
            classEntity.setStatus(ClassStatus.ACTIVE);
            classEntity.setUpdatedAt(LocalDateTime.now());
        }
        classRepository.save(classEntity);
    }

    @Transactional
    public Map<String, Object> bulkAssignStudents(Long classId, MultipartFile file) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
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
            emails = emails.stream().distinct().collect(Collectors.toList()); // Deduplicate emails

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
                    user.setPassword(UUID.randomUUID().toString().substring(0, 8));
                    user = userRepository.save(user);
                    logger.info("Created new student: {}", email);

                    Invitation existingInvite = invitationRepository.findByEmailAndClassEntity(email, classEntity)
                            .orElse(null);
                    if (existingInvite == null || existingInvite.getStatus() != InvitationStatus.PENDING) {
                        Invitation invite = new Invitation();
                        invite.setClassEntity(classEntity);
                        invite.setEmail(email);
                        invite.setClassCode(classEntity.getClassCode());
                        invite.setStatus(InvitationStatus.PENDING);
                        invite.setCreatedAt(LocalDateTime.now());
                        classEntity.getInvitations().add(invite);
                        invitationRepository.save(invite);
                        logger.info("Created invitation for unregistered student: {}", email);
                    }
                }
                if (!classEntity.getStudents().contains(user)) {
                    classEntity.getStudents().add(user);
                    addedStudents.add(user);
                    logger.debug("Added student {} to class {}", email, classId);
                } else {
                    logger.info("Student {} already associated with class {}, skipping addition", email, classId);
                }

                String classCode = classEntity.getClassCode();
                String subject = "Class Code for " + classEntity.getClassName();
                String registrationUrl = "http://localhost:8081/student/register?email=" + email + "&code=" + classCode;

                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject(subject);

                try {
                    if (userOpt.isPresent() && userOpt.get().getRole() == Role.STUDENT) {
                        message.setText("Hello,\n\nYour class code for " + classEntity.getClassName() + " is: " + classCode + "\n\nJoin using this code on the student portal.\n\nBest,\nAssessCraft Team");
                    } else {
                        message.setText("Hello,\n\nYou have been invited to join " + classEntity.getClassName() + ".\nPlease register using this link: " + registrationUrl + "\n\nBest,\nAssessCraft Team");
                    }
                    mailSender.send(message);
                    logger.info("Email sent successfully to: {}", email);
                } catch (MailException e) {
                    emailFailures.add(email + ": " + e.getMessage());
                    logger.error("Failed to send email to {}: {}", email, e.getMessage());
                }
            }

            try {
                classRepository.save(classEntity);
                if (classEntity.getStatus() == ClassStatus.DRAFT && !classEntity.getStudents().isEmpty()) {
                    classEntity.setStatus(ClassStatus.ACTIVE);
                    classEntity.setUpdatedAt(LocalDateTime.now());
                    classRepository.save(classEntity);
                    logger.info("Class {} status updated to ACTIVE after adding students", classId);
                }
            } catch (DataIntegrityViolationException e) {
                logger.warn("Duplicate student-class association detected for class {}, skipping: {}", classId, e.getMessage());
            }

            if (!invalidEmails.isEmpty()) result.put("invalidEmails", invalidEmails);
            if (!emailFailures.isEmpty()) result.put("emailFailures", emailFailures);
            if (!addedStudents.isEmpty()) result.put("addedStudents", addedStudents.size());
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage());
        }
    }

    @Transactional
    public void addStudent(Long classId, String email) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (user.getRole() != Role.STUDENT) {
                throw new RuntimeException("User is not a student: " + email);
            }
        } else {
            user = new User();
            user.setEmail(email);
            user.setRole(Role.STUDENT);
            user.setPassword(UUID.randomUUID().toString().substring(0, 8));
            user = userRepository.save(user);
            logger.info("Created new student: {}", email);

            Invitation existingInvite = invitationRepository.findByEmailAndClassEntity(email, classEntity)
                    .orElse(null);
            if (existingInvite == null || existingInvite.getStatus() != InvitationStatus.PENDING) {
                Invitation invite = new Invitation();
                invite.setClassEntity(classEntity);
                invite.setEmail(email);
                invite.setClassCode(classEntity.getClassCode());
                invite.setStatus(InvitationStatus.PENDING); // Fixed to use enum
                invite.setCreatedAt(LocalDateTime.now());
                classEntity.getInvitations().add(invite);
                saveInvitation(invite); // Use new method
                logger.info("Created invitation for unregistered student: {}", email);
            }
        }

        if (!classEntity.getStudents().contains(user)) {
            classEntity.getStudents().add(user);
            if (classEntity.getStatus() == ClassStatus.DRAFT) {
                classEntity.setStatus(ClassStatus.ACTIVE);
                classEntity.setUpdatedAt(LocalDateTime.now());
            }
            classRepository.save(classEntity);
            logger.info("Added student {} to class {}", email, classId);

            // Send email
            String classCode = classEntity.getClassCode();
            String subject = "Class Code for " + classEntity.getClassName();
            String registrationUrl = "http://localhost:8081/student/register?email=" + email + "&code=" + classCode;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.STUDENT) {
                message.setText("Hello,\n\nYour class code for " + classEntity.getClassName() + " is: " + classCode + "\n\nJoin using this code on the student portal.\n\nBest,\nAssessCraft Team");
            } else {
                message.setText("Hello,\n\nYou have been invited to join " + classEntity.getClassName() + ".\nPlease register using this link: " + registrationUrl + "\n\nBest,\nAssessCraft Team");
            }
            try {
                mailSender.send(message);
                logger.info("Email sent successfully to: {}", email);
            } catch (MailException e) {
                logger.error("Failed to send email to {}: {}", email, e.getMessage());
                throw new RuntimeException("Failed to send invitation email: " + e.getMessage());
            }
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
}