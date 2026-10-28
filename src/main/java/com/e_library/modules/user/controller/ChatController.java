package com.e_library.modules.user.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.e_library.modules.suporte.service.AnexoService;
import com.e_library.modules.user.service.GeminiChatService;
import com.e_library.modules.user.service.SuporteService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Libera o acesso para o front-end
public class ChatController {

    private final GeminiChatService geminiChatService;
    private final SuporteService suporteService;
    private final AnexoService anexoService;

    public ChatController(GeminiChatService geminiChatService, SuporteService suporteService,
                          AnexoService anexoService) {
        this.geminiChatService = geminiChatService;
        this.suporteService = suporteService;
        this.anexoService = anexoService;
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

    // Endpoint 2: dúvida do usuário logado para o bibliotecário (texto + anexos opcionais)
    @PostMapping(value = "/suporte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> enviarParaAtendente(
            @RequestParam("duvida") String duvida,
            @RequestParam(value = "arquivos", required = false) List<MultipartFile> arquivos,
            Authentication auth) {
        try {
            List<AnexoService.ArquivoValidado> validos = anexoService.validar(arquivos);
            suporteService.salvarMensagemSuporte(auth.getName(), duvida, validos);
            return ResponseEntity.ok("Sua mensagem foi enviada. Um bibliotecário responderá em breve!");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}
