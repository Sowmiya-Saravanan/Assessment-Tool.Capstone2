package com.assesscraft.mvc.model;

import lombok.Data;

@Data
public class OptionForm {
    private String text;
    private boolean isCorrect; // Indicates if this option is the correct one for MCQ

    public OptionForm() {
        this.isCorrect = false;
    }
}