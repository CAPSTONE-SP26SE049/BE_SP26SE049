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

    List<AccountReward> findByAccountId(UUID accountId);

    @Query("SELECT ar FROM AccountReward ar JOIN ar.rewardCatalog rc WHERE ar.account.id = :accountId AND rc.rewardType = :rewardType")
    List<AccountReward> findByAccountIdAndRewardType(@Param("accountId") UUID accountId,
                                                     @Param("rewardType") RewardType rewardType);

    Optional<AccountReward> findByAccountIdAndRewardCatalogId(UUID accountId, UUID rewardCatalogId);
}
