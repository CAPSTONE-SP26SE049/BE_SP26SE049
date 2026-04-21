package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.FriendPublicProfileResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.FriendSearchResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.FriendshipResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Friendship;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.FriendshipStatus;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.FriendshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;


@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private static final int MAX_FRIENDS = 100;
    private static final int MAX_PENDING_SENT = 20;

    private final FriendshipRepository friendshipRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    // ─── Send Friend Request ────────────────────────────────────────

    @Transactional
    public FriendshipResponse sendRequest(String email, UUID addresseeId) {
        Account requester = findAccountByEmail(email);

        // Cannot send request to yourself
        if (requester.getId().equals(addresseeId)) {
            throw new ApiException("BAD_REQUEST", "Không thể gửi lời mời kết bạn cho chính mình");
        }

        Account addressee = accountRepository.findById(addresseeId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        // Check if friendship already exists between these two users
        Optional<Friendship> existing = friendshipRepository.findByUsers(requester, addressee);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            switch (f.getStatus()) {
                case ACCEPTED:
                    throw new ApiException("BAD_REQUEST", "Hai bạn đã là bạn bè rồi");
                case PENDING:
                    throw new ApiException("BAD_REQUEST", "Lời mời kết bạn đã được gửi trước đó");
                case BLOCKED:
                    throw new ApiException("BAD_REQUEST", "Không thể gửi lời mời kết bạn");
                case DECLINED:
                    // Allow re-sending after a decline — remove old record
                    friendshipRepository.delete(f);
                    break;
            }
        }

        // Check friend limit (max 100)
        long friendCount = friendshipRepository.countAcceptedByAccount(requester.getId());
        if (friendCount >= MAX_FRIENDS) {
            throw new ApiException("BAD_REQUEST", "Bạn đã đạt giới hạn " + MAX_FRIENDS + " bạn bè");
        }

        // Check pending sent limit (max 20)
        long pendingSent = friendshipRepository.countPendingSentByAccount(requester.getId());
        if (pendingSent >= MAX_PENDING_SENT) {
            throw new ApiException("BAD_REQUEST", "Bạn đã đạt giới hạn " + MAX_PENDING_SENT + " lời mời đang chờ");
        }

        // Create friendship
        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(FriendshipStatus.PENDING)
                .build();
        friendshipRepository.save(friendship);

        // Send notification to addressee
        notificationService.createNotification(
                addressee.getId(),
                "FRIEND_REQUEST",
                "Lời mời kết bạn",
                (requester.getFullName() != null ? requester.getFullName() : requester.getEmail())
                        + " đã gửi lời mời kết bạn cho bạn",
                friendship.getId()
        );

        log.info("Friend request sent: {} -> {}", requester.getEmail(), addressee.getEmail());
        return toResponse(friendship, requester);
    }

    // ─── Accept Request ─────────────────────────────────────────────

    @Transactional
    public FriendshipResponse acceptRequest(String email, UUID friendshipId) {
        Account currentUser = findAccountByEmail(email);
        Friendship friendship = findFriendshipById(friendshipId);

        // Only the addressee can accept
        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new ApiException("FORBIDDEN", "Chỉ người nhận mới có thể chấp nhận lời mời");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ApiException("BAD_REQUEST", "Lời mời kết bạn không ở trạng thái chờ");
        }

        // Check if addressee has reached friend limit
        long friendCount = friendshipRepository.countAcceptedByAccount(currentUser.getId());
        if (friendCount >= MAX_FRIENDS) {
            throw new ApiException("BAD_REQUEST", "Bạn đã đạt giới hạn " + MAX_FRIENDS + " bạn bè");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        // Notify the requester
        notificationService.createNotification(
                friendship.getRequester().getId(),
                "FRIEND_ACCEPTED",
                "Lời mời được chấp nhận",
                (currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getEmail())
                        + " đã chấp nhận lời mời kết bạn",
                friendship.getId()
        );

        log.info("Friend request accepted: {} accepted {}", email, friendship.getRequester().getEmail());
        return toResponse(friendship, currentUser);
    }

    // ─── Decline Request ────────────────────────────────────────────

    @Transactional
    public void declineRequest(String email, UUID friendshipId) {
        Account currentUser = findAccountByEmail(email);
        Friendship friendship = findFriendshipById(friendshipId);

        // Only the addressee can decline
        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new ApiException("FORBIDDEN", "Chỉ người nhận mới có thể từ chối lời mời");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ApiException("BAD_REQUEST", "Lời mời kết bạn không ở trạng thái chờ");
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        friendshipRepository.save(friendship);
        log.info("Friend request declined: {} declined {}", email, friendship.getRequester().getEmail());
    }

    // ─── Remove Friend / Cancel Request ─────────────────────────────

    @Transactional
    public void removeFriend(String email, UUID friendshipId) {
        Account currentUser = findAccountByEmail(email);
        Friendship friendship = findFriendshipById(friendshipId);

        // Either party can remove
        if (!friendship.getRequester().getId().equals(currentUser.getId())
                && !friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new ApiException("FORBIDDEN", "Bạn không có quyền thực hiện hành động này");
        }

        friendshipRepository.delete(friendship);
        log.info("Friendship removed: {} removed friendship {}", email, friendshipId);
    }

    // ─── Block User ─────────────────────────────────────────────────

    @Transactional
    public void blockUser(String email, UUID friendshipId) {
        Account currentUser = findAccountByEmail(email);
        Friendship friendship = findFriendshipById(friendshipId);

        // Either party can block
        if (!friendship.getRequester().getId().equals(currentUser.getId())
                && !friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new ApiException("FORBIDDEN", "Bạn không có quyền thực hiện hành động này");
        }

        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendshipRepository.save(friendship);
        log.info("User blocked: {} blocked friendship {}", email, friendshipId);
    }

    // ─── Get Friends List ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FriendshipResponse> getFriends(String email) {
        Account currentUser = findAccountByEmail(email);

        return friendshipRepository.findAllAcceptedByAccount(currentUser.getId())
                .stream()
                .map(f -> toResponse(f, currentUser))
                .collect(Collectors.toList());
    }

    // ─── Get Pending Requests Received ──────────────────────────────

    @Transactional(readOnly = true)
    public List<FriendshipResponse> getPendingRequests(String email) {
        Account currentUser = findAccountByEmail(email);

        return friendshipRepository.findPendingRequestsReceived(currentUser.getId())
                .stream()
                .map(f -> {
                    Account other = f.getRequester();
                    return FriendshipResponse.builder()
                            .friendshipId(f.getId())
                            .userId(other.getId())
                            .fullName(other.getFullName())
                            .avatarUrl(other.getAvatarUrl())
                            .status(f.getStatus().name())
                            .createdAt(f.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Get Sent Requests ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FriendshipResponse> getSentRequests(String email) {
        Account currentUser = findAccountByEmail(email);

        return friendshipRepository.findPendingSentRequests(currentUser.getId())
                .stream()
                .map(f -> {
                    Account other = f.getAddressee();
                    return FriendshipResponse.builder()
                            .friendshipId(f.getId())
                            .userId(other.getId())
                            .fullName(other.getFullName())
                            .avatarUrl(other.getAvatarUrl())
                            .status(f.getStatus().name())
                            .createdAt(f.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Search Users ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FriendSearchResponse> searchUsers(String email, String query) {
        Account currentUser = findAccountByEmail(email);

        if (query == null || query.trim().length() < 2) {
            throw new ApiException("BAD_REQUEST", "Từ khóa tìm kiếm phải có ít nhất 2 ký tự");
        }

        String trimmedQuery = query.trim();

        // ✅ DB-level LIKE search — no more findAll() in memory!
        List<Account> matchedUsers = accountRepository.searchActiveAccountsByNameOrEmail(
                trimmedQuery, currentUser.getId(), PageRequest.of(0, 20));

        if (matchedUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch fetch friendship statuses
        List<UUID> matchedIds = matchedUsers.stream().map(Account::getId).collect(Collectors.toList());
        List<Friendship> friendships = friendshipRepository.findAllByAccountAndOtherIds(currentUser.getId(), matchedIds);

        // Build a map: otherUserId -> status
        Map<UUID, String> statusMap = new HashMap<>();
        for (Friendship f : friendships) {
            UUID otherId = f.getRequester().getId().equals(currentUser.getId())
                    ? f.getAddressee().getId()
                    : f.getRequester().getId();
            statusMap.put(otherId, f.getStatus().name());
        }

        return matchedUsers.stream()
                .map(a -> FriendSearchResponse.builder()
                        .userId(a.getId())
                        .fullName(a.getFullName())
                        .avatarUrl(a.getAvatarUrl())
                        .friendshipStatus(statusMap.getOrDefault(a.getId(), null))
                        .build())
                .collect(Collectors.toList());
    }

    // ─── View Friend Profile ─────────────────────────────────────────

    /**
     * Returns the public profile of a friend.
     * Only allowed if the two users have an ACCEPTED friendship.
     * Sensitive fields (email, phone, passwordHash, codes) are never exposed.
     */
    @Transactional(readOnly = true)
    public FriendPublicProfileResponse getFriendProfile(String requesterEmail, UUID targetUserId) {
        Account requester = findAccountByEmail(requesterEmail);

        // Self-lookup not needed here but guard anyway
        if (requester.getId().equals(targetUserId)) {
            throw new ApiException("BAD_REQUEST", "Không thể xem hồ sơ của chính mình qua endpoint này");
        }

        Account target = accountRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        // Verify friendship is ACCEPTED
        Friendship friendship = friendshipRepository.findByUsers(requester, target)
                .orElseThrow(() -> new ApiException("FORBIDDEN", "Bạn chưa kết bạn với người này"));

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new ApiException("FORBIDDEN", "Bạn chưa kết bạn với người này");
        }

        return FriendPublicProfileResponse.builder()
                .id(target.getId())
                .fullName(target.getFullName())
                .avatarUrl(target.getAvatarUrl())
                .region(target.getRegion())
                .totalStars(target.getTotalStars())
                .currentStreakDays(target.getCurrentStreakDays())
                .totalExperience(target.getTotalExperience())
                .memberSince(target.getCreatedAt())
                .build();
    }

    // ─── Helper Methods ─────────────────────────────────────────────

    private Account findAccountByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));
    }

    private Friendship findFriendshipById(UUID friendshipId) {
        return friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy quan hệ bạn bè"));
    }

    /**
     * Convert Friendship entity to FriendshipResponse, showing the "other" user's info.
     */
    private FriendshipResponse toResponse(Friendship friendship, Account currentUser) {
        Account other = friendship.getRequester().getId().equals(currentUser.getId())
                ? friendship.getAddressee()
                : friendship.getRequester();

        return FriendshipResponse.builder()
                .friendshipId(friendship.getId())
                .userId(other.getId())
                .fullName(other.getFullName())
                .avatarUrl(other.getAvatarUrl())
                .status(friendship.getStatus().name())
                .createdAt(friendship.getCreatedAt())
                .build();
    }
}
