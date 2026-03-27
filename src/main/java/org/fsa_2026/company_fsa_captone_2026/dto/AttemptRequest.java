package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Attempt Request DTO
 * Đã cập nhật: bỏ quizAttemptId (QuizAttempt đã gộp vào StudySession)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptRequest implements Serializable {

    private String sessionId;
    private String challengeId;
    private String audioUrl;
    private Boolean isPassed;
}
