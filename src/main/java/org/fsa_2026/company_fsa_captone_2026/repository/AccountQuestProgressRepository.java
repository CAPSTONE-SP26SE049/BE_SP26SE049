package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.AccountQuestProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountQuestProgressRepository extends JpaRepository<AccountQuestProgress, UUID> {
    List<AccountQuestProgress> findByAccountIdAndQuestDate(UUID accountId, LocalDate questDate);
    Optional<AccountQuestProgress> findByAccountIdAndQuestIdAndQuestDate(UUID accountId, UUID questId, LocalDate questDate);
}
