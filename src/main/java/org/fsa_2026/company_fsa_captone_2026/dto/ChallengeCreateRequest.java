package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import java.util.UUID;

/**
 * Request DTO for creating/updating a Challenge
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeCreateRequest implements Serializable {

    @NotNull(message = "Level ID không được để trống")
    private UUID levelId;

    @NotBlank(message = "Loại thử thách không được để trống")
    private String type;

    @NotBlank(message = "Nội dung văn bản không được để trống")
    private String contentText;

    @NotBlank(message = "Phiên âm IPA không được để trống")
    private String phoneticTranscriptionIpa;

    private String referenceAudioUrl;

    private String focusPhonemes;

    private String comment;
}
