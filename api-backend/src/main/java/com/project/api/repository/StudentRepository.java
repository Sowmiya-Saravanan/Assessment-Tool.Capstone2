package com.project.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.api.model.Student;
import com.project.api.model.User;

public interface StudentRepository extends JpaRepository<Student,Long>{

    Optional<Student> findByUser(User user);
}
