package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ErrorTagResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.PlacementRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ErrorTagService
 * Sau gộp bảng: ErrorTag được lưu trong bảng learning_unit với type = "ERROR_TAG"
 */
@Service
@RequiredArgsConstructor
public class ErrorTagService {

    private final LearningUnitRepository learningUnitRepository;
    private final PlacementRuleRepository placementRuleRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ErrorTagResponse> getErrorTagsByDialect(UUID dialectId) {
        if (dialectId == null) {
            return getAllErrorTags();
        }
        return placementRuleRepository.findByTargetDialectId(dialectId).stream()
                .map(rule -> ErrorTagResponse.fromEntity(rule.getErrorTag()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ErrorTagResponse> getAllErrorTags() {
        return learningUnitRepository.findByType("ERROR_TAG").stream()
                .map(ErrorTagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ErrorTagResponse createErrorTag(String tagCode, String name, String description) {
        boolean exists = learningUnitRepository.findByType("ERROR_TAG").stream()
                .anyMatch(u -> {
                    try {
                        if (u.getMetadataJson() != null) {
                            Map<?, ?> meta = objectMapper.readValue(u.getMetadataJson(), Map.class);
                            return tagCode.equals(meta.get("tag_code"));
                        }
                    } catch (Exception ignored) {}
                    return false;
                });

        if (exists) {
            throw new ApiException("CONFLICT", "Mã lỗi này đã tồn tại (Tag Code already exists)");
        }

        String metadataJson;
        try {
            Map<String, Object> metadata = Map.of(
                    "tag_code", tagCode,
                    "description", description != null ? description : ""
            );
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            metadataJson = "{}";
        }

        LearningUnit tag = LearningUnit.builder()
                .name(name)
                .type("ERROR_TAG")
                .metadataJson(metadataJson)
                .build();

        return ErrorTagResponse.fromEntity(learningUnitRepository.save(tag));
    }

    @Transactional
    public ErrorTagResponse updateErrorTag(UUID id, String tagCode, String name, String description) {
        LearningUnit tag = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy mã lỗi"));

        if (!"ERROR_TAG".equals(tag.getType())) {
            throw new ApiException("BAD_REQUEST", "ID không phải Error Tag");
        }

        try {
            Map<String, Object> metadata = tag.getMetadataJson() != null
                    ? objectMapper.readValue(tag.getMetadataJson(), Map.class)
                    : new java.util.HashMap<>();

            if (tagCode != null) {
                metadata.put("tag_code", tagCode);
            }
            if (description != null) {
                metadata.put("description", description);
            }
            tag.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "Lỗi khi cập nhật metadata");
        }

        if (name != null) {
            tag.setName(name);
        }

        return ErrorTagResponse.fromEntity(learningUnitRepository.save(tag));
    }

    @Transactional
    public void deleteErrorTag(UUID id) {
        LearningUnit tag = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy mã lỗi"));

        if (!"ERROR_TAG".equals(tag.getType())) {
            throw new ApiException("BAD_REQUEST", "ID không phải Error Tag");
        }

        boolean usedInPlacementRule = !placementRuleRepository.findAll().stream()
                .filter(r -> r.getErrorTag() != null && r.getErrorTag().getId().equals(id))
                .collect(Collectors.toList()).isEmpty();

        if (usedInPlacementRule) {
            throw new ApiException("PRECONDITION_FAILED", "Không thể xóa mã lỗi đang được dùng trong Placement Rules");
        }

        learningUnitRepository.deleteById(id);
    }
}
