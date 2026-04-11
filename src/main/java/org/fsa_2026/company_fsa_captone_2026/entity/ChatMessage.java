package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.MessageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ChatMessage entity for real-time chat.
 *
 * Stores minimal message data for 1-1 chat between two accounts.
 */
@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;
}

