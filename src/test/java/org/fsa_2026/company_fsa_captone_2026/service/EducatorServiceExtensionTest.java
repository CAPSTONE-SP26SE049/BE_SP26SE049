package org.fsa_2026.company_fsa_captone_2026.service;

import org.fsa_2026.company_fsa_captone_2026.dto.PlacementRuleRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.PlacementRuleResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizQuestionRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Level;
import org.fsa_2026.company_fsa_captone_2026.entity.PlacementRule;
import org.fsa_2026.company_fsa_captone_2026.entity.Quiz;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LevelRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.PlacementRuleRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuizRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EducatorServiceExtensionTest {

    @Mock
    private PlacementRuleRepository placementRuleRepository;

    @Mock
    private org.fsa_2026.company_fsa_captone_2026.repository.ErrorTagRepository errorTagRepository;

    @Mock
    private org.fsa_2026.company_fsa_captone_2026.repository.DialectRepository dialectRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LevelRepository levelRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private org.fsa_2026.company_fsa_captone_2026.repository.ContentApprovalHistoryRepository contentApprovalHistoryRepository;

    @InjectMocks
    private EducatorService educatorService;

    @Test
    public void testUpdateOrCreatePlacementRule() {
        java.util.UUID errorTagId = java.util.UUID.randomUUID();
        java.util.UUID dialectId = java.util.UUID.randomUUID();

        PlacementRuleRequest request = PlacementRuleRequest.builder()
                .errorTagId(errorTagId)
                .threshold(30)
                .dialectId(dialectId)
                .checkpoint("level-1")
                .priority(1)
                .build();

        org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag mockErrorTag = new org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag();
        mockErrorTag.setId(errorTagId);
        mockErrorTag.setTagCode("L_N");

        org.fsa_2026.company_fsa_captone_2026.entity.Dialect mockDialect = new org.fsa_2026.company_fsa_captone_2026.entity.Dialect();
        mockDialect.setId(dialectId);
        mockDialect.setName("Northern");

        when(errorTagRepository.findById(errorTagId)).thenReturn(java.util.Optional.of(mockErrorTag));
        when(dialectRepository.findById(dialectId)).thenReturn(java.util.Optional.of(mockDialect));
        when(placementRuleRepository.findAll()).thenReturn(new ArrayList<>());
        when(placementRuleRepository.save(any(PlacementRule.class)))
                .thenAnswer(invocation -> {
                    PlacementRule rule = invocation.getArgument(0);
                    if (rule.getId() == null) rule.setId(java.util.UUID.randomUUID());
                    return rule;
                });

        PlacementRuleResponse result = educatorService.updateOrCreatePlacementRule(request);

        assertNotNull(result);
        assertEquals(errorTagId.toString(), result.getErrorTag().getId());
        assertEquals(30, result.getThreshold());
        assertEquals(dialectId.toString(), result.getDialectId());
    }

    @Test
    public void testCreateQuiz() {
        UUID levelId = UUID.randomUUID();
        String educatorEmail = "educator@test.com";

        // Mocks
        Account mockAccount = new Account();
        mockAccount.setId(UUID.randomUUID());
        mockAccount.setEmail(educatorEmail);

        Level mockLevel = new Level();
        mockLevel.setId(levelId);

        when(accountRepository.findByEmail(educatorEmail)).thenReturn(Optional.of(mockAccount));
        when(levelRepository.findById(levelId)).thenReturn(Optional.of(mockLevel));

        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz q = invocation.getArgument(0);
            if (q.getId() == null) q.setId(UUID.randomUUID());
            if (q.getQuestions() != null) {
                for (org.fsa_2026.company_fsa_captone_2026.entity.QuizQuestion qt : q.getQuestions()) {
                    if (qt.getId() == null) qt.setId(UUID.randomUUID());
                }
            }
            return q;
        });

        // Request
        QuizQuestionRequest qReq = QuizQuestionRequest.builder()
                .skillType("READING")
                .difficulty("EASY")
                .questionOrder(1)
                .points(10)
                .contentData(Map.of("passage", "Ba tôi nàm nông.", "targetErrorWord", "nàm"))
                .build();

        QuizCreateRequest request = QuizCreateRequest.builder()
                .levelId(levelId)
                .title("Test Quiz")
                .passingScore(80)
                .questions(List.of(qReq))
                .build();

        // Execute
        QuizResponse response = educatorService.createQuiz(educatorEmail, request);

        // Verify
        assertNotNull(response);
        assertEquals("Test Quiz", response.getTitle());
        assertEquals("PENDING", response.getStatus());
        assertEquals(80, response.getPassingScore());
        assertNotNull(response.getQuestions());
        assertEquals(1, response.getQuestions().size());
        assertEquals("READING", response.getQuestions().get(0).getSkillType());
    }

    @Test
    public void testUpdateQuiz_ApprovedCreatesDraft() {
        UUID quizId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        String educatorEmail = "educator@test.com";

        // Existing approved quiz
        Quiz existingQuiz = new Quiz();
        existingQuiz.setId(quizId);
        existingQuiz.setStatus(org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.APPROVED);
        existingQuiz.setTitle("Original Title");

        Account mockAccount = new Account();
        mockAccount.setId(UUID.randomUUID());
        mockAccount.setEmail(educatorEmail);

        Level mockLevel = new Level();
        mockLevel.setId(levelId);

        when(accountRepository.findByEmail(educatorEmail)).thenReturn(Optional.of(mockAccount));
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(levelRepository.findById(levelId)).thenReturn(Optional.of(mockLevel));

        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz q = invocation.getArgument(0);
            if (q.getId() == null) q.setId(UUID.randomUUID());
            if (q.getQuestions() != null) {
                for (org.fsa_2026.company_fsa_captone_2026.entity.QuizQuestion qt : q.getQuestions()) {
                    if (qt.getId() == null) qt.setId(UUID.randomUUID());
                }
            }
            return q;
        });

        // Request with new title
        QuizCreateRequest request = QuizCreateRequest.builder()
                .levelId(levelId)
                .title("Updated Title Draft")
                .passingScore(90)
                .build();

        QuizResponse response = educatorService.updateQuiz(educatorEmail, quizId, request);

        // Verify a new Draft PENDING is created
        assertNotNull(response);
        assertEquals("Updated Title Draft", response.getTitle());
        assertEquals("PENDING", response.getStatus());
        assertEquals(90, response.getPassingScore());
    }
}
