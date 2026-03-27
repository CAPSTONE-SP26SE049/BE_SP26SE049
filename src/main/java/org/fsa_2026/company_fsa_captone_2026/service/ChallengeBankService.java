package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeBankRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizChallengeItemResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentItemRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuizChallengeItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeBankService {

    private final ChallengeBankRepository challengeBankRepository;
    private final QuizChallengeItemRepository quizChallengeItemRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final ContentItemRepository contentItemRepository;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChallengeBank createChallenge(ChallengeBankRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản để gán người tạo"));

        ChallengeBank challenge = ChallengeBank.builder()
                .contentText(request.getContentText())
                .skillType(request.getSkillType())
                .difficultyTag(request.getDifficultyTag())
                .region(request.getRegion() != null ? request.getRegion() : "BAC")
                .metadataJson(request.getMetadataJson())
                .createdBy(account.getId())
                .build();
        return challengeBankRepository.save(challenge);
    }
    @Transactional(readOnly = true)
    public List<ChallengeBank> getAllChallenges() {
        return challengeBankRepository.findAll();
    }

    @Transactional
    public List<QuizChallengeItem> assignChallengesToQuiz(UUID quizId, List<UUID> challengeIds) {
        // Remove existing items if we want to overwrite, or just append.
        // For simplicity and based on most common use cases, we'll append or just add new ones.
        // But usually "assign" might mean "set the list of challenges".
        
        // Let's just create new items for each challengeId provided.
        List<QuizChallengeItem> items = challengeIds.stream().map(cid -> QuizChallengeItem.builder()
                .quizId(quizId)
                .challengeBankId(cid)
                .challengeId(cid) // Map to both columns to satisfy legacy DB constraint
                .build()).collect(Collectors.toList());
        
        // Find current max order index if appending, or start from 1.
        // For now, let's just use the index in the provided list.
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setOrderIndex(i + 1);
        }

        return quizChallengeItemRepository.saveAll(items);
    }
    @Transactional(readOnly = true)
    public List<QuizChallengeItemResponse> getChallengesByQuizId(UUID quizId) {
        // Primary source: quiz_challenge_item table (explicit assignment)
        List<QuizChallengeItem> items = quizChallengeItemRepository.findByQuizIdOrderByOrderIndex(quizId);

        if (!items.isEmpty()) {
            return items.stream()
                    .map(item -> QuizChallengeItemResponse.builder()
                            .orderIndex(item.getOrderIndex())
                            .challenge(challengeBankRepository.findById(item.getChallengeBankId()).orElse(null))
                            .build())
                    .collect(Collectors.toList());
        }

        // Fallback: read challengeIds from ContentItem (Educator quiz) itemsJson
        // ContentItem.itemsJson stores: [{"challenge_id":"...","skill_type":"...","question_order":1,...}]
        java.util.Optional<ContentItem> contentItemOpt = contentItemRepository.findById(quizId);
        if (contentItemOpt.isPresent() && contentItemOpt.get().getItemsJson() != null) {
            try {
                com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>> listMapType =
                        new com.fasterxml.jackson.core.type.TypeReference<>() {};
                List<java.util.Map<String, Object>> questions =
                        objectMapper.readValue(contentItemOpt.get().getItemsJson(), listMapType);

                java.util.concurrent.atomic.AtomicInteger idx = new java.util.concurrent.atomic.AtomicInteger(1);
                return questions.stream()
                        .filter(q -> q.get("challenge_id") != null)
                        .sorted(java.util.Comparator.comparingInt(q -> {
                            Object o = q.get("question_order");
                            return o instanceof Number ? ((Number) o).intValue() : 0;
                        }))
                        .map(q -> {
                            UUID challengeId = UUID.fromString(q.get("challenge_id").toString());
                            return QuizChallengeItemResponse.builder()
                                    .orderIndex(idx.getAndIncrement())
                                    .challenge(challengeBankRepository.findById(challengeId).orElse(null))
                                    .build();
                        })
                        .filter(r -> r.getChallenge() != null)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // Fall through to LearningUnit fallback
            }
        }

        // Legacy fallback: LearningUnit metadataJson.questions[]
        return learningUnitRepository.findById(quizId).map(quiz -> {
            if (quiz.getMetadataJson() == null) return java.util.Collections.<QuizChallengeItemResponse>emptyList();
            try {
                com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>> mapType =
                        new com.fasterxml.jackson.core.type.TypeReference<>() {};
                java.util.Map<String, Object> meta = objectMapper.readValue(quiz.getMetadataJson(), mapType);
                Object questionsRaw = meta.get("questions");
                if (questionsRaw == null) return java.util.Collections.<QuizChallengeItemResponse>emptyList();

                com.fasterxml.jackson.core.type.TypeReference<List<org.fsa_2026.company_fsa_captone_2026.dto.QuizQuestionRequest>> listType =
                        new com.fasterxml.jackson.core.type.TypeReference<>() {};
                List<org.fsa_2026.company_fsa_captone_2026.dto.QuizQuestionRequest> questions =
                        objectMapper.convertValue(questionsRaw, listType);

                java.util.concurrent.atomic.AtomicInteger idx = new java.util.concurrent.atomic.AtomicInteger(1);
                
                // Pre-fetch challenges by skill type to avoid excessive DB calls during streaming
                java.util.Map<org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType, List<ChallengeBank>> cache = new java.util.HashMap<>();

                return questions.stream()
                        .sorted(java.util.Comparator.comparingInt(q ->
                                q.getQuestionOrder() != null ? q.getQuestionOrder() : 0))
                        .map(q -> {
                            ChallengeBank cb = null;
                            if (q.getChallengeId() != null) {
                                cb = challengeBankRepository.findById(q.getChallengeId()).orElse(null);
                            } else if (q.getSkillType() != null) {
                                try {
                                    org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType st = 
                                        org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType.valueOf(q.getSkillType().toUpperCase());
                                    
                                    List<ChallengeBank> pool = cache.computeIfAbsent(st, challengeBankRepository::findBySkillType);
                                    if (!pool.isEmpty()) {
                                        // Simple deterministic selection based on question order
                                        int poolIdx = (q.getQuestionOrder() != null ? q.getQuestionOrder() - 1 : 0) % pool.size();
                                        cb = pool.get(poolIdx);
                                    }
                                } catch (Exception ignored) {}
                            }
                            
                            return QuizChallengeItemResponse.builder()
                                    .orderIndex(idx.getAndIncrement())
                                    .challenge(cb)
                                    .build();
                        })
                        .filter(r -> r.getChallenge() != null)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                return java.util.Collections.<QuizChallengeItemResponse>emptyList();
            }
        }).orElse(java.util.Collections.emptyList());
    }

    @Transactional
    public ChallengeBank updateChallenge(UUID id, ChallengeBankRequest request) {
        ChallengeBank challenge = challengeBankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi với ID: " + id));
        
        if (request.getContentText() != null) challenge.setContentText(request.getContentText());
        if (request.getSkillType() != null) challenge.setSkillType(request.getSkillType());
        if (request.getDifficultyTag() != null) challenge.setDifficultyTag(request.getDifficultyTag());
        if (request.getRegion() != null) challenge.setRegion(request.getRegion());
        if (request.getMetadataJson() != null) challenge.setMetadataJson(request.getMetadataJson());
        
        return challengeBankRepository.save(challenge);
    }

    @Transactional
    public void deleteChallenge(UUID id) {
        // Remove from all quizzes first to avoid constraints
        quizChallengeItemRepository.deleteByChallengeBankId(id);
        quizChallengeItemRepository.deleteByChallengeId(id);
        challengeBankRepository.deleteById(id);
    }

    @Transactional
    public void removeChallengeFromQuiz(UUID quizId, UUID challengeId) {
        quizChallengeItemRepository.deleteByQuizIdAndChallengeBankId(quizId, challengeId);
        quizChallengeItemRepository.deleteByQuizIdAndChallengeId(quizId, challengeId);
    }
}
