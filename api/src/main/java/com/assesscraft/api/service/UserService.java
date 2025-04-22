package com.assesscraft.api.service;

import com.assesscraft.api.model.Role;
import com.assesscraft.api.model.User;
import com.assesscraft.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Fetch all users with a specific role (e.g., STUDENT).
     * @param role The role to filter by.
     * @return List of users with the specified role.
     */
    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    /**
     * Find a user by their email address.
     * @param email The email to search for.
     * @return Optional containing the user, if found.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Placeholder for fetching pending students (to be implemented with a pending table later).
     * @param classId The class ID to filter pending students by.
     * @return List of pending students (currently empty).
     */
    public List<User> findPendingStudentsByClassId(Long classId) {
        // This is a placeholder; implement with a pending_students table or logic
        return List.of(); // Return empty list for now
    }
}