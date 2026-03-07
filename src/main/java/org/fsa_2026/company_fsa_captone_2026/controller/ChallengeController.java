package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeResponse;
import org.fsa_2026.company_fsa_captone_2026.service.ChallengeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/challenges")
@RequiredArgsConstructor
@Tag(name = "Challenge", description = "Game Challenge APIs")
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping("/level/{levelId}")
    @Operation(summary = "Get Challenges by Level ID")
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getChallengesByLevel(
            @PathVariable String levelId) {

        log.info("Get challenges for level: {}", levelId);
        List<ChallengeResponse> challenges = challengeService.getChallengesByLevel(levelId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thử thách thành công", challenges));
    }
}
