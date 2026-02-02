package com.aiservice.presentation.dto;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionRequest {
    private Map<String, Object> inputData;
    private Long userId;
}
