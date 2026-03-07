package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ErrorTagResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.ErrorTagRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LevelRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.PlacementRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ErrorTagService {
    private final ErrorTagRepository errorTagRepository;
    private final PlacementRuleRepository placementRuleRepository;
    private final LevelRepository levelRepository;

    @Transactional(readOnly = true)
    public List<ErrorTagResponse> getErrorTagsByDialect(UUID dialectId) {
        if (dialectId == null) {
            return getAllErrorTags();
        }
        return placementRuleRepository.findByTargetDialectId(dialectId).stream()
                .map(rule -> rule.getErrorTag())
                .distinct()
                .map(ErrorTagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ErrorTagResponse> getAllErrorTags() {
        return errorTagRepository.findAllWithRegions().stream()
                .map(ErrorTagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ErrorTagResponse createErrorTag(String tagCode, String name, String description) {
        if (errorTagRepository.findByTagCode(tagCode).isPresent()) {
            throw new ApiException("CONFLICT", "Mã lỗi này đã tồn tại (Tag Code already exists)");
        }
        ErrorTag tag = ErrorTag.builder()
                .tagCode(tagCode)
                .name(name)
                .description(description)
                .build();
        return ErrorTagResponse.fromEntity(errorTagRepository.save(tag));
    }

    @Transactional
    public ErrorTagResponse updateErrorTag(UUID id, String tagCode, String name, String description) {
        ErrorTag tag = errorTagRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy mã lỗi"));

        if (tagCode != null && !tagCode.equals(tag.getTagCode())) {
            if (errorTagRepository.findByTagCode(tagCode).isPresent()) {
                throw new ApiException("CONFLICT", "Mã lỗi mới đã tồn tại");
            }
            tag.setTagCode(tagCode);
        }

        if (name != null)
            tag.setName(name);
        if (description != null)
            tag.setDescription(description);

        return ErrorTagResponse.fromEntity(errorTagRepository.save(tag));
    }

    @Transactional
    public void deleteErrorTag(UUID id) {
        if (!errorTagRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Không tìm thấy mã lỗi");
        }

        if (levelRepository.existsByErrorTagId(id)) {
            throw new ApiException("PRECONDITION_FAILED", "Không thể xóa mã lỗi đang được sử dụng trong các Level");
        }

        errorTagRepository.deleteById(id);
    }
}
