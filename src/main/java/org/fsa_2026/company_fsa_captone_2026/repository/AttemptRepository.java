package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Attempt Repository
 */
@Repository
public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
    List<Attempt> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<Attempt> findBySessionId(UUID sessionId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Attempt a WHERE a.account.id IN (SELECT cm.student.id FROM ClassroomMember cm WHERE cm.classroom.id = :classroomId)")
    List<Attempt> findByClassroomId(@org.springframework.data.repository.query.Param("classroomId") UUID classroomId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(a.scoreOverall) FROM Attempt a")
    Double findAverageScore();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.account.id) FROM Attempt a WHERE a.createdAt >= :startDate")
    long countDistinctAccountByCreatedAtAfter(
            @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate);
}
