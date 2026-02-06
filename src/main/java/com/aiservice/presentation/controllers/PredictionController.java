package com.aiservice.presentation.controllers;

import com.aiservice.application.services.PredictionService;
import com.aiservice.presentation.dto.PredictionRequest;
import com.aiservice.presentation.dto.PredictionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public PredictionResponse createPrediction(@Valid @RequestBody PredictionRequest request) {
        log.info("Creating prediction for user ID: {}", request.getUserId());
        return predictionService.createPrediction(request);
    }

    @GetMapping("/{id}")
    public PredictionResponse getPredictionById(@PathVariable Long id) {
        log.info("Fetching prediction by ID: {}", id);
        return predictionService.getPredictionById(id);
    }

    @GetMapping("/")
    public Page<PredictionResponse> getAllPredictions(Pageable pageable) {
        log.info("Fetching all predictions with pagination");
        return predictionService.getAllPredictions(pageable);
    }

    @GetMapping("/user/{userId}")
    public Page<PredictionResponse> getPredictionsByUserId(@PathVariable Long userId, Pageable pageable) {
        log.info("Fetching predictions for user ID: {}", userId);
        return predictionService.getPredictionsByUserId(userId, pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePrediction(@PathVariable Long id) {
        log.info("Deleting prediction with ID: {}", id);
        predictionService.deletePrediction(id);
    }
}
