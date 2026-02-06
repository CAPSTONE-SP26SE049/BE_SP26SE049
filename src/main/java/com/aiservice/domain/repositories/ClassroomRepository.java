package com.aiservice.domain.repositories;

import com.aiservice.domain.entities.Classroom;
import com.aiservice.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Optional<Classroom> findByCode(String code);
    List<Classroom> findByEducator(User educator);
}

