package org.fsa_2026.company_fsa_captone_2026.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.MinigameDtos.*;
import org.fsa_2026.company_fsa_captone_2026.service.MinigameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import org.fsa_2026.company_fsa_captone_2026.service.AIService;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MinigameChallengeController {

    private final MinigameService minigameService;
    private final AIService aiService;

    // ── Learner endpoints ──

    @GetMapping("/api/v1/minigames/word-challenges")
    public ResponseEntity<ApiResponse<List<MinigameChallengeResponse>>> getWordChallenges(
            @RequestParam(required = false) String pairType) {
        List<MinigameChallengeResponse> data = pairType != null
                ? minigameService.getByGameTypeAndPairType("WORD_CHALLENGE", pairType)
                : minigameService.getByGameType("WORD_CHALLENGE");
        return ResponseEntity.ok(ApiResponse.success("Thành công", data));
    }

    @GetMapping("/api/v1/minigames/sentence-completions")
    public ResponseEntity<ApiResponse<List<MinigameChallengeResponse>>> getSentenceCompletions(
            @RequestParam(required = false) String pairType) {
        List<MinigameChallengeResponse> data = pairType != null
                ? minigameService.getByGameTypeAndPairType("SENTENCE_COMPLETION", pairType)
                : minigameService.getByGameType("SENTENCE_COMPLETION");
        return ResponseEntity.ok(ApiResponse.success("Thành công", data));
    }

    @GetMapping("/api/v1/minigames/matching-pairs")
    public ResponseEntity<ApiResponse<List<MinigameChallengeResponse>>> getMatchingPairs(
            @RequestParam(required = false) String pairType) {
        List<MinigameChallengeResponse> data = pairType != null
                ? minigameService.getByGameTypeAndPairType("MATCHING_PAIRS", pairType)
                : minigameService.getByGameType("MATCHING_PAIRS");
        return ResponseEntity.ok(ApiResponse.success("Thành công", data));
    }

    @GetMapping("/api/v1/minigames/word-guess")
    public ResponseEntity<ApiResponse<List<MinigameChallengeResponse>>> getWordGuess(
            @RequestParam(required = false) String pairType) {
        List<MinigameChallengeResponse> data = pairType != null
                ? minigameService.getByGameTypeAndPairType("WORD_GUESS", pairType)
                : minigameService.getByGameType("WORD_GUESS");
        return ResponseEntity.ok(ApiResponse.success("Thành công", data));
    }

    @GetMapping("/api/v1/minigames/scenarios")
    public ResponseEntity<ApiResponse<List<MinigameChallengeResponse>>> getScenarios(
            @RequestParam(required = false) String pairType) {
        List<MinigameChallengeResponse> data = pairType != null
                ? minigameService.getByGameTypeAndPairType("CONVERSATION_SCENARIO", pairType)
                : minigameService.getByGameType("CONVERSATION_SCENARIO");
        return ResponseEntity.ok(ApiResponse.success("Thành công", data));
    }

    // ── Admin endpoints ──

    @GetMapping("/api/v1/admin/minigames")
    public ResponseEntity<ApiResponse<List<MinigameChallengeResponse>>> getAll(
            @RequestParam(required = false) String gameType,
            @RequestParam(required = false) String pairType) {
        List<MinigameChallengeResponse> data;
        if (gameType != null && pairType != null) {
            data = minigameService.getByGameTypeAndPairType(gameType, pairType);
        } else if (gameType != null) {
            data = minigameService.getByGameType(gameType);
        } else {
            data = minigameService.getAll();
        }
        return ResponseEntity.ok(ApiResponse.success("Thành công", data));
    }

    @PostMapping("/api/v1/admin/minigames")
    public ResponseEntity<ApiResponse<MinigameChallengeResponse>> create(
            @RequestBody MinigameChallengeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo câu hỏi thành công",
                        minigameService.create(request)));
    }

    @PutMapping("/api/v1/admin/minigames/{id}")
    public ResponseEntity<ApiResponse<MinigameChallengeResponse>> update(
            @PathVariable UUID id,
            @RequestBody MinigameChallengeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công",
                minigameService.update(id, request)));
    }

    @DeleteMapping("/api/v1/admin/minigames/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        minigameService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa câu hỏi", null));
    }

    @PostMapping("/api/v1/minigames/conversation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> conversationReply(
            @RequestBody Map<String, Object> request) {
        String pairType = (String) request.get("pairType");
        String message = (String) request.get("message");
        List<Map<String, Object>> history = (List<Map<String, Object>>) request.get("history");

        String systemInstruction = "Bạn là trợ lý học tập tiếng Việt đóng vai một đối tác hội thoại AI thân thiện và tự nhiên. " +
                "Nhiệm vụ của bạn là:\n" +
                "1. Trò chuyện tự nhiên với học viên tiếng Việt dựa trên kịch bản và lịch sử cuộc trò chuyện. Trả lời ngắn gọn (1-2 câu).\n" +
                "2. Phân tích tin nhắn mới nhất của người học để phát hiện các lỗi phát âm/chính tả liên quan đến cặp âm cần luyện tập (phụ âm bắt đầu bằng N hoặc L nếu cặp âm là N_L; S hoặc X nếu là S_X; D, GI, hoặc R nếu là D_GI_R; TR hoặc CH nếu là TR_CH).\n" +
                "QUAN TRỌNG:\n" +
                "- Nội dung phản hồi phải có ý nghĩa, bám ngữ cảnh và giúp cuộc trò chuyện tiếp tục tự nhiên. Tuyệt đối tránh phản hồi sáo rỗng.\n" +
                "- KHÔNG cố tình nhồi hoặc lặp lại hàng loạt từ chứa cặp âm đang luyện nếu không phù hợp ngữ cảnh.\n" +
                "- Hãy lọc ra danh sách các từ viết ĐÚNG chính tả chứa phụ âm cần luyện tập và xếp vào mảng \"correctWords\" (chỉ các từ bắt đầu bằng phụ âm đang luyện tập viết đúng chính tả, ví dụ: với N_L thì chỉ các từ bắt đầu bằng N hoặc L viết đúng chính tả mới được vào đây; từ \"chi\" không bắt đầu bằng N/L nên KHÔNG được nằm trong correctWords).\n" +
                "- Hãy tìm ra tất cả các từ viết SAI chính tả hoặc bị NGỌNG, LẪN LỘN giữa các phụ âm đang luyện tập (ví dụ: gõ/nói \"ninh\" thay vì \"linh\" trong cụm \"nấm linh chi\"; gõ \"lem\" thay vì \"nem\" trong \"nem lụi\"; gõ \"lường\" thay vì \"nướng\"...) và xếp vào mảng \"incorrectWords\".\n" +
                "- Bạn phải phân tích kỹ ngữ cảnh của từ đó trong câu để xác định xem người học có thực sự bị nhầm lẫn âm hay không.\n" +
                "- Một từ tuyệt đối KHÔNG được xuất hiện đồng thời ở cả hai danh sách \"correctWords\" và \"incorrectWords\". Nếu từ đó đã viết sai chính tả (ví dụ: \"ninh\" thay vì \"linh\"), nó chỉ được phép nằm trong \"incorrectWords\" và tuyệt đối không được xuất hiện trong \"correctWords\".\n" +
                "TUYỆT ĐỐI không sử dụng chữ viết Cyrillic (như нално, направо, налево...), tiếng Nga, tiếng Anh (nghiêm cấm các từ tiếng Anh thông dụng như 'lunch', 'dinner', 'cafe', 'party', 'bus'...) hay bất kỳ ngôn ngữ nào khác ngoài tiếng Việt. 100% nội dung phản hồi trong 'reply' phải là tiếng Việt chuẩn.\n" +
                "Hãy trả về phản hồi dưới dạng JSON thuần túy có định dạng chính xác sau:\n" +
                "{\n" +
                "  \"reply\": \"nội dung phản hồi tiếng Việt tự nhiên của bạn\",\n" +
                "  \"correctWords\": [\"từ_đúng_1\", \"từ_đúng_2\"],\n" +
                "  \"incorrectWords\": [\"từ_sai_1\", \"từ_sai_2\"]\n" +
                "}";

        StringBuilder promptBuilder = new StringBuilder();
        if (pairType != null) {
            promptBuilder.append("Cặp âm cần luyện tập: ").append(pairType).append("\n");
            promptBuilder.append("Ưu tiên sự tự nhiên của hội thoại. Chỉ dùng từ chứa cặp âm này khi thực sự phù hợp ngữ cảnh.\n");
        }
        promptBuilder.append("Hãy đóng vai tự nhiên, trả lời ngắn gọn (1-2 câu), thân thiện và khuyến khích người học trả lời tiếp.\n\n");
        
        if (history != null && !history.isEmpty()) {
            promptBuilder.append("Lịch sử cuộc trò chuyện:\n");
            for (Map<String, Object> msg : history) {
                promptBuilder.append("- ").append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
            }
        }
        promptBuilder.append("Tin nhắn mới nhất từ người học: ").append(message).append("\n");

        Map<String, Object> aiResponse = aiService.chatWithGroq(systemInstruction, promptBuilder.toString());
        return ResponseEntity.ok(ApiResponse.success("Thành công", aiResponse));
    }
}
