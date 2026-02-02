package com.aiservice.infrastructure.ai;

import com.aiservice.application.services.AIService;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIServiceImpl implements AIService {

    @Override
    public Map<String, Object> predict(Map<String, Object> inputData) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        data.put("class", "sample_class");
        data.put("label", 0);
        data.put("probabilities", new double[] { 0.8, 0.2 });

        result.put("result", data);
        result.put("confidence", 0.8);
        result.put("model_version", "v1.0");

        return result;
    }
}
