package com.assesscraft.api.service;

import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.ClassStatus;
import com.assesscraft.api.model.Role;
import com.assesscraft.api.model.User;
import com.assesscraft.api.repository.ClassRepository;
import com.assesscraft.api.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ClassService {

    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

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
        classRepository.save(classEntity);
    }

    @Transactional(readOnly = true)
    public List<User> getPendingStudents(Long classId) {
        return List.of(); // Placeholder
    }

    @Transactional
    public void approveStudents(Long classId, List<Long> studentIds) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        List<User> students = userRepository.findAllById(studentIds)
                .stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .toList();
        classEntity.getStudents().addAll(students);
        classRepository.save(classEntity);
    }

    @Transactional
    public void bulkAssignStudents(Long classId, MultipartFile file) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            throw new RuntimeException("Only Excel files (.xlsx or .xls) are allowed");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> emails = new ArrayList<>();
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header
                String email = row.getCell(0).getStringCellValue().trim();
                if (!email.isEmpty() && EMAIL_PATTERN.matcher(email).matches()) {
                    emails.add(email);
                } else {
                    logger.warn("Invalid email format skipped: {}", email);
                }
            }

            for (String email : emails) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                String classCode = classEntity.getClassCode();
                String subject = "Class Code for " + classEntity.getClassName();
                String registrationUrl = "http://localhost:8081/student/register?email=" + email + "&code=" + classCode;

                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject(subject);

                try {
                    if (userOpt.isPresent() && userOpt.get().getRole() ==Role.STUDENT) {
                        // Existing student: Send class code
                        message.setText("Hello,\n\nYour class code for " + classEntity.getClassName() + " is: " + classCode + "\n\nJoin using this code on the student portal.\n\nBest,\nAssessCraft Team");
                        mailSender.send(message);
                        logger.info("Class code email sent to existing student: {}", email);
                        classEntity.getStudents().add(userOpt.get());
                    } else {
                        // New student: Send registration link
                        message.setText("Hello,\n\nYou have been invited to join " + classEntity.getClassName() + ".\nPlease register using this link: " + registrationUrl + "\n\nBest,\nAssessCraft Team");
                        mailSender.send(message);
                        logger.info("Registration link email sent to new student: {}", email);
                    }
                } catch (MailException e) {
                    logger.error("Failed to send email to {}: {}", email, e.getMessage());
                }
            }
            classRepository.save(classEntity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage());
        }
    }
}