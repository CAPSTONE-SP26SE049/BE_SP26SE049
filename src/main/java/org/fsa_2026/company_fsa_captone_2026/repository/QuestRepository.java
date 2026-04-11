package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestRepository extends JpaRepository<Quest, UUID> {
    List<Quest> findByIsActiveTrueAndStudentIdIsNull();
    List<Quest> findByIsActiveTrueAndStudentId(UUID studentId);
    List<Quest> findByQuestTypeAndIsActiveTrue(String questType);
}
