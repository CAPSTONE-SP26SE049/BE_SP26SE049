package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.SessionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionDetailRepository extends JpaRepository<SessionDetail, UUID> {

    List<SessionDetail> findBySessionId(UUID sessionId);

    List<SessionDetail> findByContentItemId(UUID contentItemId);

    @Query("SELECT sd FROM SessionDetail sd WHERE sd.session.account.id = :accountId ORDER BY sd.createdAt DESC")
    List<SessionDetail> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") UUID accountId);

    boolean existsByContentItemIdAndSessionAccountIdAndIsPassed(UUID contentItemId, UUID accountId, Boolean isPassed);
}
