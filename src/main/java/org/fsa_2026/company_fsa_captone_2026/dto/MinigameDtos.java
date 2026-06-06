package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class MinigameDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinigameChallengeRequest {
        private String gameType;
        private String pairType;
        private Map<String, Object> questionData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinigameChallengeResponse {
        private UUID id;
        private String gameType;
        private String pairType;
        private Map<String, Object> questionData;
        private Instant createdAt;
    }
}
