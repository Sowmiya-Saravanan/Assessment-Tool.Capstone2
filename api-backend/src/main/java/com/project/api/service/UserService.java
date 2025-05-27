package com.project.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.api.dto.EducatorLoginRequest;
import com.project.api.dto.EducatorRegisterRequest;
import com.project.api.dto.StudentLoginRequest;
import com.project.api.dto.StudentRegisterRequest;
import com.project.api.dto.AdminRegisterRequest;
import com.project.api.model.Student;
import com.project.api.model.User;
import com.project.api.model.UserRole;
import com.project.api.repository.StudentRepository;
import com.project.api.repository.UserRepository;
import com.project.api.exception.UserAlreadyExistsException;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    // Email regex for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    /**
     * Find a user by their email address.
     * @param email The email to search for.
     * @return Optional containing the user, if found.
     */
    public Optional<User> findUserByEmail(String email) {
        logger.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Find users by their role.
     * @param role The role to search for.
     * @return List of users with the specified role.
     */
    public List<User> findUserByRole(UserRole role) {
        logger.debug("Finding user by role: {}", role);
        return userRepository.findByRole(role);
    }

    // Register new users - Educator
    @Transactional
    public User registerEducator(EducatorRegisterRequest registerRequest) {
        logger.info("Attempting to register Educator with email: {}", registerRequest.getEmail());
        return registerUser(registerRequest, UserRole.EDUCATOR);
    }

    // Register new users - Student
    @Transactional
    public User registerStudent(StudentRegisterRequest registerRequest) {
        logger.info("Attempting to register Student with email: {}", registerRequest.getEmail());
        return registerUser(registerRequest, UserRole.STUDENT);
    }

    // Register new users - Admin
    @Transactional
    public User registerAdmin(AdminRegisterRequest registerRequest) {
        logger.info("Attempting to register Admin with email: {}", registerRequest.getEmail());
        return registerUser(registerRequest, UserRole.ADMIN);
    }

    // Overloaded registerUser for Educator
    private User registerUser(EducatorRegisterRequest registerRequest, UserRole role) {
        validateRegistrationRequest(registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getConfirmPassword());

        User newUser = new User();
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setName(registerRequest.getName());
        newUser.setRole(role);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        return saveUser(newUser, role);
    }

    // Overloaded registerUser for Student
    private User registerUser(StudentRegisterRequest registerRequest, UserRole role) {
    validateRegistrationRequest(registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getConfirmPassword());

            User newUser = new User();
            newUser.setEmail(registerRequest.getEmail());
            newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            newUser.setName(registerRequest.getName());
            newUser.setRole(role);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());

            newUser = userRepository.save(newUser); // Save the User first

            // Create a Student entity for STUDENT role
            Student student = new Student();
            student.setUser(newUser);
            student.setCreatedAt(LocalDateTime.now());
            student.setUpdatedAt(LocalDateTime.now());
            studentRepository.save(student);

            newUser.setStudent(student);
            userRepository.save(newUser);

            return saveUser(newUser, role);
        }

    // Overloaded registerUser for Admin
    private User registerUser(AdminRegisterRequest registerRequest, UserRole role) {
        validateRegistrationRequest(registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getConfirmPassword());

        User newUser = new User();
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setName(registerRequest.getName());
        newUser.setRole(role);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        return saveUser(newUser, role); // Simply save the user without creating a Student entity
    }

    // Common validation logic for registration

    private void validateRegistrationRequest(String email, String password, String confirmPassword) {
        // Validate email format
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            logger.warn("Invalid email format: {}", email);
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check if email is already registered
        if (userRepository.findByEmail(email).isPresent()) {
            logger.warn("Email is already registered: {}", email);
            throw new UserAlreadyExistsException("Email is already registered");
        }

        // Validate password
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            logger.warn("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character for email: {}", email);
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character");
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            logger.warn("Password and confirm password do not match for email: {}", email);
            throw new IllegalArgumentException("Passwords do not match");
        }
    }

    // Common method to save user
    private User saveUser(User newUser, UserRole role) {
        User savedUser = userRepository.save(newUser);
        userRepository.flush(); // Ensure the user is saved immediately

        logger.info("{} registered successfully with email: {}", role, savedUser.getEmail());
        return savedUser;
    }

    // Login methods
    public Optional<User> loginEducator(EducatorLoginRequest loginRequest) {
    Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

    if (userOpt.isEmpty() || userOpt.get().getRole() != UserRole.EDUCATOR) {
        logger.warn("Educator login failed for email: {}. User does not exist or is not an educator.", loginRequest.getEmail());
        return Optional.empty();
    }

    User user = userOpt.get();
    if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
        logger.info("Educator logged in successfully with email: {}", loginRequest.getEmail());
        return Optional.of(user);
    } else {
        logger.warn("Educator login failed for email: {}. Incorrect password.", loginRequest.getEmail());
        return Optional.empty();
    }
}

    public Optional<User> loginStudent(StudentLoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        if (userOpt.isEmpty() || userOpt.get().getRole() != UserRole.STUDENT) {
            logger.warn("Student login failed for email: {}. User does not exist or is not a student.", loginRequest.getEmail());
            return Optional.empty();
        }

        User user = userOpt.get();
        if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.info("Student logged in successfully with email: {}", loginRequest.getEmail());
            return Optional.of(user);
        } else {
            logger.warn("Student login failed for email: {}. Incorrect password.", loginRequest.getEmail());
            return Optional.empty();
        }
    }
    }