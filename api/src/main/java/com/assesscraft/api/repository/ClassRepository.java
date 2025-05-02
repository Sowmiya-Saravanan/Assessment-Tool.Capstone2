package com.assesscraft.api.repository;

import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.ClassStatus;
import com.assesscraft.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {
    // Find classes by creator (mapped to createdBy) and status
    List<Class> findByCreatedByAndStatus(User createdBy, ClassStatus status);

    // Find a class by ID
    Optional<Class> findById(Long id);

    // Find all classes by a list of IDs
    List<Class> findAllById(Iterable<Long> ids);

    // Find classes by creator (mapped to createdBy)
    List<Class> findByCreatedBy(User createdBy);

    // Count distinct students across all classes created by the educator
    @Query("SELECT COUNT(DISTINCT s) FROM Class c JOIN c.students s WHERE c.createdBy = :user")
    long countStudentsByCreatedBy(User user);
}