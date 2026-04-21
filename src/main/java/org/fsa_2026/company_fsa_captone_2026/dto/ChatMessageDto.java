package org.fsa_2026.company_fsa_captone_2026.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for chat messages over STOMP and REST (if needed).
 */
public class ChatMessageDto {
    private UUID id;
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private LocalDateTime timestamp;
    private String status;
    private String type;

    public ChatMessageDto() {
    }

    public ChatMessageDto(UUID id, UUID senderId, UUID recipientId, String content, LocalDateTime timestamp, String status) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.timestamp = timestamp;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

