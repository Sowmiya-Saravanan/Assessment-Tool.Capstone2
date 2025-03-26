package com.assessmenttool.assessment_tool_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.assessmenttool.assessment_tool_api.model.Classes;

public interface ClassesRepository extends JpaRepository<Classes, Long> {
    List<Classes> findByEducatorExternalId(String externalId);
}