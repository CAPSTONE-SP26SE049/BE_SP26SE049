package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.SpeakingAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpeakingAttemptRepository extends JpaRepository<SpeakingAttempt, UUID> {

        @Query("SELECT sa FROM SpeakingAttempt sa WHERE sa.account.id = :accountId ORDER BY sa.createdAt DESC")
        List<SpeakingAttempt> findByAccountId(@Param("accountId") UUID accountId);

        /** Admin: lấy tất cả attempts, có thể lọc theo dialect */
        @Query("SELECT sa FROM SpeakingAttempt sa WHERE " +
                        "(:dialect IS NULL OR sa.dialect = :dialect) AND " +
                        "sa.consentGiven = true " +
                        "ORDER BY sa.createdAt DESC")
        Page<SpeakingAttempt> findAllByDialect(
                        @Param("dialect") String dialect,
                        Pageable pageable);

        /** Tổng số attempts theo dialect */
        long countByDialectAndConsentGivenTrue(String dialect);

        /** Đếm số attempts lỗi (score < threshold) theo dialect */
        long countByDialectAndConsentGivenTrueAndGeminiScoreLessThan(String dialect, Integer threshold);

        /** Đếm tổng số attempts có consent */
        long countByConsentGivenTrue();

        /** Đếm tổng số attempts có consent và có gemini score */
        @Query("SELECT COUNT(sa) FROM SpeakingAttempt sa WHERE sa.consentGiven = true AND sa.geminiScore IS NOT NULL")
        long countWithGeminiScoreAndConsentGivenTrue();

        /** Tính điểm trung bình gemini score trên các attempts có consent */
        @Query("SELECT COALESCE(AVG(sa.geminiScore), 0) FROM SpeakingAttempt sa WHERE sa.consentGiven = true AND sa.geminiScore IS NOT NULL")
        Double averageGeminiScoreWithConsentGivenTrue();

        /** Tính thời gian xử lý trung bình của AI */
        @Query("SELECT AVG(sa.processingTimeMs) FROM SpeakingAttempt sa WHERE sa.consentGiven = true AND sa.processingTimeMs IS NOT NULL")
        Double getAverageProcessingTimeMs();

        /** Tính tỷ lệ chính xác (isCorrect == true) */
        @Query("SELECT CASE WHEN COUNT(sa) > 0 THEN CAST(SUM(CASE WHEN sa.isCorrect = true THEN 1 ELSE 0 END) AS double) / COUNT(sa) ELSE 0.0 END FROM SpeakingAttempt sa WHERE sa.consentGiven = true")
        Double getAccuracyRate();

        /** Lấy N lượt gần nhất để hiển thị logs cho AI Monitor */
        @Query("SELECT sa FROM SpeakingAttempt sa JOIN FETCH sa.account ORDER BY sa.createdAt DESC")
        List<SpeakingAttempt> findTopNOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);
}
