package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.FriendPublicProfileResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.FriendRequestDTO;
import org.fsa_2026.company_fsa_captone_2026.dto.FriendSearchResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.FriendshipResponse;
import org.fsa_2026.company_fsa_captone_2026.service.FriendshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Friendship Controller
 * Manages friend requests, friend list, search, and blocking.
 * Base: /api/v1/friends
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/friends")
@RequiredArgsConstructor
@Tag(name = "Friends", description = "Friend system API endpoints")
public class FriendshipController {

    private final FriendshipService friendshipService;

    // ─── Send Friend Request ────────────────────────────────────────

    @PostMapping("/request")
    @Operation(summary = "Send friend request",
            description = "Gửi lời mời kết bạn đến người dùng khác",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendFriendRequest(
            @Valid @RequestBody FriendRequestDTO request,
            Authentication authentication) {
        log.info("User {} sending friend request to {}", authentication.getName(), request.getAddresseeId());
        FriendshipResponse response = friendshipService.sendRequest(authentication.getName(), request.getAddresseeId());
        return ResponseEntity.ok(ApiResponse.success("Gửi lời mời kết bạn thành công", response));
    }

    // ─── Accept Request ─────────────────────────────────────────────

    @PutMapping("/{friendshipId}/accept")
    @Operation(summary = "Accept friend request",
            description = "Chấp nhận lời mời kết bạn",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptRequest(
            @PathVariable UUID friendshipId,
            Authentication authentication) {
        log.info("User {} accepting friend request {}", authentication.getName(), friendshipId);
        FriendshipResponse response = friendshipService.acceptRequest(authentication.getName(), friendshipId);
        return ResponseEntity.ok(ApiResponse.success("Chấp nhận lời mời kết bạn thành công", response));
    }

    // ─── Decline Request ────────────────────────────────────────────

    @PutMapping("/{friendshipId}/decline")
    @Operation(summary = "Decline friend request",
            description = "Từ chối lời mời kết bạn",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> declineRequest(
            @PathVariable UUID friendshipId,
            Authentication authentication) {
        log.info("User {} declining friend request {}", authentication.getName(), friendshipId);
        friendshipService.declineRequest(authentication.getName(), friendshipId);
        return ResponseEntity.ok(ApiResponse.success("Từ chối lời mời kết bạn thành công", null));
    }

    // ─── Remove Friend / Cancel Request ─────────────────────────────

    @DeleteMapping("/{friendshipId}")
    @Operation(summary = "Remove friend or cancel request",
            description = "Hủy kết bạn hoặc hủy lời mời đã gửi",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable UUID friendshipId,
            Authentication authentication) {
        log.info("User {} removing friendship {}", authentication.getName(), friendshipId);
        friendshipService.removeFriend(authentication.getName(), friendshipId);
        return ResponseEntity.ok(ApiResponse.success("Hủy kết bạn thành công", null));
    }

    // ─── Block User ─────────────────────────────────────────────────

    @PutMapping("/{friendshipId}/block")
    @Operation(summary = "Block user",
            description = "Chặn người dùng",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @PathVariable UUID friendshipId,
            Authentication authentication) {
        log.info("User {} blocking friendship {}", authentication.getName(), friendshipId);
        friendshipService.blockUser(authentication.getName(), friendshipId);
        return ResponseEntity.ok(ApiResponse.success("Chặn người dùng thành công", null));
    }

    // ─── Get Friends List ───────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get friends list",
            description = "Lấy danh sách bạn bè (đã accepted)",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getFriends(Authentication authentication) {
        log.info("User {} getting friends list", authentication.getName());
        List<FriendshipResponse> friends = friendshipService.getFriends(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bạn bè thành công", friends));
    }

    // ─── Get Pending Requests Received ──────────────────────────────

    @GetMapping("/requests/pending")
    @Operation(summary = "Get pending friend requests",
            description = "Lấy danh sách lời mời kết bạn đang chờ (nhận được)",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getPendingRequests(Authentication authentication) {
        log.info("User {} getting pending requests", authentication.getName());
        List<FriendshipResponse> requests = friendshipService.getPendingRequests(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lời mời đang chờ thành công", requests));
    }

    // ─── Get Sent Requests ──────────────────────────────────────────

    @GetMapping("/requests/sent")
    @Operation(summary = "Get sent friend requests",
            description = "Lấy danh sách lời mời kết bạn đã gửi",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getSentRequests(Authentication authentication) {
        log.info("User {} getting sent requests", authentication.getName());
        List<FriendshipResponse> requests = friendshipService.getSentRequests(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lời mời đã gửi thành công", requests));
    }

    // ─── Search Users ───────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search users to add friend",
            description = "Tìm kiếm người dùng để kết bạn theo tên hoặc email",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<FriendSearchResponse>>> searchUsers(
            @RequestParam String query,
            Authentication authentication) {
        log.info("User {} searching users with query: {}", authentication.getName(), query);
        List<FriendSearchResponse> results = friendshipService.searchUsers(authentication.getName(), query);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm người dùng thành công", results));
    }

    // ─── Get Friend Public Profile ──────────────────────────────────

    @GetMapping("/{userId}/profile")
    @Operation(summary = "Get friend's public profile",
            description = "Xem hồ sơ công khai của bạn bè (chỉ thông tin không nhạy cảm). Yêu cầu đã kết bạn.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<FriendPublicProfileResponse>> getFriendProfile(
            @PathVariable UUID userId,
            Authentication authentication) {
        log.info("User {} viewing public profile of {}", authentication.getName(), userId);
        FriendPublicProfileResponse profile = friendshipService.getFriendProfile(authentication.getName(), userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ bạn bè thành công", profile));
    }
}
