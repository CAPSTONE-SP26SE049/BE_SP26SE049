package com.aiservice.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionRequest {
    @NotNull(message = "Input data is required")
    private Map<String, Object> inputData;
    
    @NotNull(message = "User ID is required")
    private Long userId;
}
