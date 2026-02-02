package com.aiservice.application.services;

import java.util.Map;

public interface AIService {
    Map<String, Object> predict(Map<String, Object> inputData);
}
