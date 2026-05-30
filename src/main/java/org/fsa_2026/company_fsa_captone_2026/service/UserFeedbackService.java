package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.ErrorCode;
import org.fsa_2026.company_fsa_captone_2026.dto.UserFeedbackDtos.*;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.UserFeedback;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.UserFeedbackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFeedbackService {

    private final UserFeedbackRepository userFeedbackRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public UserFeedbackResponse createFeedback(CreateUserFeedbackRequest request, String username) {
        Account sender = accountRepository.findByEmail(username)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));

        UserFeedback feedback = UserFeedback.builder()
                .sender(sender)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .screenshotUrl(request.getScreenshotUrl())
                .status("PENDING")
                .build();

        userFeedbackRepository.save(feedback);
        return mapToResponse(feedback);
    }

    public List<UserFeedbackResponse> getMyFeedbacks(String username) {
        Account sender = accountRepository.findByEmail(username)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));

        return userFeedbackRepository.findBySenderIdOrderByCreatedAtDesc(sender.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PagedFeedbackResponse getAllFeedbacks(String status, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserFeedback> result = userFeedbackRepository.findAllWithFilters(
                (status != null && !status.isBlank()) ? status : null,
                (category != null && !category.isBlank()) ? category : null,
                pageable);

        return PagedFeedbackResponse.builder()
                .content(result.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public UserFeedbackResponse updateStatus(UUID id, UpdateFeedbackStatusRequest request) {
        UserFeedback feedback = userFeedbackRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phản hồi"));

        feedback.setStatus(request.getStatus());
        if (request.getAdminNote() != null) {
            feedback.setAdminNote(request.getAdminNote());
        }

        userFeedbackRepository.save(feedback);
        return mapToResponse(feedback);
    }

    private UserFeedbackResponse mapToResponse(UserFeedback f) {
        return UserFeedbackResponse.builder()
                .id(f.getId())
                .senderId(f.getSender().getId())
                .senderName(f.getSender().getFullName())
                .senderEmail(f.getSender().getEmail())
                .category(f.getCategory())
                .title(f.getTitle())
                .content(f.getContent())
                .status(f.getStatus())
                .screenshotUrl(f.getScreenshotUrl())
                .adminNote(f.getAdminNote())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
