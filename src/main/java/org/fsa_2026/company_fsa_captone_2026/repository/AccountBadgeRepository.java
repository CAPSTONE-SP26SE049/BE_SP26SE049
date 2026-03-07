package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.AccountBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * AccountBadge Repository
 */
@Repository
public interface AccountBadgeRepository extends JpaRepository<AccountBadge, AccountBadge.AccountBadgeId> {
    List<AccountBadge> findByAccountId(UUID accountId);
}
