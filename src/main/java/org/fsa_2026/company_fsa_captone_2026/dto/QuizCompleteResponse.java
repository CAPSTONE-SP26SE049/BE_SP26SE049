package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Response DTO trả về sau khi người chơi hoàn thành quiz.
 * Bao gồm kết quả pass/fail và thông tin thành tựu nhận được (nếu có).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizCompleteResponse implements Serializable {

    /** true nếu score >= passing_score */
    private boolean passed;

    /** Điểm đạt được */
    private int score;

    /** Điểm cần đạt */
    private int passingScore;

    /** Số sao nhận được (1-3) */
    private int starsEarned;

    /** Thông tin thành tựu vừa nhận (null nếu không đạt hoặc quiz không gắn reward) */
    private RewardResponse earnedReward;

    /** true nếu đã nhận thành tựu này trước đó */
    private boolean rewardAlreadyEarned;

    /** TỔNG số sao hiện tại của user sau khi cộng */
    private int newTotalStars;

    /** TỔNG số XP hiện tại của user */
    private int newTotalXP;
}

