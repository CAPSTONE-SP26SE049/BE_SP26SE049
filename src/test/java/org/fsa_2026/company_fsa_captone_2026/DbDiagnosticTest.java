package org.fsa_2026.company_fsa_captone_2026;

import org.fsa_2026.company_fsa_captone_2026.entity.*;
import org.fsa_2026.company_fsa_captone_2026.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
public class DbDiagnosticTest {

    @Autowired
    private LearningUnitRepository learningUnitRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountLearningUnitRepository accountLearningUnitRepository;
    @Autowired
    private CustomLearningPathRepository customLearningPathRepository;
    @Autowired
    private QuizChallengeItemRepository quizChallengeItemRepository;
    @Autowired
    private ChallengeBankRepository challengeBankRepository;

    @Test
    @Transactional
    public void testGetDailyChallengesTrace() {
        System.out.println("=== START getDailyChallenges TRACE FOR tested_student ===");
        String email = "tested_student@fsa.com";
        
        System.out.println("Trace 1: Finding account by email...");
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) {
            System.out.println("Account not found!");
            return;
        }
        Account account = accountOpt.get();
        System.out.println("Account found: ID=" + account.getId() + ", region=" + account.getRegion());

        System.out.println("Trace 2: Finding DIALECT units...");
        List<LearningUnit> dialects = learningUnitRepository.findByType("DIALECT");
        System.out.println("Dialects count: " + dialects.size());
        LearningUnit matchedDialect = null;
        String region = account.getRegion();
        if (region != null) {
            String upperRegion = region.toUpperCase();
            for (LearningUnit d : dialects) {
                String nameUpper = d.getName().toUpperCase();
                if (upperRegion.contains("NORTH") || upperRegion.contains("BAC")) {
                    if (nameUpper.contains("BẮC") || nameUpper.contains("NORTH") || nameUpper.contains("BAC")) {
                        matchedDialect = d;
                        break;
                    }
                } else if (upperRegion.contains("CENTRAL") || upperRegion.contains("TRUNG")) {
                    if (nameUpper.contains("TRUNG") || nameUpper.contains("CENTRAL")) {
                        matchedDialect = d;
                        break;
                    }
                } else if (upperRegion.contains("SOUTH") || upperRegion.contains("NAM")) {
                    if (nameUpper.contains("NAM") || nameUpper.contains("SOUTH")) {
                        matchedDialect = d;
                        break;
                    }
                }
            }
        }
        if (matchedDialect == null && !dialects.isEmpty()) {
            matchedDialect = dialects.get(0);
        }
        System.out.println("Matched dialect: " + (matchedDialect != null ? matchedDialect.getName() + " (" + matchedDialect.getId() + ")" : "null"));

        System.out.println("Trace 3: Fetching progressList...");
        List<AccountLearningUnit> progressList = accountLearningUnitRepository.findByAccountId(account.getId());
        System.out.println("progressList size: " + progressList.size());
        
        System.out.println("Trace 4: Building progressMap...");
        Map<UUID, AccountLearningUnit> progressMap = progressList.stream()
                .collect(Collectors.toMap(
                        al -> al.getLearningUnit().getId(),
                        al -> al,
                        (existing, replacement) -> existing
                ));
        System.out.println("progressMap size: " + progressMap.size());

        LearningUnit activeLevel = null;

        System.out.println("Trace 5: Checking CustomLearningPath...");
        var customPathOpt = customLearningPathRepository.findFirstByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(account.getId());
        if (customPathOpt.isPresent()) {
            System.out.println("Custom learning path is present: ID=" + customPathOpt.get().getId());
            List<CustomPathLevel> customLevels = customPathOpt.get().getLevels();
            System.out.println("Custom levels count: " + customLevels.size());
            for (CustomPathLevel pl : customLevels) {
                AccountLearningUnit progress = progressMap.get(pl.getLevel().getId());
                if (progress == null || !Boolean.TRUE.equals(progress.getIsCompleted())) {
                    activeLevel = pl.getLevel();
                    break;
                }
            }
            if (activeLevel == null && !customLevels.isEmpty()) {
                activeLevel = customLevels.get(customLevels.size() - 1).getLevel();
            }
        } else {
            System.out.println("Custom learning path not present.");
        }
        System.out.println("Active level after custom path check: " + (activeLevel != null ? activeLevel.getName() : "null"));

        System.out.println("Trace 6: Checking matchedDialect roadmap...");
        if (activeLevel == null && matchedDialect != null) {
            List<LearningUnit> allLevels = new ArrayList<>();
            System.out.println("Fetching descendant levels...");
            fetchAllDescendantLevels(matchedDialect.getId(), allLevels);
            System.out.println("Descendant levels count: " + allLevels.size());
            
            // Sort (extract metadata)
            System.out.println("Sorting levels...");
            allLevels.sort(Comparator.comparingInt(this::extractLevelOrderFromMetadata));
            
            System.out.println("Finding active level...");
            for (LearningUnit lvl : allLevels) {
                AccountLearningUnit progress = progressMap.get(lvl.getId());
                if (progress == null || !Boolean.TRUE.equals(progress.getIsCompleted())) {
                    activeLevel = lvl;
                    break;
                }
            }
            if (activeLevel == null && !allLevels.isEmpty()) {
                activeLevel = allLevels.get(allLevels.size() - 1);
            }
        }
        System.out.println("Active level: " + (activeLevel != null ? activeLevel.getName() + " (" + activeLevel.getId() + ")" : "null"));

        System.out.println("Trace 7: Collecting speaking challenges...");
        List<ChallengeBank> speakingChallenges = new ArrayList<>();
        String regionFilter = "BAC";
        
        if (activeLevel != null) {
            System.out.println("activeLevel is not null. Finding parent dialect for region filtering...");
            LearningUnit current = activeLevel;
            while (current != null) {
                System.out.println(" - current: name=" + current.getName() + ", type=" + current.getType());
                if ("DIALECT".equalsIgnoreCase(current.getType())) {
                    String nameUpper = current.getName().toUpperCase();
                    if (nameUpper.contains("BẮC") || nameUpper.contains("NORTH") || nameUpper.contains("BAC")) {
                        regionFilter = "BAC";
                    } else if (nameUpper.contains("TRUNG") || nameUpper.contains("CENTRAL")) {
                        regionFilter = "TRUNG";
                    } else if (nameUpper.contains("NAM") || nameUpper.contains("SOUTH")) {
                        regionFilter = "NAM";
                    }
                    break;
                }
                current = current.getParent();
            }
            System.out.println("Region filter resolved: " + regionFilter);

            System.out.println("Finding QUIZ units under activeLevel...");
            List<LearningUnit> quizzes = learningUnitRepository.findByParentAndType(activeLevel, "QUIZ");
            System.out.println("Quizzes found count: " + quizzes.size());
            List<UUID> quizIds = quizzes.stream().map(LearningUnit::getId).collect(Collectors.toList());
            List<QuizChallengeItem> challengeItems = new ArrayList<>();
            for (UUID qid : quizIds) {
                System.out.println(" - Fetching challenge items for quiz ID: " + qid);
                challengeItems.addAll(quizChallengeItemRepository.findByQuizIdOrderByOrderIndex(qid));
            }
            System.out.println("Total challenge items collected: " + challengeItems.size());

            List<UUID> challengeIds = challengeItems.stream()
                    .map(item -> item.getChallengeBankId() != null ? item.getChallengeBankId() : item.getChallengeId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            System.out.println("Distinct challenge IDs: " + challengeIds.size());

            for (UUID cid : challengeIds) {
                System.out.println(" - Fetching challenge ID: " + cid);
                challengeBankRepository.findById(cid).ifPresent(c -> {
                    if (c.getSkillType() == org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType.SPEAKING) {
                        speakingChallenges.add(c);
                    }
                });
            }
            System.out.println("Collected speaking challenges count: " + speakingChallenges.size());
        }

        System.out.println("Trace 8: Completed getDailyChallenges trace!");
        System.out.println("=== END getDailyChallenges TRACE FOR tested_student ===");
    }

    private void fetchAllDescendantLevels(UUID parentId, List<LearningUnit> accumulator) {
        List<LearningUnit> children = learningUnitRepository.findByParentId(parentId);
        for (LearningUnit child : children) {
            if ("LEVEL".equals(child.getType())) {
                accumulator.add(child);
            }
        }
    }

    private int extractLevelOrderFromMetadata(LearningUnit level) {
        if (level.getMetadataJson() == null || level.getMetadataJson().isBlank()) return 0;
        return 0;
    }
}
