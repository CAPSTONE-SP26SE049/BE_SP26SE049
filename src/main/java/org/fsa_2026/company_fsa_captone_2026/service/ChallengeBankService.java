package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeBankRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuizChallengeItemRepository;
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

    @Transactional
    public ChallengeBank createChallenge(ChallengeBankRequest request) {
        ChallengeBank challenge = ChallengeBank.builder()
                .contentText(request.getContentText())
                .skillType(request.getSkillType())
                .difficultyTag(request.getDifficultyTag())
                .isGlobal(request.getIsGlobal() != null ? request.getIsGlobal() : true)
                .metadataJson(request.getMetadataJson())
                .createdBy(request.getCreatedBy())
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
                .build()).collect(Collectors.toList());
        
        // Find current max order index if appending, or start from 1.
        // For now, let's just use the index in the provided list.
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setOrderIndex(i + 1);
        }

        return quizChallengeItemRepository.saveAll(items);
    }
}
