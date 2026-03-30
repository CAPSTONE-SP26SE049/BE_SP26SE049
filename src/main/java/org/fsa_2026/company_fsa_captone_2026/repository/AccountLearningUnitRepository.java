package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountLearningUnitRepository extends JpaRepository<AccountLearningUnit, UUID> {

    List<AccountLearningUnit> findByAccountId(UUID accountId);

    Optional<AccountLearningUnit> findByAccountIdAndLearningUnitId(UUID accountId, UUID learningUnitId);

    boolean existsByAccountIdAndLearningUnitId(UUID accountId, UUID learningUnitId);

    void deleteByLearningUnitId(UUID learningUnitId);

    /**
     * Gộp tiến độ quiz (LearningUnit type QUIZ) theo parent level.
     * Đạt: is_completed hoặc highest_score &gt;= 80 (80% — khớp GameplayService).
     */
    @Query(value = """
            SELECT lu.parent_id AS level_id,
                   COUNT(DISTINCT alu.account_id) AS learner_count,
                   SUM(CASE WHEN alu.is_completed = true
                       OR (alu.highest_score IS NOT NULL AND alu.highest_score >= 80.0)
                       THEN 1 ELSE 0 END) AS passed_count,
                   COUNT(*) AS total_progress
            FROM account_learning_unit alu
            INNER JOIN learning_unit lu ON alu.learning_unit_id = lu.id
            WHERE UPPER(lu.type) = 'QUIZ' AND lu.parent_id IS NOT NULL
            GROUP BY lu.parent_id
            """, nativeQuery = true)
    List<Object[]> aggregateQuizProgressByLevelParent();
}
