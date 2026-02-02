package com.aiservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "challenges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Challenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "level_id")
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeType type;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "phonetic_transcription_ipa")
    private String phoneticTranscriptionIpa;

    @Column(name = "reference_audio_url")
    private String referenceAudioUrl;

    @Column(name = "focus_phonemes", columnDefinition = "TEXT")
    private String focusPhonemes; // Store as JSON string or handle with converter

    public enum ChallengeType {
        WORD, SENTENCE, CONVERSATION
    }
}
