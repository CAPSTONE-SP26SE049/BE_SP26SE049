package org.fsa_2026.company_fsa_captone_2026.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentItemRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuizChallengeItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DEBUG ONLY controller — no authentication required.
 * DELETE THIS CONTROLLER before production deployment.
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/public/debug")
@RequiredArgsConstructor
public class DebugController {

    private final ChallengeBankRepository challengeBankRepository;
    private final QuizChallengeItemRepository quizChallengeItemRepository;
    private final ContentItemRepository contentItemRepository;
    private final LearningUnitRepository learningUnitRepository;

    @GetMapping("/challenge-bank")
    public ResponseEntity<Map<String, Object>> getAllChallenges() {
        List<ChallengeBank> all = challengeBankRepository.findAll();
        Map<String, List<Map<String, Object>>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        cb -> cb.getSkillType() != null ? cb.getSkillType().name() : "NULL",
                        Collectors.mapping(cb -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("id", cb.getId());
                            item.put("contentText", cb.getContentText());
                            item.put("skillType", cb.getSkillType() != null ? cb.getSkillType().name() : null);
                            item.put("metadataJson", cb.getMetadataJson());
                            return item;
                        }, Collectors.toList())
                ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", all.size());
        result.put("data", grouped);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/content-items/level/{levelId}")
    public ResponseEntity<Map<String, Object>> getContentItemsByLevel(@PathVariable UUID levelId) {
        List<ContentItem> items = contentItemRepository.findByLearningUnitId(levelId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("levelId", levelId);
        response.put("count", items.size());
        response.put("items", items.stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", item.getId());
            m.put("title", item.getTitle());
            m.put("type", item.getType());
            m.put("metadataJson", item.getMetadataJson());
            m.put("itemsJson", item.getItemsJson());
            return m;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/content-items/type/{type}")
    public ResponseEntity<List<Map<String, Object>>> getContentItemsByType(@PathVariable String type) {
        List<ContentItem> items = type.equalsIgnoreCase("ALL") ? contentItemRepository.findAll() : contentItemRepository.findByType(type);
        return ResponseEntity.ok(items.stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", item.getId());
            m.put("title", item.getTitle());
            m.put("type", item.getType());
            m.put("levelId", item.getLearningUnit() != null ? item.getLearningUnit().getId() : null);
            m.put("metadataJson", item.getMetadataJson());
            m.put("itemsJson", item.getItemsJson());
            return m;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/learning-units/all")
    public ResponseEntity<List<Map<String, Object>>> getAllLearningUnits() {
        List<LearningUnit> units = learningUnitRepository.findAll();
        return ResponseEntity.ok(units.stream().map(lu -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", lu.getId());
            m.put("name", lu.getName());
            m.put("type", lu.getType());
            m.put("parentId", lu.getParent() != null ? lu.getParent().getId() : null);
            m.put("metadataJson", lu.getMetadataJson());
            return m;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/learning-units/{id}")
    public ResponseEntity<Map<String, Object>> getLearningUnitById(@PathVariable UUID id) {
        return learningUnitRepository.findById(id).map(lu -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", lu.getId());
            m.put("name", lu.getName());
            m.put("type", lu.getType());
            m.put("metadataJson", lu.getMetadataJson());
            return ResponseEntity.ok(m);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/quiz/{quizId}/full")
    public ResponseEntity<Map<String, Object>> getQuizFull(@PathVariable UUID quizId) {
        List<QuizChallengeItem> items = quizChallengeItemRepository.findByQuizIdOrderByOrderIndex(quizId);
        List<Map<String, Object>> resolved = items.stream().map(qci -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("quizChallengeItemId", qci.getId());
            row.put("challengeId", qci.getChallengeId());
            row.put("orderIndex", qci.getOrderIndex());
            UUID cbId = qci.getChallengeBankId() != null ? qci.getChallengeBankId() : qci.getChallengeId();
            if (cbId != null) {
                challengeBankRepository.findById(cbId).ifPresent(cb -> {
                    row.put("challenge_contentText", cb.getContentText());
                    row.put("challenge_skillType", cb.getSkillType());
                    row.put("challenge_metadataJson", cb.getMetadataJson());
                });
            }
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("quizId", quizId);
        result.put("challenges", resolved);
        return ResponseEntity.ok(result);
    }
}
