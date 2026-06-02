package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.NotificationResponse;
import org.fsa_2026.company_fsa_captone_2026.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notification Controller
 * Manages in-app notifications for the current user.
 * Base: /api/v1/notifications
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification API endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications",
            description = "Lấy tất cả thông báo của người dùng hiện tại",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(Authentication authentication) {
        log.info("User {} getting notifications", authentication.getName());
        List<NotificationResponse> notifications = notificationService.getNotifications(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", notifications));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read",
            description = "Đánh dấu thông báo đã đọc",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication) {
        log.info("User {} marking notification {} as read", authentication.getName(), notificationId);
        notificationService.markAsRead(authentication.getName(), notificationId);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã đọc thành công", null));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read",
            description = "Đánh dấu tất cả thông báo của người dùng hiện tại là đã đọc",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        log.info("User {} marking all notifications as read", authentication.getName());
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu tất cả đã đọc thành công", null));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count",
            description = "Lấy số lượng thông báo chưa đọc",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        log.info("User {} getting unread count", authentication.getName());
        long count = notificationService.getUnreadCount(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy số thông báo chưa đọc thành công",
                Map.of("unreadCount", count)));
    }
}
