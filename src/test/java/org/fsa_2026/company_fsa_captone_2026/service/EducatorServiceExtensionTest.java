package org.fsa_2026.company_fsa_captone_2026.service;

import org.fsa_2026.company_fsa_captone_2026.dto.PlacementRuleRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.PlacementRuleResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizQuestionRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.PlacementRule;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentItemRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.PlacementRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
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
    private LearningUnitRepository learningUnitRepository;

    @Mock
    private ContentItemRepository contentItemRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private org.fsa_2026.company_fsa_captone_2026.repository.ContentApprovalHistoryRepository contentApprovalHistoryRepository;

    @InjectMocks
    private EducatorService educatorService;

    @Test
    public void testUpdateOrCreatePlacementRule() {
        UUID errorTagId = UUID.randomUUID();
        UUID dialectId = UUID.randomUUID();

        PlacementRuleRequest request = PlacementRuleRequest.builder()
                .errorTagId(errorTagId)
                .threshold(30)
                .dialectId(dialectId)
                .checkpoint("level-1")
                .priority(1)
                .build();

        LearningUnit mockErrorTag = new LearningUnit();
        mockErrorTag.setId(errorTagId);
        mockErrorTag.setType("ERROR_TAG");
        mockErrorTag.setName("L_N");

        LearningUnit mockDialect = new LearningUnit();
        mockDialect.setId(dialectId);
        mockDialect.setType("DIALECT");
        mockDialect.setName("Northern");

        when(learningUnitRepository.findById(errorTagId)).thenReturn(Optional.of(mockErrorTag));
        when(learningUnitRepository.findById(dialectId)).thenReturn(Optional.of(mockDialect));
        when(placementRuleRepository.findAll()).thenReturn(new ArrayList<>());
        when(placementRuleRepository.save(any(PlacementRule.class)))
                .thenAnswer(invocation -> {
                    PlacementRule rule = invocation.getArgument(0);
                    if (rule.getId() == null) rule.setId(UUID.randomUUID());
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

        LearningUnit mockLevel = new LearningUnit();
        mockLevel.setId(levelId);
        mockLevel.setType("LEVEL");
        mockLevel.setName("Level 1");

        UUID challengeId = UUID.randomUUID();
        ContentItem mockChallenge = new ContentItem();
        mockChallenge.setId(challengeId);
        mockChallenge.setLearningUnit(mockLevel);
        mockChallenge.setTitle("Ba tôi nàm nông.");
        mockChallenge.setType("PRONUNCIATION");
        mockChallenge.setStatus("PENDING");

        when(accountRepository.findByEmail(educatorEmail)).thenReturn(Optional.of(mockAccount));
        when(learningUnitRepository.findById(levelId)).thenReturn(Optional.of(mockLevel));
        when(contentItemRepository.findById(challengeId)).thenReturn(Optional.of(mockChallenge));

        when(contentItemRepository.save(any(ContentItem.class))).thenAnswer(invocation -> {
            ContentItem q = invocation.getArgument(0);
            if (q.getId() == null) q.setId(UUID.randomUUID());
            return q;
        });

        // Request
        QuizQuestionRequest qReq = QuizQuestionRequest.builder()
                .skillType("READING")
                .difficulty("EASY")
                .questionOrder(1)
                .points(10)
                .challengeId(challengeId)
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
        ContentItem existingQuiz = new ContentItem();
        existingQuiz.setId(quizId);
        existingQuiz.setType("QUIZ");
        existingQuiz.setStatus("APPROVED");
        existingQuiz.setTitle("Original Title");

        Account mockAccount = new Account();
        mockAccount.setId(UUID.randomUUID());
        mockAccount.setEmail(educatorEmail);

        LearningUnit mockLevel = new LearningUnit();
        mockLevel.setId(levelId);
        mockLevel.setType("LEVEL");
        mockLevel.setName("Level 1");

        when(accountRepository.findByEmail(educatorEmail)).thenReturn(Optional.of(mockAccount));
        when(contentItemRepository.findById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(learningUnitRepository.findById(levelId)).thenReturn(Optional.of(mockLevel));

        when(contentItemRepository.save(any(ContentItem.class))).thenAnswer(invocation -> {
            ContentItem q = invocation.getArgument(0);
            if (q.getId() == null) q.setId(UUID.randomUUID());
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
