package org.fsa_2026.company_fsa_captone_2026.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fsa_2026.company_fsa_captone_2026.dto.ErrorTagCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.ErrorTagResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
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
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ErrorTagResponse> getErrorTagsByDialect(UUID dialectId) {
        List<ErrorTagResponse> allTags = getAllErrorTags();
        if (dialectId == null) {
            return allTags;
        }
        
        LearningUnit dialect = learningUnitRepository.findById(dialectId).orElse(null);
        if (dialect != null && dialect.getName() != null) {
            String name = dialect.getName().toLowerCase();
            String region = null;
            if (name.contains("bắc") || name.contains("north")) region = "NORTH";
            else if (name.contains("trung") || name.contains("central")) region = "CENTRAL";
            else if (name.contains("nam") || name.contains("south")) region = "SOUTH";

            if (region != null) {
                final String r = region;
                return allTags.stream()
                        .filter(t -> t.getRegions() == null || t.getRegions().isEmpty() || t.getRegions().contains(r))
                        .collect(Collectors.toList());
            }
        }
        return allTags;
    }

    @Transactional(readOnly = true)
    public List<ErrorTagResponse> getAllErrorTags() {
        return learningUnitRepository.findByType(TYPE_ERROR_TAG).stream()
                .map(ErrorTagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ErrorTagResponse createErrorTag(ErrorTagCreateRequest request) {
        String tagCode = request.getTagCode();
        String name = request.getName();
        String description = request.getDescription();
        List<String> regions = request.getRegions();

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
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put(META_TAG_CODE, tagCode);
            metadata.put("description", description != null ? description : "");
            metadata.put("regions", regions != null ? regions : new java.util.ArrayList<String>());
            
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            metadataJson = "{}";
        }

        LearningUnit tag = LearningUnit.builder()
                .name(name)
                .type(TYPE_ERROR_TAG)
                .errorTag(tagCode) // NEW: Populate the dedicated column
                .metadataJson(metadataJson)
                .build();

        return ErrorTagResponse.fromEntity(learningUnitRepository.save(tag));
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public ErrorTagResponse updateErrorTag(UUID id, ErrorTagCreateRequest request) {
        String tagCode = request.getTagCode();
        String name = request.getName();
        String description = request.getDescription();
        List<String> regions = request.getRegions();

        LearningUnit tag = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy mã lỗi"));

        if (!TYPE_ERROR_TAG.equals(tag.getType())) {
            throw new ApiException("BAD_REQUEST", "ID không phải Error Tag");
        }

        if (tagCode != null) {
            tag.setErrorTag(tagCode); // NEW: Update the dedicated column
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
            if (regions != null) {
                metadata.put("regions", regions);
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

        learningUnitRepository.deleteById(id);
    }
}
