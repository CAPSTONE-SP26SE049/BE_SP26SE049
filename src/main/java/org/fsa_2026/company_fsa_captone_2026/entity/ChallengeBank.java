package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.DifficultyTag;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * ChallengeBank Entity - Lưu trữ kho câu hỏi cho ứng dụng học tiếng Việt.
 */
@Entity
@Table(name = "challenge_bank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeBank {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "content_text", columnDefinition = "TEXT", nullable = false)
    private String contentText;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", length = 50, nullable = false)
    private SkillType skillType;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_tag", length = 50, nullable = false)
    private DifficultyTag difficultyTag;

    @Builder.Default
    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = true;

    /**
     * Miền: BAC (Bắc), TRUNG (Trung), NAM (Nam).
     */
    @Builder.Default
    @Column(name = "region", length = 20)
    private String region = "BAC";
    /**
     * CỰC KỲ QUAN TRỌNG: Cột kiểu JSONB trong Postgres.
     * Lưu các dữ liệu linh hoạt như: options, correct_answer, audio_url, image_url.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadataJson;

    @Column(name = "created_by") // Khớp với cột created_by UUID bạn mới thêm
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
