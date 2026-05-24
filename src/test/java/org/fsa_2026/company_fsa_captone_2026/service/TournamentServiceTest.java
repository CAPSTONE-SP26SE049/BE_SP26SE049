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

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void testFinalizeWeeklyTournament_Success() {
        UUID tournamentId = UUID.randomUUID();
        Tournament activeTournament = Tournament.builder()
                .name("Weekly Tour #1")
                .type("WEEKLY")
                .status("ACTIVE")
                .startsAt(Instant.now())
                .endsAt(Instant.now())
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
}
