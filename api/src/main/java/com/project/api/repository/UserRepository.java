package com.project.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import com.project.api.model.User;
import com.project.api.model.UserRole;


public interface UserRepository extends JpaRepository<User,Long>{

    Optional<User> findByUserId(Long userId);

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

}
