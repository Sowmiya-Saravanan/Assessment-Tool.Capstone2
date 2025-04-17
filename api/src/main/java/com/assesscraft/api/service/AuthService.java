// package com.assesscraft.api.service;

// import com.assesscraft.api.dto.AuthRequest;
// import com.assesscraft.api.model.User;
// import com.assesscraft.api.repository.UserRepository;
// import com.assesscraft.api.util.JwtUtil;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.AuthenticationException;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.stereotype.Service;

// import java.util.HashMap;
// import java.util.Map;

// @Service
// public class AuthService {
//     private final AuthenticationManager authenticationManager;
//     private final UserRepository userRepository;
//     private final JwtUtil jwtUtil;

//     public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtUtil jwtUtil) {
//         this.authenticationManager = authenticationManager;
//         this.userRepository = userRepository;
//         this.jwtUtil = jwtUtil;
//     }

//     public Map<String, String> login(AuthRequest authRequest) throws AuthenticationException {
//         authenticationManager.authenticate(
//             new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword()));

//         User user = userRepository.findByEmail(authRequest.getEmail())
//                 .orElseThrow(() -> new UsernameNotFoundException("User not found"));

//         String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

//         Map<String, String> response = new HashMap<>();
//         response.put("token", token);
//         response.put("role", user.getRole().name());
//         return response;
//     }

//     public boolean validateToken(String token) {
//         try {
//             jwtUtil.validateToken(token);
//             return true;
//         } catch (Exception e) {
//             return false;
//         }
//     }
// }