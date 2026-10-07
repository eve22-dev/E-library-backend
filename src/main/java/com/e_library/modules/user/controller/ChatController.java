package com.e_library.modules.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.e_library.modules.user.service.GeminiChatService;
import com.e_library.modules.user.service.SuporteService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Libera o acesso para o front-end
public class ChatController {

   
    private final GeminiChatService geminiChatService = new GeminiChatService();

    private final SuporteService suporteService = new SuporteService();

    // Endpoint 1: Conversa normal com o bot
    @PostMapping
    public ResponseEntity<String> conversar(@RequestBody Map<String, String> payload) {
        String mensagem = payload.get("mensagem");
        String respostaGemini = geminiChatService.callGemini(mensagem);

        // Se a IA não souber ou o usuário pedir um humano, ela devolve [TRANSBORDO]
        if (respostaGemini != null && respostaGemini.contains("[TRANSBORDO]")) {
            return ResponseEntity.ok("Descreva sua dúvida brevemente => duvida escrita => vamos passar para um atendente!");
        }

        return ResponseEntity.ok(respostaGemini);
    }

    // Endpoint 2: Salva a dúvida do usuário no banco para o bibliotecário ler
    @PostMapping("/suporte")
    public ResponseEntity<String> enviarParaAtendente(@RequestBody Map<String, String> payload) {
        String duvida = payload.get("duvida");
        UUID userId = UUID.fromString(payload.get("userId")); // O frontend mandará o ID de quem está logado

        suporteService.salvarMensagemSuporte(userId, duvida);

        return ResponseEntity.ok("Sua mensagem foi enviada. Um bibliotecário responderá em breve nas Solicitações de Chat!");
    }
}