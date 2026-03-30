package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Request DTO khi người chơi hoàn thành quiz.
 * Game client gửi kết quả lên backend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizCompleteRequest implements Serializable {

    /** Điểm người chơi đạt được (0-100) */
    @NotNull(message = "Score is required")
    private Integer score;

    /** Số câu trả lời đúng */
    private Integer correctAnswers;

    /** Tổng số câu hỏi */
    private Integer totalQuestions;

    /** Thời gian làm bài (giây) */
    private Integer timeTakenSeconds;
}
