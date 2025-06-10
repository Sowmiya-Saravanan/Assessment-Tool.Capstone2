package com.project.api.dto;

import lombok.Data;

@Data
public class ClassCreateRequest {

    private String className;
    private String description;
    private String status;
}
