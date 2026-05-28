package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.DailyChallengeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DailyChallengeAttemptRepository extends JpaRepository<DailyChallengeAttempt, UUID> {

    /**
     * Tìm các lượt nộp bài thành công (isCorrect = true) của người dùng kể từ một mốc thời gian.
     */
    List<DailyChallengeAttempt> findByAccountIdAndIsCorrectTrueAndCreatedAtGreaterThanEqual(UUID accountId, Instant afterDate);
}
