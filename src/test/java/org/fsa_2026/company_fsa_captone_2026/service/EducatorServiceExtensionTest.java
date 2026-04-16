package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.SessionDetail;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChatMessageRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.EducatorFeedbackRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.SessionDetailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EducatorServiceExtensionTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SessionDetailRepository sessionDetailRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private EducatorFeedbackRepository educatorFeedbackRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EducatorService educatorService;

    @Test
    void testGetDashboardSummary() {
        String educatorEmail = "educator@test.com";

        Account educator = new Account();
        educator.setId(UUID.randomUUID());
        educator.setEmail(educatorEmail);
        educator.setRoleCode(RoleCode.EDUCATOR);

        Account student = new Account();
        student.setId(UUID.randomUUID());
        student.setEmail("student@test.com");
        student.setRoleCode(RoleCode.USER);
        student.setIsActive(true);
        student.setTotalExperience(20);
        student.setTotalStars(3);

        SessionDetail sessionDetail = new SessionDetail();
        sessionDetail.setId(UUID.randomUUID());
        sessionDetail.setScoreOverall(BigDecimal.valueOf(85));

        when(accountRepository.findByEmail(educatorEmail)).thenReturn(Optional.of(educator));
        when(accountRepository.findAll()).thenReturn(List.of(educator, student));
        when(sessionDetailRepository.findAll()).thenReturn(List.of(sessionDetail));
        when(educatorFeedbackRepository.count()).thenReturn(2L);

        var summary = educatorService.getDashboardSummary(educatorEmail);

        assertNotNull(summary);
        assertEquals(1, summary.get("totalStudents"));
        assertEquals(1L, summary.get("activeStudents"));
        assertEquals(85.0, summary.get("averagePronunciationScore"));
        assertEquals(2L, summary.get("pendingFeedbackCount"));
    }
}
