package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.AttemptPhonemeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * AttemptPhonemeFeedback Repository
 */
@Repository
public interface AttemptPhonemeFeedbackRepository extends JpaRepository<AttemptPhonemeFeedback, UUID> {
    List<AttemptPhonemeFeedback> findByAttemptIdOrderBySequenceOrderAsc(UUID attemptId);

    List<AttemptPhonemeFeedback> findByAttemptIdIn(List<UUID> attemptIds);

    @Query("SELECT f FROM AttemptPhonemeFeedback f WHERE f.attempt.account.id = :studentId")
    List<AttemptPhonemeFeedback> findByStudentId(UUID studentId);
}
