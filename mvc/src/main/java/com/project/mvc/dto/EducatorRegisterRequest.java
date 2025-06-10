package com.project.mvc.dto;



import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class EducatorRegisterRequest {

     @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        private String password;
    
        @NotBlank(message = "Confirm password is required")
        @Size(min = 8, max = 128, message = "Confirm password must be between 8 and 128 characters")
        private String confirmPassword;

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        private String name;
}