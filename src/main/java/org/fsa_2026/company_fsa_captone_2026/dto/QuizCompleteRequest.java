package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    /** Điểm phần trăm (0-100), phải khớp với correctAnswers/totalQuestions */
    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score must be between 0 and 100")
    @Max(value = 100, message = "Score must be between 0 and 100")
    private Integer score;

    /** Số câu trả lời đúng */
    @Min(value = 0, message = "Correct answers cannot be negative")
    private Integer correctAnswers;

    /** Tổng số câu hỏi (mặc định 10 nếu null) */
    @Min(value = 1, message = "Total questions must be at least 1")
    private Integer totalQuestions;

    /** Thời gian làm bài (giây) */
    private Integer timeTakenSeconds;
}
