package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    List<StudySession> findByAccountId(UUID accountId);

    List<StudySession> findByAccountIdAndSessionType(UUID accountId, String sessionType);

    List<StudySession> findBySessionType(String sessionType);

    @Query("SELECT COUNT(DISTINCT s.account.id) FROM StudySession s WHERE s.startedAt > :after")
    long countDistinctAccountByStartedAtAfter(@Param("after") Instant after);

    @Query("SELECT s.startedAt, s.endedAt FROM StudySession s WHERE s.endedAt IS NOT NULL")
    List<Object[]> findStartedAndEndedAtForFinishedSessions();
}
