package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fsa_2026.company_fsa_captone_2026.entity.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RegionCode;
import org.fsa_2026.company_fsa_captone_2026.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@SpringBootTest
public class RoadmapAssignmentTest {

    @Autowired
    private EntryTestService entryTestService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntryTestResultRepository entryTestResultRepository;

    @Autowired
    private CustomLearningPathRepository customLearningPathRepository;

    @Autowired
    private LearningUnitRepository learningUnitRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    @Rollback(false)
    public void dumpAllUnits() throws Exception {
        System.err.println("\n--- DUMP ALL LEARNING UNITS ---");
        List<LearningUnit> allUnits = learningUnitRepository.findAll();
        System.err.println("Total Units: " + allUnits.size());
        for (LearningUnit lu : allUnits) {
            System.err.println(String.format("Unit: ID=%s, Name=%s, Type=%s, Tag=%s, Level=%s", 
                lu.getId(), lu.getName(), lu.getType(), lu.getErrorTag(), lu.getDifficultyLevel()));
        }

        String realEmail = "hangul.notifications@gmail.com";
        Account account = accountRepository.findByEmail(realEmail).orElse(null);
        if (account != null) {
            Optional<EntryTestResult> resultOpt = entryTestResultRepository.findFirstByAccountIdOrderByCreatedAtDesc(account.getId());
            if (resultOpt.isPresent()) {
                EntryTestResult res = resultOpt.get();
                List<Map<String, Object>> stepResults = objectMapper.readValue(res.getDetails(), new TypeReference<List<Map<String, Object>>>() {});
                RegionCode region = res.getDetectedRegion() != null ? res.getDetectedRegion() : RegionCode.NORTH;
                
                System.err.println("Running assignPersonalRoadmap for " + realEmail);
                entryTestService.assignPersonalRoadmap(account, region, res, stepResults);
                
                Optional<CustomLearningPath> pathOpt = customLearningPathRepository.findFirstByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(account.getId());
                if (pathOpt.isPresent()) {
                    System.err.println("Path Levels: " + pathOpt.get().getLevels().size());
                } else {
                    System.err.println("No path created.");
                }
            }
        }
    }
}
