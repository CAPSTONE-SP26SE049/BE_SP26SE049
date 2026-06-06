package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.ErrorCode;
import org.fsa_2026.company_fsa_captone_2026.dto.MinigameDtos.*;
import org.fsa_2026.company_fsa_captone_2026.entity.MinigameChallenge;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.MinigameChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinigameService {

    private final MinigameChallengeRepository repository;

    public List<MinigameChallengeResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MinigameChallengeResponse> getByGameType(String gameType) {
        return repository.findByGameType(gameType).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MinigameChallengeResponse> getByGameTypeAndPairType(String gameType, String pairType) {
        return repository.findByGameTypeAndPairType(gameType, pairType).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public MinigameChallengeResponse create(MinigameChallengeRequest request) {
        MinigameChallenge entity = MinigameChallenge.builder()
                .gameType(request.getGameType())
                .pairType(request.getPairType())
                .questionData(request.getQuestionData())
                .build();
        return toResponse(repository.save(entity));
    }

    @Transactional
    public MinigameChallengeResponse update(UUID id, MinigameChallengeRequest request) {
        MinigameChallenge entity = repository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy câu hỏi"));
        entity.setGameType(request.getGameType());
        entity.setPairType(request.getPairType());
        entity.setQuestionData(request.getQuestionData());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy câu hỏi");
        }
        repository.deleteById(id);
    }

    private MinigameChallengeResponse toResponse(MinigameChallenge e) {
        return MinigameChallengeResponse.builder()
                .id(e.getId())
                .gameType(e.getGameType())
                .pairType(e.getPairType())
                .questionData(e.getQuestionData())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
