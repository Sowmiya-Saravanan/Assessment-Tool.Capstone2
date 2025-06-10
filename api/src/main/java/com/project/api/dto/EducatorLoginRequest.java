package com.project.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EducatorLoginRequest {

    private String email;
    private String password;

}