package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.LessonPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonPlanRepository extends JpaRepository<LessonPlan, UUID> {
    List<LessonPlan> findByEducatorId(UUID educatorId);
}
