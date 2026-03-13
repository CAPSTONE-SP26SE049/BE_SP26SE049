package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * Challenge Entity
 * Bảng challenges - Quản lý các thử thách/bài tập
 */
@Entity
@Table(name = "challenge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Challenge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = false)
    private Level level;

    @Column(name = "type", nullable = false, length = 255)
    private String type;

    @Column(name = "content_text", nullable = false, columnDefinition = "text")
    private String contentText;

    @Column(name = "phonetic_transcription_ipa", nullable = false, length = 255)
    private String phoneticTranscriptionIpa;

    @Column(name = "reference_audio_url", nullable = false, length = 255)
    private String referenceAudioUrl;

    @Column(name = "focus_phonemes", nullable = false, columnDefinition = "text")
    private String focusPhonemes;

    // Relationships
    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attempt> attempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus status = org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.APPROVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Challenge parent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id")
    private Challenge draft;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
