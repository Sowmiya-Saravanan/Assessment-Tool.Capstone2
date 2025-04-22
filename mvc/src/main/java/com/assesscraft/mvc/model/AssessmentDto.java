package com.assesscraft.mvc.model;

import java.time.LocalDateTime;

public class AssessmentDto {
    private Long assessmentId;
    private String title;
    private String description;
    private String type; // e.g., "QUIZ", "TEST"
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String gradingMode; // e.g., "AUTOMATIC", "MANUAL"
    private String status; // e.g., "DRAFT", "ASSIGNED"
    private Boolean allowResumption;
    private Integer maxAttempts;
    private Boolean resultsPublished;
    private Long categoryId;
    private Long classId;

    // Getters and Setters
    public Long getAssessmentId() { return assessmentId; }
    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getGradingMode() { return gradingMode; }
    public void setGradingMode(String gradingMode) { this.gradingMode = gradingMode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getAllowResumption() { return allowResumption; }
    public void setAllowResumption(Boolean allowResumption) { this.allowResumption = allowResumption; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    public Boolean getResultsPublished() { return resultsPublished; }
    public void setResultsPublished(Boolean resultsPublished) { this.resultsPublished = resultsPublished; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
}