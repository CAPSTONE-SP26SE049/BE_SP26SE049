package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeBankRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizChallengeItemResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuizChallengeItemRepository;
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
    private final AccountRepository accountRepository;

    @Transactional
    public ChallengeBank createChallenge(ChallengeBankRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản để gán người tạo"));

        ChallengeBank challenge = ChallengeBank.builder()
                .contentText(request.getContentText())
                .skillType(request.getSkillType())
                .difficultyTag(request.getDifficultyTag())
                .isGlobal(request.getIsGlobal() != null ? request.getIsGlobal() : true)
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
        if (request.getDifficultyTag() != null) challenge.setDifficultyTag(request.getDifficultyTag());
        if (request.getIsGlobal() != null) challenge.setIsGlobal(request.getIsGlobal());
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
