package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * SpeakingAttempt - Lưu trữ dữ liệu giọng nói của người dùng
 * cho mục đích thu thập dataset để huấn luyện mô hình AI.
 * Chỉ lưu khi người dùng đã đồng ý (consent_given = true).
 */
@Entity
@Table(name = "speaking_attempt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingAttempt {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /** Người dùng thực hiện */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** Câu hỏi liên kết (nullable nếu câu hỏi đã bị xóa) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = true)
    private ChallengeBank challenge;

    /** Văn bản mẫu cần đọc */
    @Column(name = "target_text", columnDefinition = "TEXT", nullable = false)
    private String targetText;

    /** Văn bản ASR nhận diện được từ giọng nói */
    @Column(name = "asr_transcription", columnDefinition = "TEXT")
    private String asrTranscription;

    /** URL audio trên Cloudinary */
    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    /** Điểm AI (0-100) */
    @Column(name = "gemini_score")
    private Integer geminiScore;

    /** Kết quả đúng/sai */
    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    /** Vùng miền (BAC, TRUNG, NAM, NORTH, CENTRAL, SOUTH) */
    @Column(name = "dialect", length = 20)
    private String dialect;

    /** Người dùng đã đồng ý thu thập dữ liệu */
    @Column(name = "consent_given", nullable = false)
    @Builder.Default
    private Boolean consentGiven = true;
}
