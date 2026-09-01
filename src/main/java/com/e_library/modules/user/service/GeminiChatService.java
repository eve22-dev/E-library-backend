package com.e_library.modules.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiChatService {

    @Value("${google.gemini.api.key}")
    private String apiKey;

    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public String callGemini(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = GEMINI_URL + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> part = new HashMap<>();
        part.put("text", "Você é um bibliotecário virtual prestativo da e-library. Responda de forma educada e prestativa: " + prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("candidates")) {
                List<Map> candidates = (List<Map>) responseBody.get("candidates");
                if (!candidates.isEmpty()) {
                    Map candidate = candidates.get(0);
                    Map contentMap = (Map) candidate.get("content");
                    List<Map> parts = (List<Map>) contentMap.get("parts");
                    if (!parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            return "Não consegui processar a resposta no momento.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao comunicar com a IA do Gemini: " + e.getMessage();
        }
    }
}