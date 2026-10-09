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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String callGemini(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = GEMINI_URL + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Treinando o prompt com as suas FAQs e regra de transbordo
        String systemPrompt = "Você é o assistente virtual da biblioteca E-library. " +
                "Responda de forma educada e curta apenas sobre os seguintes tópicos:\n" +
                "1. Quem pode utilizar a Biblioteca? R: Alunos e funcionários.\n" +
                "2. Quantos itens posso emprestar e por quantos dias? R: Até 3 itens por 7 dias.\n" +
                "3. Quais materiais posso fazer empréstimos? R: Livros didáticos e literaturas do acervo.\n" +
                "4. Como posso renovar o empréstimo pela web? R: Acesse o portal, vá em Meus Empréstimos e clique em Renovar.\n" +
                "5. Como realizar reserva de materiais? R: Pesquise o livro no portal e clique em Reservar.\n" +
                "6. Como acessar a Biblioteca online? R: Faça login no site com seu RGM/Email e senha.\n\n" +
                "REGRA CRÍTICA: Se o usuário perguntar qualquer coisa fora desses tópicos, ou disser explicitamente que quer falar com um atendente/humano, VOCÊ NÃO DEVE RESPONDER A PERGUNTA. Responda EXATAMENTE com a palavra: [TRANSBORDO]";

        Map<String, Object> part = new HashMap<>();
        part.put("text", systemPrompt + "\n\nMensagem do usuário: " + prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<?, ?> responseBody = response.getBody();
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
            return "Erro ao comunicar com os nossos serviços: " + e.getMessage();
        }
    }
}