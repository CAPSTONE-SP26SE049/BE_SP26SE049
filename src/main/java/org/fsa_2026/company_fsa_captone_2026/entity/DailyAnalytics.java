package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Daily Analytics Entity
 * Bảng daily_analytics - Lưu trữ dữ liệu thống kê theo ngày cho Admin Dashboard
 */
@Entity
@Table(name = "daily_analytics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DailyAnalytics extends BaseEntity {

    @Column(name = "record_date", nullable = false, unique = true)
    private LocalDate recordDate;

    @Column(name = "total_users", nullable = false)
    private Long totalUsers = 0L;

    @Column(name = "active_users", nullable = false)
    private Long activeUsers = 0L;

    @Column(name = "total_attempts", nullable = false)
    private Long totalAttempts = 0L;

    @Column(name = "average_score", nullable = false)
    private Double averageScore = 0.0;

    // Lưu trữ Json biểu đồ tỷ lệ lỗi theo vùng miền (N/L, S/X,...)
    @Column(name = "error_heatmaps_json", columnDefinition = "text")
    private String errorHeatmapsJson;
}
