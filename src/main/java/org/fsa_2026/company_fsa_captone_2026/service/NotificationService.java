package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.NotificationResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Notification;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;

    /**
     * Create a new notification for a recipient.
     */
    @Transactional
    public void createNotification(UUID recipientId, String type, String title, String message, UUID referenceId) {
        Account recipient = accountRepository.findById(recipientId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người nhận thông báo"));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Notification created: type={}, recipient={}", type, recipientId);
    }

    /**
     * Get all notifications for the current user.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public void markAsRead(String email, UUID notificationId) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy thông báo"));

        if (!notification.getRecipient().getId().equals(account.getId())) {
            throw new ApiException("FORBIDDEN", "Bạn không có quyền truy cập thông báo này");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Get unread notification count for the current user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        return notificationRepository.countByRecipientIdAndIsReadFalse(account.getId());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
