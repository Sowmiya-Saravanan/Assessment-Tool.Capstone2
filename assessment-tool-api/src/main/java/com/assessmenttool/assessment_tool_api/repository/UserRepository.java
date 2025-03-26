package com.assessmenttool.assessment_tool_api.repository;

import com.assessmenttool.assessment_tool_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByExternalId(String externalId);

    Optional<User> findByEmail(String email);
}