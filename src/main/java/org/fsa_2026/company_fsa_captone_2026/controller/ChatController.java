package org.fsa_2026.company_fsa_captone_2026.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ChatMessageDto;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.MessageStatus;
import org.fsa_2026.company_fsa_captone_2026.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @GetMapping(Constants.API_PREFIX + "/chat/history/{friendId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            @PathVariable UUID friendId,
            Authentication authentication
    ) {
        UUID currentUserId = chatMessageService.resolveUserId(authentication != null ? authentication.getName() : null);
        return ResponseEntity.ok(chatMessageService.getChatHistory(currentUserId, friendId));
    }

    @GetMapping(Constants.API_PREFIX + "/chat/unread")
    @ResponseBody
    public ResponseEntity<Map<UUID, Long>> getUnreadCounts(Authentication authentication) {
        UUID currentUserId = chatMessageService.resolveUserId(authentication != null ? authentication.getName() : null);
        return ResponseEntity.ok(chatMessageService.getUnreadCounts(currentUserId));
    }

    @PostMapping(Constants.API_PREFIX + "/chat/send")
    @ResponseBody
    public ResponseEntity<ChatMessageDto> sendChatMessage(
            @RequestBody ChatMessageDto incoming,
            Authentication authentication
    ) {
        UUID senderId = chatMessageService.resolveUserId(authentication != null ? authentication.getName() : null);
        if (incoming == null || incoming.getRecipientId() == null) {
            return ResponseEntity.badRequest().build();
        }

        ChatMessageDto dto = new ChatMessageDto(
                incoming.getId(),
                senderId,
                incoming.getRecipientId(),
                incoming.getContent(),
                incoming.getTimestamp() != null ? incoming.getTimestamp() : LocalDateTime.now(),
                incoming.getStatus() != null ? incoming.getStatus() : MessageStatus.SENT.name()
        );

        ChatMessageDto savedDto = chatMessageService.saveMessage(dto);

        String destination = "/topic/chat/" + savedDto.getRecipientId();
        simpMessagingTemplate.convertAndSend(destination, savedDto);
        
        return ResponseEntity.ok(savedDto);
    }

    @MessageMapping("/chat")
    public void handleChatMessage(@Payload ChatMessageDto incoming, Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.warn("WS chat message missing principal");
            return;
        }

        UUID senderId;
        try {
            senderId = UUID.fromString(principal.getName());
        } catch (Exception ex) {
            log.warn("WS principal is not UUID: {}", principal.getName());
            return;
        }

        if (incoming == null || incoming.getRecipientId() == null) return;

        // Trust senderId from Principal (UUID from JWT sub) to avoid spoofing
        ChatMessageDto dto = new ChatMessageDto(
                incoming.getId(),
                senderId,
                incoming.getRecipientId(),
                incoming.getContent(),
                incoming.getTimestamp() != null ? incoming.getTimestamp() : LocalDateTime.now(),
                incoming.getStatus() != null ? incoming.getStatus() : MessageStatus.SENT.name()
        );

        ChatMessageDto savedDto = chatMessageService.saveMessage(dto);

        // Thay vì dùng convertAndSendToUser, ta gửi thẳng vào một topic đích danh
        String destination = "/topic/chat/" + savedDto.getRecipientId();
        simpMessagingTemplate.convertAndSend(destination, savedDto);
    }

    @MessageMapping("/chat.read")
    public void processReadReceipt(@Payload ChatMessageDto payload) {
        if (payload == null || payload.getSenderId() == null || payload.getRecipientId() == null) return;

        // Payload: senderId là người MỚI ĐỌC (recipient của tin nhắn gốc),
        // recipientId là NGƯỜI GỬI TIN NHẮN GỐC
        chatMessageService.markAsRead(payload.getRecipientId(), payload.getSenderId());

        // Tạo một DTO thông báo đã xem
        ChatMessageDto receipt = new ChatMessageDto();
        receipt.setSenderId(payload.getSenderId());
        receipt.setRecipientId(payload.getRecipientId());
        receipt.setType("READ_RECEIPT");

        // Bắn thông báo về lại cho người gửi tin nhắn gốc
        simpMessagingTemplate.convertAndSend("/topic/chat/" + payload.getRecipientId(), receipt);
    }
}

