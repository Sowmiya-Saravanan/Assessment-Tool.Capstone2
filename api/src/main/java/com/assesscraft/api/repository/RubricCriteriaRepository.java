package com.assesscraft.api.repository;

import com.assesscraft.api.model.RubricCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RubricCriteriaRepository extends JpaRepository<RubricCriteria, Long> {
}