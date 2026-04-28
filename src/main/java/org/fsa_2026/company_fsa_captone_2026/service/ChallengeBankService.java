package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeBankRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizChallengeItemResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentItemRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuizChallengeItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeBankService {

    private final ChallengeBankRepository challengeBankRepository;
    private final QuizChallengeItemRepository quizChallengeItemRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final ContentItemRepository contentItemRepository;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;
    private final QuizService quizService;
    private final TTSService ttsService;

    @Transactional
    public ChallengeBank createChallenge(ChallengeBankRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản để gán người tạo"));

        Map<String, Object> metadata = request.getMetadataJson();
        if (metadata == null) metadata = new LinkedHashMap<>();
        
        // Auto-generate audio if missing for Listening/Speaking
        ensureAudioUrl(request.getSkillType(), request.getRegion(), metadata);

        ChallengeBank challenge = ChallengeBank.builder()
                .contentText(request.getContentText())
                .skillType(request.getSkillType())
                .region(request.getRegion() != null ? request.getRegion() : "BAC")
                .metadataJson(metadata)
                .createdBy(account.getId())
                .build();
        return challengeBankRepository.save(challenge);
    }
    @Transactional(readOnly = true)
    public List<ChallengeBank> getAllChallenges() {
        return challengeBankRepository.findAll();
    }

    @Transactional
    public Map<String, Object> assignChallengesToQuiz(UUID quizId, List<UUID> challengeIds) {
        // Create assignment items (append behavior kept as-is)
        List<QuizChallengeItem> items = challengeIds.stream().map(cid -> QuizChallengeItem.builder()
                .quizId(quizId)
                .challengeBankId(cid)
                .challengeId(cid) // Map to both columns to satisfy legacy DB constraint
                .build()).collect(Collectors.toList());

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setOrderIndex(i + 1);
        }

        List<QuizChallengeItem> saved = quizChallengeItemRepository.saveAll(items);

        // Recalculate quiz scoring metadata right after question update
        Map<String, Object> scoring = quizService.recalculateQuizScoring(quizId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("assignedItems", saved);
        response.put("scoring", scoring);
        return response;
    }
    @Transactional(readOnly = true)
    public List<QuizChallengeItemResponse> getChallengesByQuizId(UUID quizId) {
        // Primary source: quiz_challenge_item table (explicit assignment)
        List<QuizChallengeItem> items = quizChallengeItemRepository.findByQuizIdOrderByOrderIndex(quizId);

        return items.stream()
                .sorted((a, b) -> {
                    Integer aOrder = a.getOrderIndex() == null ? Integer.MAX_VALUE : a.getOrderIndex();
                    Integer bOrder = b.getOrderIndex() == null ? Integer.MAX_VALUE : b.getOrderIndex();
                    return Integer.compare(aOrder, bOrder);
                })
                .map(item -> {
                    UUID challengeId = item.getChallengeBankId() != null ? item.getChallengeBankId() : item.getChallengeId();
                    return QuizChallengeItemResponse.builder()
                            .orderIndex(item.getOrderIndex())
                            .challenge(challengeId != null ? challengeBankRepository.findById(challengeId).orElse(null) : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ChallengeBank updateChallenge(UUID id, ChallengeBankRequest request) {
        ChallengeBank challenge = challengeBankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi với ID: " + id));
        
        if (request.getContentText() != null) challenge.setContentText(request.getContentText());
        if (request.getSkillType() != null) challenge.setSkillType(request.getSkillType());
        if (request.getRegion() != null) challenge.setRegion(request.getRegion());
        
        if (request.getMetadataJson() != null) {
            Map<String, Object> metadata = request.getMetadataJson();
            ensureAudioUrl(challenge.getSkillType(), challenge.getRegion(), metadata);
            challenge.setMetadataJson(metadata);
        }
        
        return challengeBankRepository.save(challenge);
    }



    @Transactional
    public void deleteChallenge(UUID id) {
        // Check if this challenge is used in any quiz
        List<QuizChallengeItem> linkedQuizzes = quizChallengeItemRepository.findByChallengeId(id);
        if (!linkedQuizzes.isEmpty()) {
            throw new RuntimeException("Không thể xóa câu hỏi này vì đang được sử dụng trong bài kiểm tra. Vui lòng gỡ khỏi các bài kiểm tra trước.");
        }
        
        // Remove mappings if any (just in case they exist under challengeBankId column)
        quizChallengeItemRepository.deleteByChallengeBankId(id);
        challengeBankRepository.deleteById(id);
    }

    @Transactional
    public Map<String, Object> removeChallengeFromQuiz(UUID quizId, UUID challengeId) {
        quizChallengeItemRepository.deleteByQuizIdAndChallengeBankId(quizId, challengeId);
        quizChallengeItemRepository.deleteByQuizIdAndChallengeId(quizId, challengeId);

        Map<String, Object> scoring = quizService.recalculateQuizScoring(quizId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("removedChallengeId", challengeId);
        response.put("scoring", scoring);
        return response;
    }

    private void ensureAudioUrl(SkillType skillType, String region, Map<String, Object> metadata) {
        if (skillType == SkillType.LISTENING || skillType == SkillType.SPEAKING || skillType == SkillType.ENTRY_TEST) {
            String transcript = (String) metadata.get("transcript");
            String audioUrl = (String) metadata.get("audioUrl");

            if (transcript != null && !transcript.isBlank() && (audioUrl == null || audioUrl.isBlank())) {
                try {
                    String voice = "banmai"; // North
                    if ("TRUNG".equalsIgnoreCase(region) || "CENTRAL".equalsIgnoreCase(region)) {
                        voice = "hue";
                    } else if ("NAM".equalsIgnoreCase(region) || "SOUTH".equalsIgnoreCase(region)) {
                        voice = "linh";
                    }

                    Map<String, Object> ttsRes = ttsService.synthesize(transcript, voice);
                    if (ttsRes != null && ttsRes.containsKey("async")) {
                        metadata.put("audioUrl", ttsRes.get("async"));
                        log.info("Auto-generated audio for challenge: {}", ttsRes.get("async"));
                    }
                } catch (Exception e) {
                    log.error("Failed to auto-generate audio in ensureAudioUrl: {}", e.getMessage());
                }
            }
        }
    }
}
