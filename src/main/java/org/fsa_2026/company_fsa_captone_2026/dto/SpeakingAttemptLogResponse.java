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
    @com.fasterxml.jackson.annotation.JsonProperty("groqScore")
    private Integer groqScore;

    @com.fasterxml.jackson.annotation.JsonProperty("groqFeedback")
    private String groqFeedback;
    private Boolean isCorrect;
    private String dialect;
    @com.fasterxml.jackson.annotation.JsonProperty("processingTimeMs")
    private Long processingTimeMs;   // Groq latency
    @com.fasterxml.jackson.annotation.JsonProperty("asrProcessingTimeMs")
    private Long asrProcessingTimeMs; // Parakeet latency
    
    @com.fasterxml.jackson.annotation.JsonProperty("asrScore")
    private Integer asrScore;
    
    @com.fasterxml.jackson.annotation.JsonProperty("wordDetails")
    private String wordDetails;
    
    @com.fasterxml.jackson.annotation.JsonProperty("recordId")
    private String recordId;

    private Instant createdAt;
}
