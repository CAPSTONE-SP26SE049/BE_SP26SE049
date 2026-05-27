package org.fsa_2026.company_fsa_captone_2026.service;

import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;
import org.fsa_2026.company_fsa_captone_2026.entity.Tournament;
import org.fsa_2026.company_fsa_captone_2026.entity.TournamentParticipant;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRewardRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.RewardCatalogRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.TournamentParticipantRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.TournamentRepository;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RewardCatalogRepository rewardCatalogRepository;

    @Mock
    private AccountRewardRepository accountRewardRepository;

    @Mock
    private BadgeUnlockService badgeUnlockService;

    @Mock
    private org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository challengeBankRepository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void testFinalizeWeeklyTournament_Success() {
        UUID tournamentId = UUID.randomUUID();
        Tournament activeTournament = Tournament.builder()
                .name("Weekly Tour #1")
                .type("WEEKLY")
                .status("ACTIVE")
                .startsAt(Instant.now().minusSeconds(120))
                .endsAt(Instant.now().minusSeconds(60))
                .build();
        activeTournament.setId(tournamentId);

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(activeTournament));

        // Create 3 players
        Account p1 = Account.builder().email("p1@test.com").fullName("Player One").totalExperience(100).build();
        p1.setId(UUID.randomUUID());
        Account p2 = Account.builder().email("p2@test.com").fullName("Player Two").totalExperience(100).build();
        p2.setId(UUID.randomUUID());
        Account p3 = Account.builder().email("p3@test.com").fullName("Player Three").totalExperience(100).build();
        p3.setId(UUID.randomUUID());

        TournamentParticipant part1 = TournamentParticipant.builder().account(p1).totalXp(500).build();
        TournamentParticipant part2 = TournamentParticipant.builder().account(p2).totalXp(300).build();
        TournamentParticipant part3 = TournamentParticipant.builder().account(p3).totalXp(100).build();

        when(tournamentParticipantRepository.findByTournamentIdOrderByTotalXpDesc(tournamentId))
                .thenReturn(List.of(part1, part2, part3));

        // Mock Badges
        RewardCatalog winnerBadge = RewardCatalog.builder().code("TOURNAMENT_WINNER").name("Tournament Champion").build();
        winnerBadge.setId(UUID.randomUUID());
        RewardCatalog participantBadge = RewardCatalog.builder().code("TOURNAMENT_PARTICIPANT").name("Participant").build();
        participantBadge.setId(UUID.randomUUID());

        when(rewardCatalogRepository.findByCode("TOURNAMENT_WINNER")).thenReturn(Optional.of(winnerBadge));
        when(rewardCatalogRepository.findByCode("TOURNAMENT_PARTICIPANT")).thenReturn(Optional.of(participantBadge));

        when(accountRewardRepository.findByAccountIdAndRewardCatalogId(any(), any())).thenReturn(Optional.empty());

        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

        when(challengeBankRepository.findBySkillType(any())).thenReturn(List.of());

        // Run finalization
        Map<String, Object> result = tournamentService.finalizeWeeklyTournament(tournamentId);

        assertNotNull(result);
        assertEquals("Weekly Tour #1", result.get("finalizedTournament"));
        assertTrue(result.get("champion").toString().contains("Player One"));
        assertTrue(result.get("runnerUp").toString().contains("Player Two"));
        assertTrue(result.get("thirdPlace").toString().contains("Player Three"));

        // Verify prize XP increments
        assertEquals(600, p1.getTotalExperience()); // 100 + 500
        assertEquals(350, p2.getTotalExperience()); // 100 + 250
        assertEquals(200, p3.getTotalExperience()); // 100 + 100

        // Verify saves and unlocks
        verify(accountRepository, atLeastOnce()).save(p1);
        verify(accountRepository, atLeastOnce()).save(p2);
        verify(accountRepository, atLeastOnce()).save(p3);
        verify(accountRewardRepository, times(4)).save(any()); // 1 for winner badge + 3 for participant badges
    }

    @Test
    void testUpdateActiveTournament_ThrowsException_WhenActive() {
        Tournament activeTournament = Tournament.builder()
                .name("Weekly Tour #1")
                .type("WEEKLY")
                .status("ACTIVE")
                .questionsJson("[]")
                .build();
        activeTournament.setId(UUID.randomUUID());

        when(tournamentRepository.findByTypeAndStatus("WEEKLY", "ACTIVE")).thenReturn(List.of(activeTournament));

        Instant newEndsAt = Instant.now().plusSeconds(3600);
        ApiException exception = assertThrows(ApiException.class, () -> {
            tournamentService.updateActiveTournament("New Name", "New Desc", newEndsAt);
        });

        assertEquals("Giải đấu đang diễn ra (ACTIVE). Không thể chỉnh sửa thông tin để bảo vệ quyền lợi của các học viên đang thi đấu.", exception.getMessage());
    }

    @Test
    void testAssignActiveTournamentQuestions_ThrowsException_WhenActive() {
        Tournament activeTournament = Tournament.builder()
                .name("Weekly Tour #1")
                .type("WEEKLY")
                .status("ACTIVE")
                .questionsJson("[\"existing-uuid\"]")
                .build();
        activeTournament.setId(UUID.randomUUID());

        when(tournamentRepository.findByTypeAndStatus("WEEKLY", "ACTIVE")).thenReturn(List.of(activeTournament));

        UUID q1 = UUID.randomUUID();
        List<UUID> qids = List.of(q1);

        ApiException exception = assertThrows(ApiException.class, () -> {
            tournamentService.assignActiveTournamentQuestions(qids);
        });

        assertEquals("Giải đấu đang diễn ra (ACTIVE). Không thể thay đổi bộ câu hỏi thi đấu để bảo vệ quyền lợi của các học viên.", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetFinishedTournamentsHistory_Success() {
        Tournament finishedTournament = Tournament.builder()
                .name("Weekly Tour #0 (Finished)")
                .type("WEEKLY")
                .status("FINISHED")
                .startsAt(Instant.now().minusSeconds(86400 * 7))
                .endsAt(Instant.now().minusSeconds(86400 * 6))
                .build();
        UUID tId = UUID.randomUUID();
        finishedTournament.setId(tId);

        when(tournamentRepository.findByTypeAndStatusOrderByEndsAtDesc("WEEKLY", "FINISHED"))
                .thenReturn(List.of(finishedTournament));

        Account championUser = Account.builder()
                .email("champ@test.com")
                .fullName("The Champion")
                .avatarUrl("avatar.png")
                .build();
        championUser.setId(UUID.randomUUID());

        TournamentParticipant participant = TournamentParticipant.builder()
                .tournament(finishedTournament)
                .account(championUser)
                .totalXp(600)
                .challengesCompleted(5)
                .averageScore(java.math.BigDecimal.valueOf(98.5))
                .build();

        when(tournamentParticipantRepository.findByTournamentIdOrderByTotalXpDesc(tId))
                .thenReturn(List.of(participant));

        List<Map<String, Object>> history = tournamentService.getFinishedTournamentsHistory();

        assertNotNull(history);
        assertEquals(1, history.size());
        Map<String, Object> hEntry = history.get(0);
        assertEquals("Weekly Tour #0 (Finished)", hEntry.get("name"));
        assertEquals("FINISHED", hEntry.get("status"));

        List<Map<String, Object>> winners = (List<Map<String, Object>>) hEntry.get("winners");
        assertNotNull(winners);
        assertEquals(1, winners.size());
        Map<String, Object> champEntry = winners.get(0);
        assertEquals(1, champEntry.get("rankPosition"));
        assertEquals("The Champion", champEntry.get("fullName"));
        assertEquals("champ@test.com", champEntry.get("email"));
        assertEquals(600, champEntry.get("totalXp"));
    }

    @Test
    void testCreateUpcomingTournament_Success() {
        Instant startsAt = Instant.now().plusSeconds(3600);
        Instant endsAt = Instant.now().plusSeconds(7200);

        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

        Tournament created = tournamentService.createUpcomingTournament(
                "Upcoming Season #2",
                "Description test",
                startsAt,
                endsAt,
                List.of()
        );

        assertNotNull(created);
        assertEquals("Upcoming Season #2", created.getName());
        assertEquals("Description test", created.getDescription());
        assertEquals("UPCOMING", created.getStatus());
        assertEquals("WEEKLY", created.getType());
        assertEquals(startsAt, created.getStartsAt());
        assertEquals(endsAt, created.getEndsAt());
    }

    @Test
    void testFinalizeWeeklyTournament_WithUpcoming_Success() {
        UUID tournamentId = UUID.randomUUID();
        Tournament activeTournament = Tournament.builder()
                .name("Weekly Tour #1")
                .type("WEEKLY")
                .status("ACTIVE")
                .startsAt(Instant.now().minusSeconds(120))
                .endsAt(Instant.now().minusSeconds(60))
                .build();
        activeTournament.setId(tournamentId);

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(activeTournament));

        // Upcoming tournament exists
        Tournament upcomingTournament = Tournament.builder()
                .name("Upcoming Weekly Season #2")
                .type("WEEKLY")
                .status("UPCOMING")
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .build();

        when(tournamentRepository.findByTypeAndStatusOrderByStartsAtAsc("WEEKLY", "UPCOMING"))
                .thenReturn(new ArrayList<>(List.of(upcomingTournament)));

        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

        // Empty winners / participants mock to trigger fallback without crash
        when(tournamentParticipantRepository.findByTournamentIdOrderByTotalXpDesc(tournamentId))
                .thenReturn(List.of());
        when(accountRepository.findTop50ByIsActiveTrueOrderByTotalStarsDescBadgeCountDescCurrentStreakDaysDesc())
                .thenReturn(List.of());

        Map<String, Object> result = tournamentService.finalizeWeeklyTournament(tournamentId);

        assertNotNull(result);
        assertEquals("Weekly Tour #1", result.get("finalizedTournament"));
        assertEquals("Upcoming Weekly Season #2", result.get("newTournament"));
        assertEquals("ACTIVE", upcomingTournament.getStatus());
    }
}

