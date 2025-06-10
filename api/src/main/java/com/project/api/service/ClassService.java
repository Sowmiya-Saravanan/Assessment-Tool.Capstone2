package com.project.api.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.api.dto.ClassCreateRequest;
import com.project.api.model.ClassStatus;
import com.project.api.model.Student;
import com.project.api.model.User;
import com.project.api.model.Assessment;
import com.project.api.model.Class;
import com.project.api.model.UserRole;
import com.project.api.repository.ClassRepository;
import com.project.api.repository.StudentRepository;
import com.project.api.repository.UserRepository;

@Service
public class ClassService {

    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    public Optional<Class> findById(Long classId) {
        return classRepository.findById(classId);
    }

    //Educator creating class
   @Transactional
    public Class createClass(ClassCreateRequest request, String educatorEmail) {
        logger.info("Creating class for educator: {}", educatorEmail);

        // Verify the user is an educator
        Optional<User> educatorOpt = userRepository.findByEmail(educatorEmail);
        if (educatorOpt.isEmpty() || educatorOpt.get().getRole() != UserRole.EDUCATOR) {
            logger.error("User {} is not an educator or does not exist", educatorEmail);
            throw new IllegalArgumentException("Only educators can create classes");
        }
        User educator = educatorOpt.get();

        // Validate request
        if (request.getClassName() == null || request.getClassName().trim().isEmpty()) {
            throw new IllegalArgumentException("Class name is required");
        }
        ClassStatus status;
        try {
            status = ClassStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid class status: " + request.getStatus());
        }

        // Create the class
        Class newClass = new Class();
        newClass.setClassName(request.getClassName());
        newClass.setDescription(request.getDescription());
        newClass.setStatus(status);
        newClass.setCreatedBy(educator);
        newClass.setCreatedAt(LocalDateTime.now());
        newClass.setUpdatedAt(LocalDateTime.now());

        // Generate a unique class code
        String classCode;
        int attempts = 0;
        do {
            classCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            attempts++;
            if (attempts > 10) {
                logger.warn("Multiple attempts ({}) to generate unique class code", attempts);
            }
        } while (classRepository.findByClassCode(classCode).isPresent());
        newClass.setClassCode(classCode);

        // Save the class
        Class savedClass = classRepository.save(newClass);
        logger.info("Class created successfully: classId={}, className={}", savedClass.getClassId(), savedClass.getClassName());
        return savedClass;
    }

    
    //Students joining a class
   @Transactional
    public Class joinClass(String classCode, String studentEmail) {
        logger.info("Student {} attempting to join class with code: {}", studentEmail, classCode);

        // Find the User by email
        Optional<User> userOpt = userRepository.findByEmail(studentEmail);
        if (userOpt.isEmpty() || userOpt.get().getRole() != UserRole.STUDENT) {
            logger.error("User {} is not a student or does not exist", studentEmail);
            throw new IllegalArgumentException("Only students can join classes");
        }
        User user = userOpt.get();

        // Find or create the associated Student entity
        Optional<Student> studentOpt = studentRepository.findByUser(user);
        Student student;
        if (studentOpt.isEmpty()) {
            // If no Student entity exists for this User, create one
            student = new Student();
            student.setUser(user);
            student.setCreatedAt(LocalDateTime.now());
            student.setUpdatedAt(LocalDateTime.now());
            student = studentRepository.save(student);
            user.setStudent(student); // Update the User's student field
            userRepository.save(user);
            logger.info("Created new Student entity for user: {}", studentEmail);
        } else {
            student = studentOpt.get();
        }

        // Find the Class by class code
        Optional<Class> classOpt = classRepository.findByClassCode(classCode);
        if (classOpt.isEmpty()) {
            logger.error("Class with code {} does not exist", classCode);
            throw new IllegalArgumentException("Class code does not exist");
        }
        Class targetClass = classOpt.get();

        // Check if the student is already enrolled
        if (targetClass.getStudents().contains(student)) {
            logger.warn("Student {} is already enrolled in class {}", studentEmail, targetClass.getClassId());
            throw new IllegalArgumentException("You are already enrolled in this class");
        }

        // Add the Student to the Class
        targetClass.getStudents().add(student);  // This now works because student is of type Student
        targetClass.setUpdatedAt(LocalDateTime.now());
        Class updatedClass = classRepository.save(targetClass);
        logger.info("Student {} successfully joined class: classId={}, className={}", studentEmail, updatedClass.getClassId(), updatedClass.getClassName());
        return updatedClass;
    }


    //List of classes a student is enrolled in
    public List<Class> getStudentClasses(String studentEmail) {

    logger.info("Fetching classes for student: {}", studentEmail);

    // Find the User by email
    Optional<User> userOpt = userRepository.findByEmail(studentEmail);
    if (userOpt.isEmpty() || userOpt.get().getRole() != UserRole.STUDENT) {
        logger.error("User {} is not a student or does not exist", studentEmail);
        throw new IllegalArgumentException("Only students can view their classes");
    }
    User user = userOpt.get();

    // Find the associated Student entity
    Optional<Student> studentOpt = studentRepository.findByUser(user);
    if (studentOpt.isEmpty()) {
        logger.info("Student entity not found for user: {}. No classes to display.", studentEmail);
        return new ArrayList<>(); // Return empty list if no Student entity exists
    }
    Student student = studentOpt.get();

    // Get the classes the student is enrolled in
    List<Class> classes = student.getClasses(); // Use the Student's classes field
    logger.info("Found {} classes for student: {}", classes.size(), studentEmail);
    return classes;
}

    //List of classes created by educator
    public List<Class> getEducatorClasses(String educatorEmail) {
    logger.info("Fetching classes for educator: {}", educatorEmail);

    Optional<User> userOpt = userRepository.findByEmail(educatorEmail);
    if (userOpt.isEmpty() || userOpt.get().getRole() != UserRole.EDUCATOR) {
        logger.error("User {} is not an educator or does not exist", educatorEmail);
        throw new IllegalArgumentException("Only educators can view their classes");
    }
    User educator = userOpt.get();


    List<Class> classes = classRepository.findByCreatedBy(educator);
        classes.forEach(cls -> {
        cls.getStudents().size(); // Initialize students collection
    });
    logger.info("Found {} classes for educator: {}", classes.size(), educatorEmail);
    return classes;
}


@Transactional
public Map<String, Object> getClassDetailsForStudent(Long classId, String studentEmail) {
    logger.info("Fetching details for class ID: {} by student: {}", classId, studentEmail);
    Optional<User> userOpt = userRepository.findByEmail(studentEmail);
    if (userOpt.isEmpty() || userOpt.get().getRole() != UserRole.STUDENT) {
        logger.error("User {} is not a student", studentEmail);
        throw new IllegalArgumentException("Only students can access class details");
    }
    Optional<Student> studentOpt = studentRepository.findByUser(userOpt.get());
    if (studentOpt.isEmpty()) {
        logger.error("Student entity not found for user: {}", studentEmail);
        throw new IllegalArgumentException("Student not found");
    }
    Optional<Class> classOpt = classRepository.findById(classId);
    if (classOpt.isEmpty()) {
        logger.error("Class with ID {} not found", classId);
        throw new IllegalArgumentException("Class not found");
    }
    Class classroom = classOpt.get();
    if (!classroom.getStudents().contains(studentOpt.get())) {
        logger.error("Student {} is not enrolled in class {}", studentEmail, classId);
        throw new IllegalArgumentException("You are not enrolled in this class");
    }
    Map<String, Object> classDetails = new HashMap<>();
    classDetails.put("classId", classroom.getClassId());
    classDetails.put("className", classroom.getClassName());
    classDetails.put("description", classroom.getDescription());
    classDetails.put("classCode", classroom.getClassCode());
    List<Map<String, Object>> assessmentList = new ArrayList<>();
    for (Assessment assessment : classroom.getAssessments()) {
        Map<String, Object> assessmentInfo = new HashMap<>();
        assessmentInfo.put("id", assessment.getAssessmentId());
        assessmentInfo.put("title", assessment.getTitle());
        assessmentInfo.put("status", assessment.getStatus().toString());
        assessmentList.add(assessmentInfo);
    }
    classDetails.put("assessments", assessmentList);
    logger.info("Successfully fetched details for class ID: {}", classId);
    return classDetails;
}
@Transactional
    public Map<String, Object> getClassDetails(Long classId, String educatorEmail) {
        logger.info("Fetching details for class ID: {} by educator: {}", classId, educatorEmail);

        Optional<User> userOpt = userRepository.findByEmail(educatorEmail);
        if (userOpt.isEmpty() || userOpt.get().getRole() != UserRole.EDUCATOR) {
            logger.error("User {} is not an educator or does not exist", educatorEmail);
            throw new IllegalArgumentException("Only educators can access class details");
        }

        Optional<Class> classOpt = classRepository.findById(classId);
        if (classOpt.isEmpty()) {
            logger.error("Class with ID {} not found", classId);
            throw new IllegalArgumentException("Class not found");
        }

        Class classroom = classOpt.get();
        if (!classroom.getCreatedBy().getEmail().equals(educatorEmail)) {
            logger.error("Educator {} is not authorized to access class {}", educatorEmail, classId);
            throw new IllegalArgumentException("You are not authorized to view this class details");
        }

        Map<String, Object> classDetails = new HashMap<>();
        List<Map<String, String>> studentList = new ArrayList<>();
        for (Student student : classroom.getStudents()) {
            User user = student.getUser();
            Map<String, String> studentInfo = new HashMap<>();
            studentInfo.put("email", user.getEmail());
            studentInfo.put("name", user.getName() != null ? user.getName() : "");

            studentList.add(studentInfo);
        }
        classDetails.put("students", studentList);

        List<Map<String, Object>> assessmentList = new ArrayList<>();
        for (Assessment assessment : classroom.getAssessments()) {
            Map<String, Object> assessmentInfo = new HashMap<>();
            assessmentInfo.put("id", assessment.getAssessmentId());
            assessmentInfo.put("title", assessment.getTitle());
            assessmentInfo.put("status", assessment.getStatus().toString());
            assessmentList.add(assessmentInfo);
        }
        classDetails.put("assessments", assessmentList);

        logger.info("Successfully fetched details for class ID: {}", classId);
        return classDetails;
    }
}

    
