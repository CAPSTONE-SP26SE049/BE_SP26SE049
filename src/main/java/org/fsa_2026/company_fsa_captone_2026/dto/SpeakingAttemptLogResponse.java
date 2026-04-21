package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingAttemptLogResponse {
    private UUID id;
    private String userEmail;
    private String userFullName;
    private String targetText;
    private String asrTranscription;
    private String audioUrl;
    private Integer geminiScore;
    private String geminiFeedback;
    private Boolean isCorrect;
    private String dialect;
    private Long processingTimeMs;   // Gemini latency
    private Long asrProcessingTimeMs; // Parakeet latency
    private Instant createdAt;
}
