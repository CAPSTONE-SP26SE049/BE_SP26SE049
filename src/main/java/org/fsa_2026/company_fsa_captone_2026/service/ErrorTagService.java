package org.fsa_2026.company_fsa_captone_2026.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fsa_2026.company_fsa_captone_2026.dto.ErrorTagResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.PlacementRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * ErrorTagService
 * Sau gộp bảng: ErrorTag được lưu trong bảng learning_unit với type = "ERROR_TAG"
 */
@Service
@RequiredArgsConstructor
public class ErrorTagService {

    private static final String TYPE_ERROR_TAG = "ERROR_TAG";
    private static final String META_TAG_CODE = "tag_code";

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
        return learningUnitRepository.findByType(TYPE_ERROR_TAG).stream()
                .map(ErrorTagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ErrorTagResponse createErrorTag(String tagCode, String name, String description) {
        boolean exists = learningUnitRepository.findByType(TYPE_ERROR_TAG).stream()
                .anyMatch(u -> {
                    try {
                        if (u.getMetadataJson() != null) {
                            Map<?, ?> meta = objectMapper.readValue(u.getMetadataJson(), Map.class);
                            return tagCode.equals(meta.get(META_TAG_CODE));
                        }
                    } catch (JsonProcessingException | ClassCastException ignored) {
                        // Ignore malformed metadata entries and continue scanning.
                    }
                    return false;
                });

        if (exists) {
            throw new ApiException("CONFLICT", "Mã lỗi này đã tồn tại (Tag Code already exists)");
        }

        String metadataJson;
        try {
            Map<String, Object> metadata = Map.of(
                    META_TAG_CODE, tagCode,
                    "description", description != null ? description : ""
            );
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            metadataJson = "{}";
        }

        LearningUnit tag = LearningUnit.builder()
                .name(name)
                .type(TYPE_ERROR_TAG)
                .metadataJson(metadataJson)
                .build();

        return ErrorTagResponse.fromEntity(learningUnitRepository.save(tag));
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public ErrorTagResponse updateErrorTag(UUID id, String tagCode, String name, String description) {
        LearningUnit tag = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy mã lỗi"));

        if (!TYPE_ERROR_TAG.equals(tag.getType())) {
            throw new ApiException("BAD_REQUEST", "ID không phải Error Tag");
        }

        try {
            Map<String, Object> metadata = tag.getMetadataJson() != null
                    ? objectMapper.readValue(tag.getMetadataJson(), Map.class)
                    : new java.util.HashMap<>();

            if (tagCode != null) {
                metadata.put(META_TAG_CODE, tagCode);
            }
            if (description != null) {
                metadata.put("description", description);
            }
            tag.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
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

        if (!TYPE_ERROR_TAG.equals(tag.getType())) {
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
