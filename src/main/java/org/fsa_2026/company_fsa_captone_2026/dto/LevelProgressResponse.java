package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO cho tiến trình quiz trong 1 level của người chơi.
 * Hiển thị tổng quan level + chi tiết từng quiz đã chơi/chưa chơi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelProgressResponse implements Serializable {

    private UUID levelId;
    private String levelName;

    /** Tổng số quiz trong level */
    private int totalQuizzes;

    /** Số quiz đã hoàn thành (passed) */
    private int completedQuizzes;

    /** true nếu tất cả quiz đã hoàn thành */
    private boolean levelCompleted;

    /** Chi tiết từng quiz */
    private List<QuizProgressItem> quizzes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizProgressItem implements Serializable {
        private UUID quizId;
        private String quizName;

        /** Điểm cần đạt */
        private Integer passingScore;

        /** Đã hoàn thành chưa */
        private boolean completed;

        /** Điểm cao nhất đạt được (null nếu chưa chơi) */
        private Integer highestScore;

        /** Số sao (0 nếu chưa chơi) */
        private int starsEarned;

        /** Thành tựu gắn cho quiz (null nếu không có) */
        private String rewardName;
        private String rewardIconUrl;
        private UUID rewardCatalogId;

        /** Người chơi đã nhận thành tựu này chưa */
        private boolean rewardEarned;

        /** Loại kĩ năng (READING, LISTENING, SPEAKING, WRITING, MIXED) */
        private String skillType;

        /** Số thứ tự bài quiz trong level (1-indexed) */
        private Integer orderIndex;

        /** Số câu hỏi trong quiz */
        private Integer questionCount;
    }
}
