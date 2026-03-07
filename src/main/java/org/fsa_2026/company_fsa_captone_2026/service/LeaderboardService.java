package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.LeaderboardEntryResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getGlobalLeaderboard() {
        return userProfileRepository.findTop10ByOrderByTotalExperienceDesc()
                .stream()
                .map(LeaderboardEntryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getRegionalLeaderboard(String region) {
        if (region == null || region.isBlank()) {
            return getGlobalLeaderboard();
        }

        return userProfileRepository.findTop10ByAccountRegionOrderByTotalExperienceDesc(region.toUpperCase())
                .stream()
                .map(LeaderboardEntryResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
