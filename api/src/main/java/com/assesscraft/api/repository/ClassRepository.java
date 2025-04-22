package com.assesscraft.api.repository;

import com.assesscraft.api.model.Class;
import com.assesscraft.api.model.ClassStatus;
import com.assesscraft.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassRepository extends JpaRepository<Class, Long> {
    @Query("SELECT c FROM Class c JOIN FETCH c.educator WHERE c.educator = :educator AND c.status = :status")
    List<Class> findByEducatorAndStatus(@Param("educator") User educator, @Param("status") ClassStatus status);

    List<Class> findByEducator(User educator); // Keep this if needed
}