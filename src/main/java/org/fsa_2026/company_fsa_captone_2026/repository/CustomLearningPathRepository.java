package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.CustomLearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomLearningPathRepository extends JpaRepository<CustomLearningPath, UUID> {
    List<CustomLearningPath> findByStudentIdAndIsActiveTrue(UUID studentId);

    List<CustomLearningPath> findByEducatorId(UUID educatorId);

    Optional<CustomLearningPath> findFirstByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(UUID studentId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.student.id FROM CustomLearningPath p WHERE p.isActive = true")
    List<UUID> findAllStudentIdsWithActivePath();

    List<CustomLearningPath> findByIsActiveTrue();
}
