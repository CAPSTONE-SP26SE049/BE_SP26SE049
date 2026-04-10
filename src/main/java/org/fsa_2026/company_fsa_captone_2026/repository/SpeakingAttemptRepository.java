package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.SpeakingAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpeakingAttemptRepository extends JpaRepository<SpeakingAttempt, UUID> {

    /** Admin: lấy tất cả attempts, có thể lọc theo dialect */
    @Query("SELECT sa FROM SpeakingAttempt sa WHERE " +
           "(:dialect IS NULL OR sa.dialect = :dialect) AND " +
           "sa.consentGiven = true " +
           "ORDER BY sa.createdAt DESC")
    Page<SpeakingAttempt> findAllByDialect(
            @Param("dialect") String dialect,
            Pageable pageable);

    /** Tổng số attempts theo dialect */
    long countByDialectAndConsentGivenTrue(String dialect);

    /** Đếm tổng có consent */
    long countByConsentGivenTrue();
}
