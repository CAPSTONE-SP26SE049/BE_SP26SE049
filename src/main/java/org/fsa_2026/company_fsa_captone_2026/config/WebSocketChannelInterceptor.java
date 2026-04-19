package org.fsa_2026.company_fsa_captone_2026.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.UUID;

/**
 * Intercepts STOMP CONNECT to authenticate via JWT and set Principal name to UUID (sub).
 *
 * This solves the mismatch between HTTP principal (email) and user destinations that require UUID:
 * /user/{UUID}/queue/messages
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION);
            String token = null;

            if (StringUtils.hasText(authHeader)) {
                token = authHeader.startsWith(BEARER) ? authHeader.substring(BEARER.length()) : authHeader;
            }

            if (!StringUtils.hasText(token)) {
                log.warn("WS CONNECT missing Authorization header");
                return message;
            }

            try {
                if (!jwtTokenProvider.validateToken(token)) {
                    log.warn("WS CONNECT invalid JWT");
                    return message;
                }

                String userIdStr = jwtTokenProvider.getUserIdFromToken(token);
                if (!StringUtils.hasText(userIdStr)) {
                    log.warn("WS CONNECT missing sub (userId) in JWT");
                    return message;
                }

                UUID userId = UUID.fromString(userIdStr);
                Principal principal = () -> userId.toString();
                accessor.setUser(principal);
                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("WS_PRINCIPAL", principal);
                }
            } catch (Exception ex) {
                log.error("WS CONNECT error while parsing JWT", ex);
            }
        } else {
            // Attempt to restore user from session for subsequent SEND/SUBSCRIBE frames
            if (accessor.getSessionAttributes() != null) {
                Object p = accessor.getSessionAttributes().get("WS_PRINCIPAL");
                if (p instanceof Principal) {
                    accessor.setUser((Principal) p);
                }
            }
        }

        return message;
    }
}

