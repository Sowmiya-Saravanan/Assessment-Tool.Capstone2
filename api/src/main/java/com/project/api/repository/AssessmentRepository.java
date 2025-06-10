package com.project.api.repository;


import com.project.api.model.Assessment;
import com.project.api.model.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByCreatedBy(User createdBy);



}
