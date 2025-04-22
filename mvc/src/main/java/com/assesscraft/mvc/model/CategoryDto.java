// CategoryDto.java
package com.assesscraft.mvc.model;

public class CategoryDto {
    private Long categoryId;
    private String name;
    private String description;

    // Getters and setters
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}