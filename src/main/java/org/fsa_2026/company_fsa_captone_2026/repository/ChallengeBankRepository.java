package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeBankRepository extends JpaRepository<ChallengeBank, UUID> {

    boolean existsByContentTextAndSkillType(String contentText, SkillType skillType);

    List<ChallengeBank> findBySkillTypeAndRegionAndLevelId(SkillType skillType, String region, UUID levelId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM ChallengeBank c WHERE c.skillType = :skillType AND c.region = :region AND (c.levelId = :levelId OR c.levelId IS NULL)")
    List<ChallengeBank> findFiltered(SkillType skillType, String region, UUID levelId);

    List<ChallengeBank> findBySkillType(SkillType skillType);
}
