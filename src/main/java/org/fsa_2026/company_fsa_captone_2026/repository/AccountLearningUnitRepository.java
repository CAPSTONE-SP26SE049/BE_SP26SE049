package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountLearningUnitRepository extends JpaRepository<AccountLearningUnit, UUID> {

    @Query("SELECT alu FROM AccountLearningUnit alu JOIN FETCH alu.learningUnit WHERE alu.account.id = :accountId")
    List<AccountLearningUnit> findByAccountIdWithLearningUnit(@org.springframework.data.repository.query.Param("accountId") UUID accountId);

    List<AccountLearningUnit> findByAccountId(UUID accountId);

    Optional<AccountLearningUnit> findByAccountIdAndLearningUnitId(UUID accountId, UUID learningUnitId);

    boolean existsByAccountIdAndLearningUnitId(UUID accountId, UUID learningUnitId);

    long countByAccountIdAndIsCompletedTrue(UUID accountId);

    @Query("SELECT AVG(alu.highestScore) FROM AccountLearningUnit alu WHERE alu.account.id = :accountId AND alu.isCompleted = true")
    Double findAverageScoreByAccountId(@org.springframework.data.repository.query.Param("accountId") UUID accountId);

    interface UserProgressProjection {
        UUID getAccountId();
        Long getCompletedCount();
        Double getAverageScore();
    }

    @Query("SELECT alu.account.id as accountId, COUNT(alu) as completedCount, AVG(alu.highestScore) as averageScore " +
           "FROM AccountLearningUnit alu WHERE alu.isCompleted = true AND alu.account.id IN :accountIds " +
           "GROUP BY alu.account.id")
    List<UserProgressProjection> findProgressByAccountIds(@org.springframework.data.repository.query.Param("accountIds") List<UUID> accountIds);
}
