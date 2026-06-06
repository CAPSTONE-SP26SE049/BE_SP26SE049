package org.fsa_2026.company_fsa_captone_2026.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ChatMessageDto;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Friendship;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.FriendshipRepository;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final AccountRepository accountRepository;
    private final FriendshipRepository friendshipRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    // Track active user sessions in memory
    private static final Map<UUID, Set<String>> activeSessions = new ConcurrentHashMap<>();

    public static boolean isUserOnline(UUID userId) {
        Set<String> sessions = activeSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        try {
            UUID userId = UUID.fromString(principal.getName());
            String sessionId = headerAccessor.getSessionId();

            activeSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            log.info("User connected: {} (Session: {})", userId, sessionId);

            // Update database lastActiveAt (though they are online, it helps keep it fresh)
            updateLastActive(userId);

            // Broadcast online status to friends
            broadcastStatusToFriends(userId, true, Instant.now());
        } catch (IllegalArgumentException e) {
            log.warn("Principal name is not a valid UUID: {}", principal.getName());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        try {
            UUID userId = UUID.fromString(principal.getName());
            String sessionId = headerAccessor.getSessionId();

            Set<String> sessions = activeSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    activeSessions.remove(userId);
                    log.info("User went offline: {}", userId);

                    // Update lastActiveAt in DB
                    Instant now = Instant.now();
                    updateLastActive(userId, now);

                    // Broadcast offline status to friends
                    broadcastStatusToFriends(userId, false, now);
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Principal name is not a valid UUID: {}", principal.getName());
        }
    }

    private void updateLastActive(UUID userId) {
        updateLastActive(userId, Instant.now());
    }

    private void updateLastActive(UUID userId, Instant time) {
        try {
            accountRepository.findById(userId).ifPresent(account -> {
                account.setLastActiveAt(time);
                accountRepository.save(account);
            });
        } catch (Exception ex) {
            log.error("Failed to update lastActiveAt for user {}", userId, ex);
        }
    }

    private void broadcastStatusToFriends(UUID userId, boolean isOnline, Instant lastActiveAt) {
        try {
            List<Friendship> friendships = friendshipRepository.findAllAcceptedByAccount(userId);
            for (Friendship friendship : friendships) {
                UUID friendId = friendship.getRequester().getId().equals(userId)
                        ? friendship.getAddressee().getId()
                        : friendship.getRequester().getId();

                ChatMessageDto statusMsg = new ChatMessageDto();
                statusMsg.setSenderId(userId);
                statusMsg.setRecipientId(friendId);
                statusMsg.setType("USER_STATUS");
                statusMsg.setContent(isOnline ? "ONLINE" : "OFFLINE");
                statusMsg.setTimestamp(LocalDateTime.ofInstant(lastActiveAt, ZoneId.systemDefault()));
                
                // Status field can hold string representation of boolean for UI convenience
                statusMsg.setStatus(String.valueOf(isOnline));

                simpMessagingTemplate.convertAndSend("/topic/chat/" + friendId, statusMsg);
            }
        } catch (Exception ex) {
            log.error("Failed to broadcast status for user {}", userId, ex);
        }
    }
}
