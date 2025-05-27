package com.project.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.project.api.model.Class;
import com.project.api.model.User;

public interface ClassRepository extends JpaRepository<Class, Long> {

    Optional<Class> findByClassCode(String classCode);
    List<Class> findByCreatedBy(User createdBy);
}
