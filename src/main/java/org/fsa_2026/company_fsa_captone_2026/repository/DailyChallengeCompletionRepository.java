package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.DailyChallengeCompletion;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyChallengeCompletionRepository extends BaseRepository<DailyChallengeCompletion, UUID> {
    Optional<DailyChallengeCompletion> findByAccountIdAndDailyChallengeId(UUID accountId, UUID dailyChallengeId);
    boolean existsByAccountIdAndDailyChallengeId(UUID accountId, UUID dailyChallengeId);
}
