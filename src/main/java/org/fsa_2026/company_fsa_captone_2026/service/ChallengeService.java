package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;

    @Transactional(readOnly = true)
    public List<ChallengeResponse> getChallengesByLevel(String levelId) {
        return challengeRepository.findByLevelId(UUID.fromString(levelId))
                .stream()
                .filter(challenge -> org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.APPROVED
                        .equals(challenge.getStatus()))
                .map(ChallengeResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
