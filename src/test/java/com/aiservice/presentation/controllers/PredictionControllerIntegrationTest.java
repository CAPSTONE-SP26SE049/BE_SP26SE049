package com.aiservice.presentation.controllers;

import com.aiservice.application.services.AIService;
import com.aiservice.domain.entities.Prediction;
import com.aiservice.domain.entities.User;
import com.aiservice.domain.repositories.PredictionRepository;
import com.aiservice.domain.repositories.UserRepository;
import com.aiservice.infrastructure.security.JwtUtils;
import com.aiservice.presentation.dto.PredictionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PredictionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private AIService aiService;

    private String authToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.UserRole.PLAYER)
                .isActive(true)
                .build();
        testUser = userRepository.save(testUser);

        // Generate auth token
        authToken = jwtUtils.generateToken(testUser.getUsername());

        // Mock AI service response
        Map<String, Object> aiResult = new HashMap<>();
        aiResult.put("result", "positive");
        aiResult.put("model_version", "v1.0");
        aiResult.put("confidence", 0.95);
        when(aiService.predict(any())).thenReturn(aiResult);
    }

    @Test
    void createPrediction_WithValidData_ShouldCreatePrediction() throws Exception {
        // Given
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("text", "test input");

        PredictionRequest request = PredictionRequest.builder()
                .userId(testUser.getId())
                .inputData(inputData)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/predictions/")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.predictionResult").value("positive"))
                .andExpect(jsonPath("$.confidenceScore").value(0.95))
                .andExpect(jsonPath("$.modelVersion").value("v1.0"))
                .andExpect(jsonPath("$.processingTimeMs").exists());
    }

    @Test
    void createPrediction_WithoutAuth_ShouldReturnForbidden() throws Exception {
        // Given
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("text", "test input");

        PredictionRequest request = PredictionRequest.builder()
                .userId(testUser.getId())
                .inputData(inputData)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/predictions/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPrediction_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given
        PredictionRequest request = PredictionRequest.builder()
                .userId(null)
                .inputData(null)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/predictions/")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPredictionById_WithExistingId_ShouldReturnPrediction() throws Exception {
        // Given
        Prediction prediction = Prediction.builder()
                .userId(testUser.getId())
                .inputData("{\"text\":\"test\"}")
                .predictionResult("positive")
                .modelVersion("v1.0")
                .confidenceScore(0.95)
                .processingTimeMs(150L)
                .build();
        prediction = predictionRepository.save(prediction);

        // When & Then
        mockMvc.perform(get("/api/v1/predictions/" + prediction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prediction.getId()))
                .andExpect(jsonPath("$.predictionResult").value("positive"));
    }

    @Test
    void getPredictionById_WithNonExistingId_ShouldReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/predictions/999")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getAllPredictions_ShouldReturnPagedResults() throws Exception {
        // Given
        for (int i = 0; i < 5; i++) {
            Prediction prediction = Prediction.builder()
                    .userId(testUser.getId())
                    .inputData("{\"text\":\"test " + i + "\"}")
                    .predictionResult("result " + i)
                    .modelVersion("v1.0")
                    .confidenceScore(0.9)
                    .processingTimeMs(100L)
                    .build();
            predictionRepository.save(prediction);
        }

        // When & Then
        mockMvc.perform(get("/api/v1/predictions/")
                .header("Authorization", "Bearer " + authToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void getPredictionsByUserId_ShouldReturnUserPredictions() throws Exception {
        // Given
        Prediction prediction = Prediction.builder()
                .userId(testUser.getId())
                .inputData("{\"text\":\"test\"}")
                .predictionResult("positive")
                .modelVersion("v1.0")
                .confidenceScore(0.95)
                .processingTimeMs(150L)
                .build();
        predictionRepository.save(prediction);

        // When & Then
        mockMvc.perform(get("/api/v1/predictions/user/" + testUser.getId())
                .header("Authorization", "Bearer " + authToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].predictionResult").value("positive"));
    }

    @Test
    void deletePrediction_WithExistingId_ShouldDeletePrediction() throws Exception {
        // Given
        Prediction prediction = Prediction.builder()
                .userId(testUser.getId())
                .inputData("{\"text\":\"test\"}")
                .predictionResult("positive")
                .modelVersion("v1.0")
                .confidenceScore(0.95)
                .processingTimeMs(150L)
                .build();
        prediction = predictionRepository.save(prediction);

        // When & Then
        mockMvc.perform(delete("/api/v1/predictions/" + prediction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePrediction_WithNonExistingId_ShouldReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/predictions/999")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
