package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.AccountReward;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AccountReward Repository - thay thế AccountAchievementRepository
 */
@Repository
public interface AccountRewardRepository extends JpaRepository<AccountReward, UUID> {

    @Query("SELECT ar FROM AccountReward ar JOIN FETCH ar.rewardCatalog WHERE ar.account.id = :accountId")
    List<AccountReward> findByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT ar FROM AccountReward ar JOIN FETCH ar.rewardCatalog rc WHERE ar.account.id = :accountId AND rc.rewardType = :rewardType")
    List<AccountReward> findByAccountIdAndRewardType(@Param("accountId") UUID accountId,
                                                     @Param("rewardType") RewardType rewardType);

    @Query("SELECT ar FROM AccountReward ar JOIN FETCH ar.rewardCatalog WHERE ar.account.id = :accountId AND UPPER(ar.status) = UPPER(:status)")
    List<AccountReward> findByAccountIdAndStatusIgnoreCase(@Param("accountId") UUID accountId, @Param("status") String status);

    Optional<AccountReward> findByAccountIdAndRewardCatalogId(UUID accountId, UUID rewardCatalogId);

    boolean existsByRewardCatalogId(UUID rewardCatalogId);
}
