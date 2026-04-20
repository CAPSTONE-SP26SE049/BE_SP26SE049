package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.DailyChallenge;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyChallengeRepository extends BaseRepository<DailyChallenge, UUID> {
    Optional<DailyChallenge> findByChallengeDate(LocalDate date);
}
