package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.CustomPathDtos.*;
import org.fsa_2026.company_fsa_captone_2026.entity.*;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomLearningPathService {

        private final CustomLearningPathRepository pathRepository;
        private final CustomPathProgressRepository progressRepository;
        private final AccountRepository accountRepository;
        private final LearningUnitRepository learningUnitRepository;

        private static final String NOT_FOUND = "NOT_FOUND";
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        /** Parse order (level_order or orderIndex) from metadataJson safely */
        @SuppressWarnings("unchecked")
        private int parseLevelOrder(LearningUnit unit) {
                try {
                        if (unit.getMetadataJson() != null) {
                                Map<String, Object> meta = OBJECT_MAPPER.readValue(unit.getMetadataJson(), Map.class);
                                Object order = meta.get("orderIndex");
                                if (order == null)
                                        order = meta.get("level_order");

                                if (order instanceof Number)
                                        return ((Number) order).intValue();
                                if (order instanceof String)
                                        return Integer.parseInt((String) order);
                        }
                } catch (Exception ignored) {
                }
                return 0;
        }

        /** Parse skillType from metadataJson safely */
        @SuppressWarnings("unchecked")
        private String parseSkillType(LearningUnit unit) {
                if (unit.getMetadataJson() != null && !unit.getMetadataJson().isBlank()) {
                        try {
                                Map<String, Object> meta = OBJECT_MAPPER.readValue(unit.getMetadataJson(), Map.class);
                                Object st = meta.get("skill_type");
                                if (st == null)
                                        st = meta.get("skillType");
                                if (st instanceof String && !((String) st).isBlank()) {
                                        return ((String) st).toUpperCase();
                                }
                        } catch (Exception ignored) {
                        }
                }
                return inferSkillTypeFromName(unit.getName());
        }

        private String inferSkillTypeFromName(String name) {
                if (name == null || name.isBlank())
                        return "MIXED";
                String lower = name.toLowerCase();
                if (lower.contains("viết") || lower.contains("writ"))
                        return "WRITING";
                if (lower.contains("nghe") || lower.contains("listen"))
                        return "LISTENING";
                if (lower.contains("nói") || lower.contains("speak") || lower.contains("phát âm"))
                        return "SPEAKING";
                if (lower.contains("đọc") || lower.contains("read"))
                        return "READING";
                return "MIXED";
        }

        @Transactional
        public CustomPathResponse createCustomPath(String educatorEmail, UUID studentId,
                        CreateCustomPathRequest request) {
                Account educator = accountRepository.findByEmail(educatorEmail)
                                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy tài khoản giáo viên"));
                Account student = accountRepository.findById(studentId)
                                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy tài khoản học viên"));

                // Deactivate existing paths for this student
                List<CustomLearningPath> existingPaths = pathRepository.findByStudentIdAndIsActiveTrue(studentId);
                existingPaths.forEach(p -> p.setIsActive(false));
                pathRepository.saveAll(existingPaths);

                CustomLearningPath path = CustomLearningPath.builder()
                                .student(student)
                                .educator(educator)
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .isActive(true)
                                .build();

                for (int i = 0; i < request.getLevelIds().size(); i++) {
                        UUID levelId = request.getLevelIds().get(i);
                        LearningUnit level = learningUnitRepository.findById(levelId)
                                        .orElseThrow(() -> new ApiException(NOT_FOUND,
                                                        "Không tìm thấy Chương với ID: " + levelId));
                        path.addLevel(level, i);
                }

                CustomLearningPath savedPath = pathRepository.save(path);
                return mapToResponse(savedPath);
        }

        @Transactional(readOnly = true)
        public CustomPathResponse getActivePathForStudent(UUID studentId) {
                return pathRepository.findFirstByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(studentId)
                                .map(this::mapToResponse)
                                .orElseThrow(() -> new ApiException(NOT_FOUND, "Học viên chưa có lộ trình tùy chỉnh"));
        }

        @Transactional(readOnly = true)
        public List<CustomPathResponse> getPathsByEducator(String educatorEmail) {
                Account educator = accountRepository.findByEmail(educatorEmail)
                                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy tài khoản giáo viên"));
                return pathRepository.findByEducatorId(educator.getId()).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public void submitProgress(UUID studentId, UUID quizId, Integer score) {
                CustomLearningPath path = pathRepository
                                .findFirstByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(studentId)
                                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy lộ trình học của bạn"));

                CustomPathProgress progress = progressRepository
                                .findByCustomPathIdAndLearningUnitId(path.getId(), quizId)
                                .orElseGet(() -> CustomPathProgress.builder()
                                                .customPath(path)
                                                .learningUnit(learningUnitRepository.findById(quizId)
                                                                .orElseThrow(() -> new ApiException(NOT_FOUND,
                                                                                "Không tìm thấy bài Quiz")))
                                                .isCompleted(false)
                                                .score(0)
                                                .build());

                if (score > progress.getScore()) {
                        progress.setScore(score);
                }

                // Assume score >= 80 is pass/complete
                if (score >= 80) {
                        progress.setIsCompleted(true);
                }

                progressRepository.save(progress);
        }

        @Transactional
        public void createAutoPath(Account student, LearningUnit dialect) {
                List<CustomLearningPath> existingPaths = pathRepository.findByStudentIdAndIsActiveTrue(student.getId());
                existingPaths.forEach(p -> p.setIsActive(false));
                pathRepository.saveAll(existingPaths);

                Account educator = accountRepository.findAll().stream()
                                .filter(a -> a.getRoleCode() == org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode.ADMIN)
                                .findFirst()
                                .orElseThrow(() -> new ApiException(NOT_FOUND,
                                                "Không tìm thấy tài khoản quản trị hệ thống"));

                CustomLearningPath path = CustomLearningPath.builder()
                                .student(student)
                                .educator(educator)
                                .title("Lộ trình cá nhân hóa - " + dialect.getName())
                                .description("Lộ trình học tập tự động dựa trên kết quả Entry Test cho miền "
                                                + dialect.getName())
                                .isActive(true)
                                .build();

                List<LearningUnit> levels = learningUnitRepository.findByParentAndType(dialect, "LEVEL");
                levels.sort(Comparator.comparingInt(this::parseLevelOrder));

                for (int i = 0; i < levels.size(); i++) {
                        path.addLevel(levels.get(i), i);
                }

                pathRepository.save(path);
        }

        private CustomPathResponse mapToResponse(CustomLearningPath path) {
                List<CustomPathProgress> progressList = progressRepository.findByCustomPathId(path.getId());
                Map<UUID, CustomPathProgress> progressMap = progressList.stream()
                                .collect(Collectors.toMap(p -> p.getLearningUnit().getId(), p -> p, (a, b) -> a));

                List<PathLevelResponse> levelResponses = path.getLevels().stream()
                                .map(pl -> {
                                        LearningUnit level = pl.getLevel();
                                        List<LearningUnit> quizzes = learningUnitRepository
                                                        .findByParentId(level.getId());

                                        List<PathQuizResponse> quizResponses = quizzes.stream()
                                                        .sorted(Comparator.comparingInt(this::parseLevelOrder))
                                                        .map(q -> {
                                                                CustomPathProgress p = progressMap.get(q.getId());
                                                                return PathQuizResponse.builder()
                                                                                .quizId(q.getId())
                                                                                .title(q.getName())
                                                                                .orderIndex(parseLevelOrder(q))
                                                                                .skillType(parseSkillType(q))
                                                                                .score(p != null ? p.getScore() : 0)
                                                                                .isCompleted(p != null
                                                                                                ? p.getIsCompleted()
                                                                                                : false)
                                                                                .rewardName(q.getRewardCatalog() != null
                                                                                                ? q.getRewardCatalog()
                                                                                                                .getName()
                                                                                                : null)
                                                                                .rewardIconUrl(q.getRewardCatalog() != null
                                                                                                ? q.getRewardCatalog()
                                                                                                                .getIconUrl()
                                                                                                : null)
                                                                                .build();
                                                        }).collect(Collectors.toList());

                                        return PathLevelResponse.builder()
                                                        .levelId(level.getId())
                                                        .levelName(level.getName())
                                                        .region(level.getParent() != null ? level.getParent().getName()
                                                                        : "Unknown")
                                                        .orderIndex(pl.getOrderIndex())
                                                        .quizzes(quizResponses)
                                                        .build();
                                })
                                .sorted(Comparator.comparing(PathLevelResponse::getOrderIndex))
                                .collect(Collectors.toList());

                return CustomPathResponse.builder()
                                .id(path.getId())
                                .title(path.getTitle())
                                .description(path.getDescription())
                                .isActive(path.getIsActive())
                                .createdAt(path.getCreatedAt())
                                .levels(levelResponses)
                                .build();
        }
}
