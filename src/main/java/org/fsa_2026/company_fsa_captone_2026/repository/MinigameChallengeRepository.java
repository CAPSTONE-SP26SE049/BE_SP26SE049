package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.MinigameChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MinigameChallengeRepository extends JpaRepository<MinigameChallenge, UUID> {
    List<MinigameChallenge> findByGameType(String gameType);
    List<MinigameChallenge> findByGameTypeAndPairType(String gameType, String pairType);
    boolean existsByGameTypeAndPairType(String gameType, String pairType);
}
