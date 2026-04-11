package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
            UUID sender1,
            UUID recipient1,
            UUID sender2,
            UUID recipient2
    );

    @Modifying
    @Query("UPDATE ChatMessage m SET m.status = 'READ' " +
            "WHERE m.senderId = :senderId AND m.recipientId = :recipientId AND m.status != 'READ'")
    int markMessagesAsRead(@Param("senderId") UUID senderId, @Param("recipientId") UUID recipientId);

    @Query("SELECT c.senderId, COUNT(c) FROM ChatMessage c " +
            "WHERE c.recipientId = :userId AND c.status <> org.fsa_2026.company_fsa_captone_2026.entity.enums.MessageStatus.READ " +
            "GROUP BY c.senderId")
    List<Object[]> countUnreadMessagesGroupedBySender(@Param("userId") UUID userId);
}

