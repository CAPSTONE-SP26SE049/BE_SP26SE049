package org.fsa_2026.company_fsa_captone_2026.controller;

import java.util.Map;
import org.fsa_2026.company_fsa_captone_2026.service.TTSService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

/**
 * Text-to-Speech Controller
 * Proxy requests to FPT.AI to avoid CORS on Frontend
 */
@RestController
@RequestMapping("/api/v1/tts")
@RequiredArgsConstructor
public class TTSController {
    private final TTSService ttsService;

    @SuppressWarnings("rawtypes")
    @PostMapping("/synthesize")
    public Map synthesize(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String voice = request.get("voice");
        
        if (text == null || text.isBlank()) {
            throw new org.fsa_2026.company_fsa_captone_2026.exception.ApiException("BAD_REQUEST", "Văn bản không được để trống");
        }
        
        return ttsService.synthesize(text, voice);
    }
}
