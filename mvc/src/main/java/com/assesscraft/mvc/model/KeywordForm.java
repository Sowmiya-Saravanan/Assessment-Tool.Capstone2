package com.assesscraft.mvc.model;

import lombok.Data;

@Data
public class KeywordForm {
    private String keyword;
    private Integer weight; // Points assigned to this keyword

    public KeywordForm() {
        this.weight = 1; // Default weight
    }
}
