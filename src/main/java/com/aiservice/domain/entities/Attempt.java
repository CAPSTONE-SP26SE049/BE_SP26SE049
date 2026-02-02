package com.aiservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @Column(name = "audio_url")
    private String audioUrl;

    private int score;

    @Column(name = "ai_analysis_json", columnDefinition = "TEXT")
    private String aiAnalysisJson;

    @Column(name = "is_passed")
    private boolean isPassed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
