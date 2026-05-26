package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.StudySession;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.StudySessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyChallengeServiceTest {

    @Mock
    private ChallengeBankRepository challengeBankRepository;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AIService aiService;

    @Mock
    private FirebaseStorageService firebaseStorageService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BadgeUnlockService badgeUnlockService;

    @InjectMocks
    private DailyChallengeService dailyChallengeService;

    @Test
    void testGetDailyChallenges_DateMatch() {
        String today = LocalDate.now().toString();
        UUID cid = UUID.randomUUID();

        when(systemConfigService.getValue("daily.challenge.date", "")).thenReturn(today);
        when(systemConfigService.getValue("daily.challenge.ids", "")).thenReturn(cid.toString());

        ChallengeBank challenge = new ChallengeBank();
        challenge.setId(cid);
        challenge.setContentText("Học Tiếng Việt");

        when(challengeBankRepository.findById(cid)).thenReturn(Optional.of(challenge));

        List<ChallengeBank> results = dailyChallengeService.getDailyChallenges();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Học Tiếng Việt", results.get(0).getContentText());
    }

    @Test
    void testGetDailyChallenges_DateMismatch_Rotates() {
        String today = LocalDate.now().toString();

        when(systemConfigService.getValue("daily.challenge.date", "")).thenReturn("2026-01-01");
        when(systemConfigService.getValue("daily.challenge.ids", "")).thenReturn("");

        ChallengeBank c1 = new ChallengeBank();
        c1.setId(UUID.randomUUID());
        ChallengeBank c2 = new ChallengeBank();
        c2.setId(UUID.randomUUID());

        when(challengeBankRepository.findAll()).thenReturn(List.of(c1, c2));

        List<ChallengeBank> results = dailyChallengeService.getDailyChallenges();

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(systemConfigService, times(1)).updateConfigs(any());
    }

    @Test
    void testSubmitDailyChallenge_Correct() throws IOException {
        String email = "student@test.com";
        UUID cid = UUID.randomUUID();
        MockMultipartFile audio = new MockMultipartFile("audio", "recording.webm", "audio/webm", new byte[]{1, 2, 3});

        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setEmail(email);
        account.setTotalExperience(100);

        ChallengeBank challenge = new ChallengeBank();
        challenge.setId(cid);
        challenge.setContentText("Học Tiếng Việt");

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(challengeBankRepository.findById(cid)).thenReturn(Optional.of(challenge));

        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("isCorrect", true);
        evaluation.put("accuracy", 95);
        evaluation.put("feedback", "Excellent!");

        when(aiService.findErrorTagUnitId("BAC")).thenReturn(null);
        when(aiService.evaluatePronunciation(any(MultipartFile.class), eq("Học Tiếng Việt"), nullable(String.class)))
                .thenReturn(evaluation);
        when(firebaseStorageService.uploadFile(any(), eq("daily-challenge-attempts"))).thenReturn("http://firebase/test.wav");
        
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> {
            StudySession ss = invocation.getArgument(0);
            ss.setId(UUID.randomUUID());
            return ss;
        });

        // Config today's challenge ids
        when(systemConfigService.getValue("daily.challenge.date", "")).thenReturn(LocalDate.now().toString());
        when(systemConfigService.getValue("daily.challenge.ids", "")).thenReturn(cid.toString());
        when(challengeBankRepository.findById(cid)).thenReturn(Optional.of(challenge));

        // Stub findByAccountIdAndSessionType to return the saved session so we count it
        when(studySessionRepository.findByAccountIdAndSessionType(eq(account.getId()), eq("DAILY")))
                .thenReturn(new ArrayList<>()); // return empty so the set is filled dynamically

        ObjectMapper realMapper = new ObjectMapper();
        realMapper.findAndRegisterModules(); RealObjectMapperMockStub(realMapper);

        Map<String, Object> submissionResult = dailyChallengeService.submitDailyChallenge(email, cid, audio, "BAC");

        assertNotNull(submissionResult);
        assertTrue((Boolean) submissionResult.get("completedAllToday"));
        assertEquals(50, submissionResult.get("xpAwarded"));
        assertEquals(150, account.getTotalExperience());
        verify(badgeUnlockService, times(1)).checkAndUnlockBadges(account);
    }

    private void RealObjectMapperMockStub(ObjectMapper realMapper) throws IOException {
        lenient().when(objectMapper.writeValueAsString(any())).thenAnswer(inv -> realMapper.writeValueAsString(inv.getArgument(0)));
        lenient().when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenAnswer(inv -> realMapper.readValue((String) inv.getArgument(0), (com.fasterxml.jackson.core.type.TypeReference) inv.getArgument(1)));
    }
}
