package com.assesscraft.api.repository;

import com.assesscraft.api.model.QuestionKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionKeywordRepository extends JpaRepository<QuestionKeyword, Long> {
}