package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Level Repository
 */
@Repository
public interface LevelRepository extends JpaRepository<Level, UUID> {
    List<Level> findByDialectIdOrderByLevelOrderAsc(UUID dialectId);

    boolean existsByErrorTagId(UUID errorTagId);
}
