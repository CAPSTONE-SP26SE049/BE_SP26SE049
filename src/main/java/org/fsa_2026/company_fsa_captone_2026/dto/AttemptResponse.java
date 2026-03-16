package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.SessionDetail;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Attempt Response DTO
 * Đã cập nhật: dùng SessionDetail thay cho Attempt (đã gộp bảng).
 * audioUrl và latencyMs được đọc từ attemptMetadataJson.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptResponse implements Serializable {

    private String id;
    private String sessionId;
    private String challengeId;
    private String audioUrl;
    private BigDecimal scoreOverall;
    private Boolean isPassed;
    private Integer latencyMs;
    private Instant createdAt;

    private List<PhonemeFeedbackResponse> feedback;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AttemptResponse fromEntity(SessionDetail detail) {
        if (detail == null)
            return null;

        String audioUrl = null;
        Integer latencyMs = null;
        try {
            if (detail.getAttemptMetadataJson() != null) {
                Map<String, Object> meta = objectMapper.readValue(detail.getAttemptMetadataJson(), Map.class);
                audioUrl = (String) meta.get("audioUrl");
                Object lms = meta.get("latencyMs");
                if (lms instanceof Number) {
                    latencyMs = ((Number) lms).intValue();
                }
            }
        } catch (Exception ignored) {
        }

        return AttemptResponse.builder()
                .id(detail.getId().toString())
                .sessionId(detail.getSession() != null ? detail.getSession().getId().toString() : null)
                .challengeId(detail.getContentItem() != null ? detail.getContentItem().getId().toString() : null)
                .audioUrl(audioUrl)
                .scoreOverall(detail.getScoreOverall())
                .isPassed(detail.getIsPassed())
                .latencyMs(latencyMs)
                .createdAt(detail.getCreatedAt())
                .build();
    }
}
