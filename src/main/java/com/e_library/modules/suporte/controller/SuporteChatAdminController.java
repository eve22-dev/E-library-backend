package com.e_library.modules.suporte.controller;

import com.e_library.modules.suporte.service.AnexoService;
import com.e_library.modules.suporte.service.SuporteAdminService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Rotas exclusivas do bibliotecário (ver SecurityConfig)
@RestController
@CrossOrigin(origins = "*")
public class SuporteChatAdminController {

    private final SuporteAdminService suporteAdminService;
    private final AnexoService anexoService;

    public SuporteChatAdminController(SuporteAdminService suporteAdminService, AnexoService anexoService) {
        this.suporteAdminService = suporteAdminService;
        this.anexoService = anexoService;
    }

    // Solicitações do chat (aba Solicitações Chat)
    @GetMapping("/api/suporte/chat")
    public ResponseEntity<List<Map<String, Object>>> listar(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(suporteAdminService.listarChat(status));
    }

    @PutMapping("/api/suporte/chat/{id}/resolver")
    public ResponseEntity<String> resolver(@PathVariable @NonNull UUID id) {
        if (!suporteAdminService.resolver(id)) {
            return ResponseEntity.status(404).body("Solicitação não encontrada.");
        }
        return ResponseEntity.ok("Solicitação marcada como resolvida.");
    }

    // Download de um anexo
    @GetMapping("/api/anexos/{id}")
    public ResponseEntity<byte[]> baixar(@PathVariable @NonNull UUID id) {
        return anexoService.buscar(id)
                .map(a -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment().filename(a.nome(), StandardCharsets.UTF_8).build().toString())
                        .contentType(MediaType.parseMediaType(a.tipo()))
                        .body(a.conteudo()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
