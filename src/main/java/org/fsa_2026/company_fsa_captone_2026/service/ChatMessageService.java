package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ChatMessageDto;
import org.fsa_2026.company_fsa_captone_2026.entity.ChatMessage;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.MessageStatus;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    @Transactional
    public ChatMessageDto saveMessage(ChatMessageDto dto) {
        ChatMessage entity = ChatMessage.builder()
                .senderId(dto.getSenderId())
                .recipientId(dto.getRecipientId())
                .content(dto.getContent())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now())
                .status(dto.getStatus() != null ? MessageStatus.valueOf(dto.getStatus()) : MessageStatus.SENT)
                .build();

        ChatMessage saved = chatMessageRepository.save(entity);

        // Create in-app notification for new message
        try {
            Account sender = accountRepository.findById(dto.getSenderId()).orElse(null);
            String senderName = sender != null && sender.getFullName() != null ? sender.getFullName() : "Một học viên";
            notificationService.createNotification(
                    dto.getRecipientId(),
                    "MESSAGE",
                    "Tin nhắn mới từ " + senderName,
                    dto.getContent(),
                    dto.getSenderId()
            );
        } catch (Exception ex) {
            // Log and ignore to prevent blocking chat message if notification fails
        }

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Long> getUnreadCounts(UUID userId) {
        List<Object[]> results = chatMessageRepository.countUnreadMessagesGroupedBySender(userId);
        Map<UUID, Long> unreadMap = new HashMap<>();
        for (Object[] result : results) {
            Number count = (Number) result[1];
            unreadMap.put((UUID) result[0], count.longValue());
        }
        return unreadMap;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatHistory(UUID user1, UUID user2) {
        return chatMessageRepository
                .findFullConversation(user1, user2)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public int markAsRead(UUID senderId, UUID recipientId) {
        return chatMessageRepository.markMessagesAsRead(senderId, recipientId);
    }

    private ChatMessageDto toDto(ChatMessage entity) {
        return new ChatMessageDto(
                entity.getId(),
                entity.getSenderId(),
                entity.getRecipientId(),
                entity.getContent(),
                entity.getTimestamp(),
                entity.getStatus() != null ? entity.getStatus().name() : null);
    }

    /**
     * Resolve current user UUID from Authentication/Principal name.
     * HTTP requests currently store email as principal, while WS stores UUID (sub).
     */
    @Transactional(readOnly = true)
    public UUID resolveUserId(String principalName) {
        if (!StringUtils.hasText(principalName)) {
            throw new ApiException("UNAUTHORIZED", "Chưa xác thực người dùng");
        }

        try {
            return UUID.fromString(principalName);
        } catch (Exception ignored) {
            // principalName is probably email (HTTP)
        }

        Account account = accountRepository.findByEmail(principalName)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));
        return account.getId();
    }
}
