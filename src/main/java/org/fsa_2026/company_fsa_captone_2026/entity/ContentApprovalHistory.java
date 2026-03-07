package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus;
import java.util.UUID;

/**
 * Audit log for content approval and rejection
 */
@Entity
@Table(name = "content_approval_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentApprovalHistory extends BaseEntity {

    @Column(name = "content_type", length = 20, nullable = false)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ContentStatus status;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "content_snapshot", columnDefinition = "TEXT")
    private String contentSnapshot;
}
