package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.SessionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionDetailRepository extends JpaRepository<SessionDetail, UUID> {

    @Query("SELECT sd FROM SessionDetail sd JOIN FETCH sd.session JOIN FETCH sd.contentItem WHERE sd.session.id = :sessionId")
    List<SessionDetail> findBySessionId(@Param("sessionId") UUID sessionId);

    List<SessionDetail> findByContentItemId(UUID contentItemId);

    @Query("SELECT sd FROM SessionDetail sd JOIN FETCH sd.session JOIN FETCH sd.contentItem WHERE sd.session.account.id = :accountId ORDER BY sd.createdAt DESC")
    List<SessionDetail> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") UUID accountId);

    Optional<SessionDetail> findFirstBySession_Account_IdOrderByCreatedAtDesc(UUID accountId);

    @Query("SELECT sd FROM SessionDetail sd JOIN FETCH sd.session s JOIN FETCH s.account JOIN FETCH sd.contentItem ORDER BY sd.createdAt DESC")
    List<SessionDetail> findTop10ByOrderByCreatedAtDesc();
}
