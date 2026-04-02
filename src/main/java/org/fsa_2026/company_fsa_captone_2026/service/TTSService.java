package org.fsa_2026.company_fsa_captone_2026.service;

import java.util.Map;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TTSService {
    private static final String FPT_API_URL = "https://api.fpt.ai/hmi/tts/v5";
    private static final String FPT_API_KEY = "oZO8MheKtxmn0JAVKiaeURTubrDtOwdp";

    @SuppressWarnings("rawtypes")
    public Map synthesize(String text, String voice) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters()
                    .add(0, new org.springframework.http.converter.StringHttpMessageConverter(java.nio.charset.StandardCharsets.UTF_8));
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", FPT_API_KEY);
            headers.set("api_key", FPT_API_KEY);
            headers.set("Content-Type", "text/plain; charset=utf-8");
            headers.set("voice", voice != null ? voice : "banmai");
            headers.set("speed", "");
            headers.set("prosody", "1");

            // FPT AI expects raw text in body
            HttpEntity<String> entity = new HttpEntity<>(text, headers);
            log.info("Calling FPT AI TTS for text: {}", text.substring(0, Math.min(text.length(), 20)) + "...");
            
            ResponseEntity<Map> response = restTemplate.exchange(FPT_API_URL, HttpMethod.POST, entity, Map.class);
            Map body = response.getBody();
            
            // Wait for the async url to be ready to avoid CORS errors on frontend
            if (body != null && body.containsKey("async")) {
                String asyncUrl = (String) body.get("async");
                pollAudioUrl(asyncUrl);
            }
            
            return body;
        } catch (Exception e) {
            log.error("FPT AI TTS Error: ", e);
            throw new ApiException("INTERNAL_SERVER_ERROR", "Lỗi khi gọi API FPT AI: " + e.getMessage());
        }
    }
    
    // Helper to poll until FPT.AI finishes processing
    private void pollAudioUrl(String url) {
        RestTemplate restTemplate = new RestTemplate();
        int retries = 0;
        int maxRetries = 20; // Maximum wait ~20 seconds
        while (retries < maxRetries) {
            try {
                ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.HEAD, null, String.class);
                if (res.getStatusCode().is2xxSuccessful()) {
                    log.info("Audio processed successfully at {}", url);
                    return; // Ready!
                }
            } catch (Exception ignored) {
                // Ignore 404 or other errors while processing
            }
            try {
                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            retries++;
        }
        log.warn("Timeout waiting for FPT.AI to process audio: {}", url);
    }
}
