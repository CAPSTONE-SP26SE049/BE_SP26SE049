package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.exception.BadRequestException;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelService {

    private final LearningUnitRepository learningUnitRepository;
    private final org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository accountLearningUnitRepository;
    private final AccountRepository accountRepository;
    private final org.fsa_2026.company_fsa_captone_2026.repository.CustomLearningPathRepository customLearningPathRepository;
    private final org.fsa_2026.company_fsa_captone_2026.repository.EntryTestResultRepository resultRepository;

    /**
     * Lấy danh sách level theo dialect — Fix U-01/U-02: parse UUID an toàn + bắt buộc dialect tồn tại trong DB.
     */
    @Transactional(readOnly = true)
    public List<LevelResponse> getLevelsByDialect(String dialectId) {
        UUID parentId = parseDialectIdOrThrow(dialectId);
        requireDialectLearningUnit(parentId);

        List<LearningUnit> allLevels = new java.util.ArrayList<>();
        fetchAllDescendantLevels(parentId, allLevels);

        return allLevels.stream()
                .map(LevelResponse::fromEntity)
                .filter(r -> r.getStatus() == null
                        || (!"REJECTED".equals(r.getStatus()) && !"DELETED".equals(r.getStatus())))
                .sorted(Comparator.comparingInt(r -> r.getLevelOrder() != null ? r.getLevelOrder() : 0))
                .collect(Collectors.toList());
    }

    private void fetchAllDescendantLevels(UUID parentId, List<LearningUnit> accumulator) {
        List<LearningUnit> children = learningUnitRepository.findByParentId(parentId);
        for (LearningUnit child : children) {
            if ("LEVEL".equals(child.getType())) {
                accumulator.add(child);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<LevelResponse> getUserRoadmap(String email, String dialectId, String type) {
        Account account = accountRepository.findByEmail(email).orElse(null);
        if (account == null)
            return List.of();

        // If explicitly requested custom path, or if no dialect is provided and user
        // HAS an active custom path
        if ("custom".equalsIgnoreCase(type)) {
            var activePath = customLearningPathRepository
                    .findFirstByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(account.getId());
            if (activePath.isPresent()) {
                log.info("Returning personalized roadmap for user {}", email);
                return getCustomPathLevels(activePath.get(), account);
            }
        }

        // Fix U-01/U-02: không nuốt lỗi dialect — propagate 400/404 từ getLevelsWithProgress
        if (dialectId != null && !dialectId.isEmpty()) {
            return getLevelsWithProgress(dialectId, account);
        }

        // Fallback: return all levels if no dialectId specified or invalid
        List<LevelResponse> levels = learningUnitRepository.findByType("LEVEL")
                .stream()
                .map(LevelResponse::fromEntity)
                .filter(r -> r.getStatus() == null
                        || (!"REJECTED".equals(r.getStatus()) && !"DELETED".equals(r.getStatus())))
                .sorted(java.util.Comparator.comparingInt(r -> r.getLevelOrder() != null ? r.getLevelOrder() : 0))
                .collect(java.util.stream.Collectors.toList());

        return populateProgressAndUnlocking(levels, account);
    }

    private List<LevelResponse> getCustomPathLevels(
            org.fsa_2026.company_fsa_captone_2026.entity.CustomLearningPath activePath, Account account) {
        List<LevelResponse> levels = activePath.getLevels().stream()
                .sorted(Comparator
                        .comparingInt(org.fsa_2026.company_fsa_captone_2026.entity.CustomPathLevel::getOrderIndex))
                .map(pl -> {
                    LevelResponse resp = LevelResponse.fromEntity(pl.getLevel());
                    resp.setLevelOrder(pl.getOrderIndex() + 1); // Display as 1-based index
                    return resp;
                })
                .collect(Collectors.toList());

        return populateProgressAndUnlocking(levels, account);
    }

    private List<LevelResponse> populateProgressAndUnlocking(List<LevelResponse> levels, Account account) {
        java.util.List<org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit> progressList = accountLearningUnitRepository
                .findByAccountIdWithLearningUnit(account.getId());

        java.util.Map<UUID, org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit> progressMap = progressList
                .stream()
                .collect(Collectors.toMap(
                        al -> al.getLearningUnit().getId(),
                        al -> al,
                        (existing, replacement) -> existing));

        // TỰ ĐỘNG KHÔI PHỤC: Lấy kết quả Entry Test gần nhất để đảm bảo mở khóa đúng lộ trình
        Optional<org.fsa_2026.company_fsa_captone_2026.entity.EntryTestResult> lastTest = resultRepository
                .findFirstByAccountIdOrderByCreatedAtDesc(account.getId());
        int entryTestUnlockedCount = 1;
        if (lastTest.isPresent()) {
            double score = lastTest.get().getOverallScore();
            if (score >= 100) entryTestUnlockedCount = 4;
            else if (score >= 80) entryTestUnlockedCount = 3;
            else if (score >= 60) entryTestUnlockedCount = 2;
        }

        boolean previousCompleted = true; // First level in any roadmap is unlocked by default

        for (int i = 0; i < levels.size(); i++) {
            LevelResponse level = levels.get(i);
            org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit progress = progressMap
                    .get(UUID.fromString(level.getId()));

            if (progress != null) {
                level.setStarsEarned(progress.getStarsEarned());
                level.setIsCompleted(progress.getIsCompleted());
            }

            // Logic mở khóa:
            // 1. Level 1 luôn mở
            // 2. Hoặc nếu i < entryTestUnlockedCount (Dựa trên Entry Test đã làm)
            // 3. Hoặc nếu level đã được mở khóa thủ công (isUnlocked = true)
            // 4. Hoặc Level n mở nếu Level n-1 đã hoàn thành (isCompleted = true)
            boolean isUnlockedManually = progress != null && progress.getIsUnlocked() != null
                    && progress.getIsUnlocked();
            boolean isUnlockedByTest = (i < entryTestUnlockedCount);

            level.setIsLocked(!previousCompleted && !isUnlockedManually && !isUnlockedByTest);

            // Cập nhật cho level tiếp theo
            previousCompleted = level.getIsCompleted() != null && level.getIsCompleted();
        }

        return levels;
    }

    @Transactional(readOnly = true)
    public List<LevelResponse> getLevelsWithProgress(String dialectId, Account account) {
        List<LevelResponse> levels = getLevelsByDialect(dialectId);
        return populateProgressAndUnlocking(levels, account);
    }

    // Fix U-02: Chuyển lỗi parse UUID Java thành 400 với message nghiệp vụ
    private UUID parseDialectIdOrThrow(String dialectId) {
        if (dialectId == null || dialectId.isBlank()) {
            throw new BadRequestException("Tham số dialectId là bắt buộc");
        }
        try {
            return UUID.fromString(dialectId.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("dialectId không hợp lệ. Vui lòng truyền UUID đúng định dạng.");
        }
    }

    // Fix U-01: dialectId phải trỏ tới learning_unit type DIALECT, không trả 200 rỗng khi sai ID
    private void requireDialectLearningUnit(UUID dialectUuid) {
        LearningUnit dialect = learningUnitRepository.findById(dialectUuid)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect"));
        if (!"DIALECT".equalsIgnoreCase(dialect.getType())) {
            throw new ApiException("NOT_FOUND", "Không tìm thấy Dialect");
        }
    }
}
