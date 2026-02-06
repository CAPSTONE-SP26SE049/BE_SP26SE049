package com.aiservice.application.services;

import com.aiservice.presentation.dto.PredictionRequest;
import com.aiservice.presentation.dto.PredictionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PredictionService {
    
    /**
     * Create a new prediction
     * @param request prediction request
     * @return prediction response
     */
    PredictionResponse createPrediction(PredictionRequest request);
    
    /**
     * Get prediction by ID
     * @param id prediction ID
     * @return prediction response
     */
    PredictionResponse getPredictionById(Long id);
    
    /**
     * Get all predictions with pagination
     * @param pageable pagination information
     * @return page of prediction responses
     */
    Page<PredictionResponse> getAllPredictions(Pageable pageable);
    
    /**
     * Get predictions by user ID with pagination
     * @param userId user ID
     * @param pageable pagination information
     * @return page of prediction responses
     */
    Page<PredictionResponse> getPredictionsByUserId(Long userId, Pageable pageable);
    
    /**
     * Delete prediction
     * @param id prediction ID
     */
    void deletePrediction(Long id);
}
