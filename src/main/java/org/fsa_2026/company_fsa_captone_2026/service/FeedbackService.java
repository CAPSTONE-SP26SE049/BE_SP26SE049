package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.ErrorCode;
import org.fsa_2026.company_fsa_captone_2026.dto.FeedbackDtos.*;
import org.fsa_2026.company_fsa_captone_2026.entity.*;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

        private final EducatorFeedbackRepository feedbackRepository;
        private final SpeakingAttemptRepository attemptRepository;
        private final AccountRepository accountRepository;
        private final NotificationRepository notificationRepository;

        @Transactional
        public FeedbackResponse sendFeedback(CreateFeedbackRequest request, String educatorUsername) {
                Account educator = accountRepository.findByEmail(educatorUsername)
                                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                                                "Không tìm thấy giáo viên"));

                Account student;
                SpeakingAttempt attempt = null;

                if (request.getAttemptId() != null) {
                        attempt = attemptRepository.findById(request.getAttemptId())
                                        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                                                        "Không tìm thấy bài luyện tập"));
                        student = attempt.getAccount();
                } else {
                        student = accountRepository.findById(request.getStudentId())
                                        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                                                        "Không tìm thấy học viên"));
                }

                EducatorFeedback feedback = EducatorFeedback.builder()
                                .educator(educator)
                                .student(student)
                                .speakingAttempt(attempt)
                                .comment(request.getComment())
                                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                                .build();

                feedbackRepository.save(feedback);

                // Create notification for student
                Notification notification = Notification.builder()
                                .recipient(student)
                                .type("EDUCATOR_FEEDBACK")
                                .title("Bạn có nhận xét mới từ giáo viên")
                                .message(request.getComment())
                                .isRead(false)
                                .build();
                notificationRepository.save(notification);

                return FeedbackResponse.builder()
                                .id(feedback.getId())
                                .educatorId(educator.getId())
                                .educatorName(educator.getFullName())
                                .studentName(student.getFullName())
                                .comment(feedback.getComment())
                                .priority(feedback.getPriority())
                                .createdAt(feedback.getCreatedAt())
                                .attemptId(attempt != null ? attempt.getId() : null)
                                .targetText(attempt != null ? attempt.getTargetText() : null)
                                .build();
        }

        public List<FeedbackResponse> getStudentFeedbacks(UUID studentId) {
                return feedbackRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        public List<FeedbackResponse> getLearnerMailbox(String username) {
                Account student = accountRepository.findByEmail(username)
                                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                                                "Không tìm thấy người dùng"));

                return getStudentFeedbacks(student.getId());
        }

        private FeedbackResponse mapToResponse(EducatorFeedback f) {
                SpeakingAttempt sa = f.getSpeakingAttempt();
                return FeedbackResponse.builder()
                                .id(f.getId())
                                .educatorId(f.getEducator().getId())
                                .educatorName(f.getEducator().getFullName())
                                .studentName(f.getStudent().getFullName())
                                .comment(f.getComment())
                                .priority(f.getPriority())
                                .createdAt(f.getCreatedAt())
                                .attemptId(sa != null ? sa.getId() : null)
                                .targetText(sa != null ? sa.getTargetText() : null)
                                .audioUrl(sa != null ? sa.getAudioUrl() : null)
                                .groqScore(sa != null ? sa.getGroqScore() : null)
                                .groqFeedback(sa != null ? sa.getGroqFeedback() : null)
                                .asrTranscription(sa != null ? sa.getAsrTranscription() : null)
                                .build();
        }

        public List<SpeakingAttemptResponse> getRecentSpeakingAttempts(UUID studentId) {
                return attemptRepository.findByAccountId(studentId).stream()
                                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .limit(100)
                                .map(this::mapToAttemptResponse)
                                .collect(Collectors.toList());
        }

        private SpeakingAttemptResponse mapToAttemptResponse(SpeakingAttempt sa) {
                return SpeakingAttemptResponse.builder()
                                .id(sa.getId())
                                .targetText(sa.getTargetText())
                                .asrTranscription(sa.getAsrTranscription())
                                .audioUrl(sa.getAudioUrl())
                                .groqScore(sa.getGroqScore())
                                .groqFeedback(sa.getGroqFeedback())
                                .createdAt(sa.getCreatedAt())
                                .build();
        }
}
