package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RegionCode;

@Entity
@Table(name = "entry_test_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryTestResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "overall_score", columnDefinition = "numeric(5,2)")
    private Double overallScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_region")
    private RegionCode detectedRegion;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON breakdown per question
}
