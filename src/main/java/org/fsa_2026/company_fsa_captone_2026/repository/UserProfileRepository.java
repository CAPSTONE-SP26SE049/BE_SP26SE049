package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UserProfile Repository
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByAccountId(UUID accountId);

    List<UserProfile> findTop10ByOrderByTotalExperienceDesc();

    List<UserProfile> findTop10ByAccountRegionOrderByTotalExperienceDesc(String region);
}
