package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizChallengeItemRepository extends JpaRepository<QuizChallengeItem, UUID> {
    long countByQuizId(UUID quizId);

    List<QuizChallengeItem> findByQuizIdOrderByOrderIndex(UUID quizId);
    List<QuizChallengeItem> findByChallengeId(UUID challengeId);
    void deleteByChallengeBankId(UUID challengeBankId);
    void deleteByChallengeId(UUID challengeId);
    void deleteByQuizIdAndChallengeBankId(UUID quizId, UUID challengeBankId);
    void deleteByQuizIdAndChallengeId(UUID quizId, UUID challengeId);

    void deleteByQuizId(UUID quizId);
}
