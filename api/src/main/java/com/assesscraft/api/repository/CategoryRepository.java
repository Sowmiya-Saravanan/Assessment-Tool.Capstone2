package com.assesscraft.api.repository;

import com.assesscraft.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Additional custom queries can be added here if needed
    // For example:
    // List<Category> findByNameContainingIgnoreCase(String name);
    // Optional<Category> findByName(String name);
}