package com.e_library.modules.suporte.controller;

import com.e_library.modules.suporte.service.AnexoService;
import com.e_library.modules.suporte.service.AvisoVencimentoService;
import com.e_library.modules.suporte.service.ConversaService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class SuporteChatAdminController {

    private final ConversaService conversaService;
    private final AnexoService anexoService;
    private final AvisoVencimentoService avisoVencimentoService;

    public SuporteChatAdminController(ConversaService conversaService, AnexoService anexoService,
                                      AvisoVencimentoService avisoVencimentoService) {
        this.conversaService = conversaService;
        this.anexoService = anexoService;
        this.avisoVencimentoService = avisoVencimentoService;
    }

    // Conversas do chat (aba Solicitações Chat). Restrita ao bibliotecário (ver SecurityConfig).
    @GetMapping("/api/suporte/chat")
    public ResponseEntity<List<Map<String, Object>>> listar(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(conversaService.listarChat(status));
    }

    @PostMapping("/api/suporte/chat/{id}/responder")
    public ResponseEntity<String> responder(@PathVariable @NonNull UUID id, @RequestBody Map<String, String> payload) {
        try {
            conversaService.responderComoBibliotecario(id, payload.get("texto"));
            return ResponseEntity.ok("Resposta enviada ao aluno.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/api/suporte/chat/{id}/resolver")
    public ResponseEntity<String> resolver(@PathVariable @NonNull UUID id) {
        if (!conversaService.resolver(id)) {
            return ResponseEntity.status(404).body("Conversa não encontrada.");
        }
        return ResponseEntity.ok("Conversa marcada como resolvida.");
    }

    // Botão de teste do painel: dispara agora os avisos de prazo
    @PostMapping("/api/suporte/avisos-vencimento")
    public ResponseEntity<String> enviarAvisos() {
        int enviados = avisoVencimentoService.enviarAvisos();
        return ResponseEntity.ok("Avisos de prazo enviados: " + enviados);
    }

    // Download de anexo: o bibliotecário baixa qualquer anexo; o aluno só os da própria conversa
    @GetMapping("/api/anexos/{id}")
    public ResponseEntity<byte[]> baixar(@PathVariable @NonNull UUID id, Authentication auth) {

        boolean bibliotecario = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_BIBLIOTECARIO".equals(a.getAuthority()));

        if (!bibliotecario && !conversaService.podeBaixarAnexo(auth.getName(), id)) {
            return ResponseEntity.status(403).build();
        }

        return anexoService.buscar(id)
                .map(a -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment().filename(a.nome(), StandardCharsets.UTF_8).build().toString())
                        .contentType(MediaType.parseMediaType(a.tipo()))
                        .body(a.conteudo()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
