package com.assesscraft.mvc.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class QuestionForm {
    private Long questionId;
    private String content;
    private String type; // e.g., "MCQ", "TRUE_FALSE", "SHORT_ANSWER", "ESSAY", "CODING"
    private Integer maxScore;
    private List<OptionForm> options; // For MCQ
    private String trueFalseAnswer; // For TRUE_FALSE
    private List<KeywordForm> keywords; // For SHORT_ANSWER/ESSAY

    public QuestionForm() {
        this.options = new ArrayList<>();
        this.keywords = new ArrayList<>();
    }
}