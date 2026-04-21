package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.CustomPathProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomPathProgressRepository extends JpaRepository<CustomPathProgress, UUID> {
    List<CustomPathProgress> findByCustomPathId(UUID pathId);

    Optional<CustomPathProgress> findByCustomPathIdAndLearningUnitId(UUID pathId, UUID learningUnitId);
}
