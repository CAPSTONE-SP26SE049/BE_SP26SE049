package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.DailyAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Daily Analytics Repository
 */
@Repository
public interface DailyAnalyticsRepository extends JpaRepository<DailyAnalytics, UUID> {
    Optional<DailyAnalytics> findByRecordDate(LocalDate recordDate);
}
