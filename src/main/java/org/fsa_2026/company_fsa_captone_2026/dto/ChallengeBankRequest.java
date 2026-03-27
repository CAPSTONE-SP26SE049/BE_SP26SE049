package org.fsa_2026.company_fsa_captone_2026.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.DifficultyTag;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin tạo câu hỏi trong kho", example = """
    {
      "contentText": "Chọn từ đúng chính tả để điền vào chỗ trống: 'Con ... đang ăn cỏ'",
      "skillType": "READING",
      "difficultyTag": "BEGINNER",
      "region": "BAC",
      "metadataJson": {
        "options": ["nợn", "lợn", "lộn"],
        "correctAnswer": "lợn",
        "imageUrl": "",
        "hint": "Hãy chú ý âm đầu 'L' hay 'N'"
      }
    }
    """)
public class ChallengeBankRequest {
    private String contentText;
    private SkillType skillType;
    private DifficultyTag difficultyTag;
    private String region; // BAC, TRUNG, NAM
    private java.util.UUID levelId;
    private Map<String, Object> metadataJson;
}
