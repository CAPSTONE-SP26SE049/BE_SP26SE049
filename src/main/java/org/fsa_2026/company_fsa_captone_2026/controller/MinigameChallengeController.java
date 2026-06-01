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

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Bạn là một đối tác hội thoại AI thân thiện. Hãy trò chuyện với người học tiếng Việt dựa trên kịch bản và cặp âm cần luyện tập.\n");
        if (pairType != null) {
            promptBuilder.append("Cặp âm cần luyện tập: ").append(pairType).append("\n");
            promptBuilder.append("Hãy cố gắng sử dụng nhiều từ chứa cặp âm này trong phản hồi của bạn để người học có thể làm quen.\n");
        }
        promptBuilder.append("Hãy đóng vai tự nhiên, trả lời ngắn gọn (1-2 câu), thân thiện và khuyến khích người học trả lời tiếp.\n\n");
        
        if (history != null && !history.isEmpty()) {
            promptBuilder.append("Lịch sử cuộc trò chuyện:\n");
            for (Map<String, Object> msg : history) {
                promptBuilder.append("- ").append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
            }
        }
        promptBuilder.append("Tin nhắn mới nhất từ người học: ").append(message).append("\n");
        promptBuilder.append("Hãy trả về phản hồi của bạn dưới dạng JSON thuần túy có định dạng: {\"reply\": \"nội dung phản hồi của bạn\"}");

        Map<String, Object> aiResponse = aiService.chatWithGroq(promptBuilder.toString());
        return ResponseEntity.ok(ApiResponse.success("Thành công", aiResponse));
    }
}
