package com.project.mvc.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AssessmentCreateRequest {

    private String title;
    private String description;
    private String type; // e.g., "QUIZ", "EXAM"
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String gradingMode; // e.g., "AUTO", "MANUAL"
    private List<Question> questions;

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getGradingMode() {
        return gradingMode;
    }

    public void setGradingMode(String gradingMode) {
        this.gradingMode = gradingMode;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    // Nested Question class
    public static class Question {
        private String text;
        private String type; // e.g., "MCQ", "TRUE_FALSE", "SHORT_ANSWER", "ESSAY"
        private Double maxScore;
        private String correctAnswer; // Index for MCQ, "true"/"false" for TRUE_FALSE
        private List<Option> options;
        private List<Keyword> keywords;

        // Getters and Setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Double getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(Double maxScore) {
            this.maxScore = maxScore;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public List<Option> getOptions() {
            return options;
        }

        public void setOptions(List<Option> options) {
            this.options = options;
        }

        public List<Keyword> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<Keyword> keywords) {
            this.keywords = keywords;
        }
    }

    // Nested Option class
    public static class Option {
        private String text;
        private Boolean isCorrect;

        // Getters and Setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Boolean getIsCorrect() {
            return isCorrect;
        }

        public void setIsCorrect(Boolean isCorrect) {
            this.isCorrect = isCorrect;
        }
    }

    // Nested Keyword class
    public static class Keyword {
        private String keyword;
        private Double weight;

        // Getters and Setters
        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public Double getWeight() {
            return weight;
        }

        public void setWeight(Double weight) {
            this.weight = weight;
        }
    }
}