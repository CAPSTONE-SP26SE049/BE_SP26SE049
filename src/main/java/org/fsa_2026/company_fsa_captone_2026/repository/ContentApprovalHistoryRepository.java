package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.ContentApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ContentApprovalHistoryRepository extends JpaRepository<ContentApprovalHistory, UUID> {
    List<ContentApprovalHistory> findByContentIdOrderByCreatedAtDesc(UUID contentId);
}
