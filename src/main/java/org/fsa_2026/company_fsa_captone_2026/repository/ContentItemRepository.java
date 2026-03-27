package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {

    List<ContentItem> findByType(String type);

    List<ContentItem> findByStatus(String status);

    List<ContentItem> findByLearningUnitId(UUID learningUnitId);

    List<ContentItem> findByLearningUnitIdAndType(UUID learningUnitId, String type);
    
    List<ContentItem> findByLearningUnitIdInAndType(List<UUID> learningUnitIds, String type);

    List<ContentItem> findByTypeAndStatus(String type, String status);

    List<ContentItem> findByTypeAndCreatedByOrderByCreatedAtDesc(String type, String createdBy);

    void deleteByLearningUnitId(UUID learningUnitId);
}
