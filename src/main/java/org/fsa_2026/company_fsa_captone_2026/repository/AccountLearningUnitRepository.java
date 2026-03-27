package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountLearningUnitRepository extends JpaRepository<AccountLearningUnit, UUID> {

    List<AccountLearningUnit> findByAccountId(UUID accountId);

    Optional<AccountLearningUnit> findByAccountIdAndLearningUnitId(UUID accountId, UUID learningUnitId);

    boolean existsByAccountIdAndLearningUnitId(UUID accountId, UUID learningUnitId);

    @Query("""
            SELECT alu
            FROM AccountLearningUnit alu
            JOIN FETCH alu.learningUnit lu
            LEFT JOIN FETCH lu.parent
            WHERE alu.account.id = :accountId
            """)
    List<AccountLearningUnit> findByAccountIdWithLearningUnit(@Param("accountId") UUID accountId);
}
