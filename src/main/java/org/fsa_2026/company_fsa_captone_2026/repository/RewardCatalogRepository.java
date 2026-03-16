package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * RewardCatalog Repository - thay thế AchievementRepository
 */
@Repository
public interface RewardCatalogRepository extends JpaRepository<RewardCatalog, UUID> {

    List<RewardCatalog> findByRewardType(RewardType rewardType);

    List<RewardCatalog> findByRewardTypeAndIsActiveTrue(RewardType rewardType);
}
