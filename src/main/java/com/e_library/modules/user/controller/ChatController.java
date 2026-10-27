package com.e_library.modules.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.e_library.modules.user.service.GeminiChatService;
import com.e_library.modules.user.service.SuporteService;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Libera o acesso para o front-end
public class ChatController {

    private final GeminiChatService geminiChatService;
    private final SuporteService suporteService;

    // Os dois serviços agora são injetados pelo Spring (antes eram criados com new e ficavam sem configuração)
    public ChatController(GeminiChatService geminiChatService, SuporteService suporteService) {
        this.geminiChatService = geminiChatService;
        this.suporteService = suporteService;
    }

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

    // Endpoint 2: Salva a dúvida do usuário logado para o bibliotecário ler
    @PostMapping("/suporte")
    public ResponseEntity<String> enviarParaAtendente(@RequestBody Map<String, String> payload, Authentication auth) {
        try {
            suporteService.salvarMensagemSuporte(auth.getName(), payload.get("duvida"));
            return ResponseEntity.ok("Sua mensagem foi enviada. Um bibliotecário responderá em breve!");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}
