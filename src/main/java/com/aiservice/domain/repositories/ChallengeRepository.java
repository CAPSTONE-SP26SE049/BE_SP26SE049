package com.aiservice.domain.repositories;

import com.aiservice.domain.entities.Challenge;
import com.aiservice.domain.entities.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByLevel(Level level);
    List<Challenge> findByType(Challenge.ChallengeType type);
}

