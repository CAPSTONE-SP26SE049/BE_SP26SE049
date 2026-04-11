package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Quest;
import org.fsa_2026.company_fsa_captone_2026.service.QuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/educator/quests")
@RequiredArgsConstructor
@Tag(name = "Educator Quests", description = "Management of daily quests by Educators")
@PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")
public class EducatorQuestController {

    private final QuestService questService;

    @PostMapping
    @Operation(summary = "Create Quest", description = "Educator creates a new daily quest goal")
    public ResponseEntity<ApiResponse<Quest>> createQuest(@RequestBody Quest quest) {
        return ResponseEntity.ok(ApiResponse.success("Quest created", questService.createQuest(quest)));
    }

    @GetMapping
    @Operation(summary = "Get All Quests", description = "List all existing quests")
    public ResponseEntity<ApiResponse<List<Quest>>> getAllQuests() {
        return ResponseEntity.ok(ApiResponse.success("Fetched all quests", questService.getAllQuests()));
    }
}
