package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizQuestionRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final LearningUnitRepository learningUnitRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public LearningUnit createQuiz(QuizCreateRequest request) {
        LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        if (!"LEVEL".equalsIgnoreCase(level.getType())) {
            throw new ApiException("INVALID_PARENT", "Parent phải là Level");
        }

        LearningUnit quiz = LearningUnit.builder()
                .parent(level)
                .name(request.getTitle())
                .type("QUIZ")
                .build();

        Map<String, Object> metadata = buildQuizMetadata(request);
        try {
            quiz.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            throw new ApiException("INVALID_METADATA", "Quiz metadata không hợp lệ");
        }

        return learningUnitRepository.save(quiz);
    }

    @Transactional
    public LearningUnit updateQuiz(UUID id, QuizCreateRequest request) {
        LearningUnit quiz = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Quiz"));

        if (!"QUIZ".equalsIgnoreCase(quiz.getType())) {
            throw new ApiException("INVALID_TYPE", "Đơn vị học tập không phải là Quiz");
        }

        quiz.setName(request.getTitle());

        Map<String, Object> metadata = buildQuizMetadata(request);
        try {
            quiz.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            throw new ApiException("INVALID_METADATA", "Quiz metadata không hợp lệ");
        }

        return learningUnitRepository.save(quiz);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllQuizzes() {
        return learningUnitRepository.findByType("QUIZ").stream()
                .map(this::buildQuizResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQuizzesByLevel(UUID levelId) {
        LearningUnit level = learningUnitRepository.findById(levelId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        if (!"LEVEL".equalsIgnoreCase(level.getType())) {
            throw new ApiException("INVALID_PARENT", "Parent phải là Level");
        }

        return learningUnitRepository.findByParentAndType(level, "QUIZ").stream()
                .map(this::buildQuizResponse)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildQuizMetadata(QuizCreateRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("description", request.getDescription());
        metadata.put("instructions", request.getInstructions());
        metadata.put("time_limit_minutes", request.getTimeLimitMinutes());
        metadata.put("passing_score", request.getPassingScore());
        metadata.put("points_per_question", request.getPointsPerQuestion());
        metadata.put("difficulty", request.getDifficulty());
        metadata.put("questions", request.getQuestions());
        metadata.put("question_count", request.getQuestionCount());
        metadata.put("comment", request.getComment());
        metadata.put("skill_type", request.getSkillType());
        return metadata;
    }

    private Map<String, Object> buildQuizResponse(LearningUnit quiz) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", quiz.getId());
        response.put("levelId", quiz.getParent() != null ? quiz.getParent().getId() : null);
        response.put("name", quiz.getName());

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (quiz.getMetadataJson() != null) {
            try {
                metadata = objectMapper.readValue(quiz.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                throw new ApiException("INVALID_METADATA", "Quiz metadata không hợp lệ");
            }
        }

        response.put("description", metadata.get("description"));
        response.put("instructions", metadata.get("instructions"));
        response.put("timeLimitMinutes", metadata.get("time_limit_minutes"));
        response.put("passingScore", metadata.get("passing_score"));
        response.put("pointsPerQuestion", metadata.get("points_per_question"));
        response.put("difficulty", metadata.get("difficulty"));

        List<QuizQuestionRequest> questions = metadata.containsKey("questions")
                ? objectMapper.convertValue(metadata.get("questions"), new TypeReference<List<QuizQuestionRequest>>() {})
                : List.of();
        response.put("questions", questions);
        response.put("questionCount", metadata.get("question_count"));
        response.put("comment", metadata.get("comment"));
        response.put("skillType", metadata.get("skill_type"));

        return response;
    }
}
