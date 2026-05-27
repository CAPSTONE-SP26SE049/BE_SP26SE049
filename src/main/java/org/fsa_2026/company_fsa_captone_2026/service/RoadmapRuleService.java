package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.ErrorCode;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.entity.RoadmapRule;
import org.fsa_2026.company_fsa_captone_2026.repository.RoadmapRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapRuleService {

    private final RoadmapRuleRepository roadmapRuleRepository;

    public List<RoadmapRule> getAllRules() {
        List<RoadmapRule> rules = roadmapRuleRepository.findByIsActiveTrueOrderByMinPercentAsc();
        if (rules.isEmpty()) {
            return getDefaultRules();
        }
        return rules;
    }

    @Transactional
    public RoadmapRule saveRule(org.fsa_2026.company_fsa_captone_2026.dto.RoadmapRuleRequest request) {
        if (request.getMinPercent() > request.getMaxPercent()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "minPercent phải nhỏ hơn hoặc bằng maxPercent");
        }
        
        RoadmapRule rule;
        if (request.getId() != null) {
            rule = roadmapRuleRepository.findById(request.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy quy tắc lộ trình với ID: " + request.getId()));
            rule.setMinPercent(request.getMinPercent());
            rule.setMaxPercent(request.getMaxPercent());
            rule.setDifficulties(request.getDifficulties());
            rule.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        } else {
            rule = RoadmapRule.builder()
                    .minPercent(request.getMinPercent())
                    .maxPercent(request.getMaxPercent())
                    .difficulties(request.getDifficulties())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();
        }
        return roadmapRuleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(UUID id) {
        roadmapRuleRepository.deleteById(id);
    }

    public List<String> getDifficultiesForScore(double score) {
        List<RoadmapRule> rules = roadmapRuleRepository.findByIsActiveTrueOrderByMinPercentAsc();
        
        if (rules.isEmpty()) {
            rules = getDefaultRules();
        }

        for (RoadmapRule rule : rules) {
            if (score >= rule.getMinPercent() && score <= rule.getMaxPercent()) {
                return Arrays.stream(rule.getDifficulties().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
        }

        // Default fallback if no range matches
        if (score <= 50) return List.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
        if (score <= 70) return List.of("INTERMEDIATE", "ADVANCED");
        return List.of("ADVANCED");
    }

    private List<RoadmapRule> getDefaultRules() {
        return List.of(
            RoadmapRule.builder().minPercent(0.0).maxPercent(50.0).difficulties("BEGINNER,INTERMEDIATE,ADVANCED").build(),
            RoadmapRule.builder().minPercent(50.1).maxPercent(70.0).difficulties("INTERMEDIATE,ADVANCED").build(),
            RoadmapRule.builder().minPercent(70.1).maxPercent(100.0).difficulties("ADVANCED").build()
        );
    }
    
    @Transactional
    public void resetToDefault() {
        roadmapRuleRepository.deleteAll();
        roadmapRuleRepository.saveAll(getDefaultRules());
    }
}
