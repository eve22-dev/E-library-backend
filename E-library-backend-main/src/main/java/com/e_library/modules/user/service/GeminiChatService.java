
package com.e_library.modules.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
public class GeminiChatService {

    @Value("${google.gemini.api.key}")
    private String apiKey;

    @Value("${google.gemini.api.url}")
    private String apiUrl;

    public String enviarMensagem(String mensagemDoUsuario) {
        RestTemplate restTemplate = new RestTemplate();
        
        String urlComChave = apiUrl + "?key=" + apiKey;

        String requestBody = """
            {
              "contents": [{
                "parts":[{
                  "text": "Você é o assistente virtual da e-library. Responda de forma curta e ajude o aluno com esta dúvida: %s"
                }]
              }]
            }
            """.formatted(mensagemDoUsuario);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(urlComChave, request, String.class);
        
        return response.getBody();
    }
}
