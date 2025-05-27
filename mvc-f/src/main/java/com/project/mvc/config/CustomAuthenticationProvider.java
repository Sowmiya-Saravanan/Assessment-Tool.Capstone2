// package com.project.mvc.config;

// import com.project.mvc.dto.EducatorLoginRequest;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.authentication.AuthenticationProvider;
// import org.springframework.security.authentication.BadCredentialsException;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.AuthenticationException;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.stereotype.Component;
// import org.springframework.web.client.RestTemplate;

// import java.util.Collections;
// import java.util.Map;

// @Component
// public class CustomAuthenticationProvider implements AuthenticationProvider {

//     @Autowired
//     private RestTemplate restTemplate;

//     @Value("${api.base.url}")
//     private String backendUrl;

//     @Override
//     public Authentication authenticate(Authentication authentication) throws AuthenticationException {
//         String email = authentication.getName();
//         String password = authentication.getCredentials().toString();

//         EducatorLoginRequest loginRequest = new EducatorLoginRequest();
//         loginRequest.setEmail(email);
//         loginRequest.setPassword(password);

//         ResponseEntity<Map> response = restTemplate.postForEntity(
//             backendUrl + "/api/educator/login",
//             loginRequest,
//             Map.class
//         );

//         if (response.getStatusCode() == HttpStatus.OK && "success".equals(response.getBody().get("status"))) {
//             String role = (String) response.getBody().get("role");
//             return new UsernamePasswordAuthenticationToken(
//                 email,
//                 password,
//                 Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
//             );
//         } else {
//             throw new BadCredentialsException("Invalid email or password");
//         }
//     }

//     @Override
//     public boolean supports(Class<?> authentication) {
//         return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
//     }
// }