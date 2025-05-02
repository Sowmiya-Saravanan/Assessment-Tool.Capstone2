package com.assesscraft.mvc.model;

public class ClassEntity {
    private String className;
    private String description;

    public ClassEntity() {
    }

    public ClassEntity(String className, String description) {
        this.className = className;
        this.description = description;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ClassEntity{" +
                "className='" + className + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}