package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Account sender;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "screenshot_url", columnDefinition = "TEXT")
    private String screenshotUrl;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;
}
