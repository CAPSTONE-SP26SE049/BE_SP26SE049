package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ErrorTagRepository extends JpaRepository<ErrorTag, UUID> {
    Optional<ErrorTag> findByTagCode(String tagCode);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT e FROM ErrorTag e " +
            "LEFT JOIN FETCH e.placementRules r " +
            "LEFT JOIN FETCH r.targetDialect")
    java.util.List<ErrorTag> findAllWithRegions();
}
