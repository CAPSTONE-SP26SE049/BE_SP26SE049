package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Friendship;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Friendship Repository
 */
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    /**
     * Find existing friendship between two users (in either direction).
     */
    @Query("SELECT f FROM Friendship f JOIN FETCH f.requester JOIN FETCH f.addressee WHERE " +
           "(f.requester = :user1 AND f.addressee = :user2) OR " +
           "(f.requester = :user2 AND f.addressee = :user1)")
    Optional<Friendship> findByUsers(@Param("user1") Account user1, @Param("user2") Account user2);

    /**
     * List all accepted friends for a given account (as requester or addressee).
     */
    @Query("SELECT f FROM Friendship f JOIN FETCH f.requester JOIN FETCH f.addressee WHERE " +
           "(f.requester.id = :accountId OR f.addressee.id = :accountId) " +
           "AND f.status = 'ACCEPTED' ORDER BY f.updatedAt DESC")
    List<Friendship> findAllAcceptedByAccount(@Param("accountId") UUID accountId);

    /**
     * Pending friend requests received by the given account.
     */
    @Query("SELECT f FROM Friendship f JOIN FETCH f.requester JOIN FETCH f.addressee WHERE f.addressee.id = :accountId AND f.status = 'PENDING' ORDER BY f.createdAt DESC")
    List<Friendship> findPendingRequestsReceived(@Param("accountId") UUID accountId);

    /**
     * Pending friend requests sent by the given account.
     */
    @Query("SELECT f FROM Friendship f JOIN FETCH f.requester JOIN FETCH f.addressee WHERE f.requester.id = :accountId AND f.status = 'PENDING' ORDER BY f.createdAt DESC")
    List<Friendship> findPendingSentRequests(@Param("accountId") UUID accountId);

    /**
     * Count accepted friendships for limit enforcement (max 100).
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
           "(f.requester.id = :accountId OR f.addressee.id = :accountId) " +
           "AND f.status = 'ACCEPTED'")
    long countAcceptedByAccount(@Param("accountId") UUID accountId);

    /**
     * Count pending sent requests for limit enforcement (max 20).
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.requester.id = :accountId AND f.status = 'PENDING'")
    long countPendingSentByAccount(@Param("accountId") UUID accountId);

    /**
     * Find all friendships (any status) involving two accounts — used for search to show status.
     */
    @Query("SELECT f FROM Friendship f JOIN FETCH f.requester JOIN FETCH f.addressee WHERE " +
           "(f.requester.id = :accountId AND f.addressee.id IN :otherIds) OR " +
           "(f.addressee.id = :accountId AND f.requester.id IN :otherIds)")
    List<Friendship> findAllByAccountAndOtherIds(@Param("accountId") UUID accountId,
                                                  @Param("otherIds") List<UUID> otherIds);
}
