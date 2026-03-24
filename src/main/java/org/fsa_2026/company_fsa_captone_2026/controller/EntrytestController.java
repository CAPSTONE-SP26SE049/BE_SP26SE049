package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EntrytestRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.EntrytestResponse;
import org.fsa_2026.company_fsa_captone_2026.service.EntrytestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/entrytest")
@RequiredArgsConstructor
@Tag(name = "Entrytest", description = "Entrytest Speaking & Region Recommendation APIs")
public class EntrytestController {

    private final EntrytestService entrytestService;

    @PostMapping("/submit")
    @Operation(summary = "Submit Entrytest Audio", description = "Submit audio for accent classification and suggested region", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<EntrytestResponse>> submitEntrytest(
            @RequestBody EntrytestRequest request,
            Authentication authentication) {
        log.info("Evaluating entrytest accent for user: {}", authentication.getName());
        EntrytestResponse response = entrytestService.evaluateAccent(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Đánh giá giọng thành công", response));
    }

    @PutMapping("/select-region")
    @Operation(summary = "Select Region", description = "Update user region based on entrytest choice", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> selectRegion(
            @RequestParam String selectedRegion,
            Authentication authentication) {
        log.info("User {} selecting region: {}", authentication.getName(), selectedRegion);
        entrytestService.updateUserRegion(authentication.getName(), selectedRegion);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vùng miền thành công", null));
    }
}
