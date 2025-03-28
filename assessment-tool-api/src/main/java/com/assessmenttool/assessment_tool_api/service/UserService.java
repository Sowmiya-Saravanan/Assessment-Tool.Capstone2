// package com.assessmenttool.assessment_tool_api.service;

// import com.assessmenttool.assessment_tool_api.model.Role;
// import com.assessmenttool.assessment_tool_api.model.User;
// import com.assessmenttool.assessment_tool_api.repository.UserRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.oauth2.jwt.Jwt;
// import org.springframework.stereotype.Service;

// import java.util.List;
// import java.util.Optional;

// @Service
// public class UserService {

//     @Autowired
//     private UserRepository userRepository;

//     // Create a user during first login (via Keycloak JWT)
//     public User findOrCreateUser(Jwt jwt, Role expectedRole) {
//         String externalId = jwt.getSubject();
//         Optional<User> userOptional = userRepository.findByExternalId(externalId);

//         User user;
//         if (userOptional.isEmpty()) {
//             user = new User();
//             user.setExternalId(externalId);
//             user.setName(jwt.getClaimAsString("name"));
//             user.setEmail(jwt.getClaimAsString("email"));
//             user.setRole(expectedRole);
//             user = userRepository.save(user);
//         } else {
//             user = userOptional.get();
//         }

//         if (user.getRole() != expectedRole) {
//             throw new SecurityException("User does not have the required role: " + expectedRole);
//         }

//         return user;
//     }

//     // Create a user manually (by admin)
//     public User createUser(User user) {
//         // Validate required fields
//         if (user.getExternalId() == null || user.getExternalId().isEmpty()) {
//             throw new IllegalArgumentException("External ID is required");
//         }
//         if (user.getName() == null || user.getName().isEmpty()) {
//             throw new IllegalArgumentException("Name is required");
//         }
//         if (user.getEmail() == null || user.getEmail().isEmpty()) {
//             throw new IllegalArgumentException("Email is required");
//         }
//         if (user.getRole() == null) {
//             throw new IllegalArgumentException("Role is required");
//         }

//         // Check for uniqueness
//         if (userRepository.findByExternalId(user.getExternalId()).isPresent()) {
//             throw new IllegalArgumentException("User with external ID " + user.getExternalId() + " already exists");
//         }
//         if (userRepository.findByEmail(user.getEmail()).isPresent()) {
//             throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists");
//         }

//         return userRepository.save(user);
//     }

//     // Read a user by ID
//     public User findById(Long id) {
//         return userRepository.findById(id)
//                 .orElseThrow(() -> new IllegalArgumentException("User with ID " + id + " not found"));
//     }

//     // Read a user by external ID
//     public Optional<User> findByExternalId(String externalId) {
//         return userRepository.findByExternalId(externalId);
//     }

//     // Read all users
//     public List<User> findAll() {
//         return userRepository.findAll();
//     }

//     // Update a user
//     public User updateUser(Long id, User updatedUser) {
//         User user = findById(id);

//         // Update fields if provided
//         if (updatedUser.getName() != null && !updatedUser.getName().isEmpty()) {
//             user.setName(updatedUser.getName());
//         }
//         if (updatedUser.getEmail() != null && !updatedUser.getEmail().isEmpty()) {
//             Optional<User> userWithEmail = userRepository.findByEmail(updatedUser.getEmail());
//             if (userWithEmail.isPresent() && !userWithEmail.get().getUserId().equals(id)) {
//                 throw new IllegalArgumentException("Email " + updatedUser.getEmail() + " is already in use");
//             }
//             user.setEmail(updatedUser.getEmail());
//         }
//         if (updatedUser.getRole() != null) {
//             user.setRole(updatedUser.getRole());
//         }

//         return userRepository.save(user);
//     }

//     // Delete a user
//     public void deleteUser(Long id) {
//         User user = findById(id);
//         userRepository.delete(user);
//     }
// }