package com.e_library.modules.user.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.e_library.modules.suporte.service.AnexoService;
import com.e_library.modules.suporte.service.ConversaService;
import com.e_library.modules.user.service.GeminiChatService;
import com.e_library.modules.user.service.SuporteService;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Libera o acesso para o front-end
public class ChatController {

    public record PedidoChat(String mensagem, List<GeminiChatService.Turno> historico) {}

    // Cada usuário pode mandar até 12 mensagens por minuto para a IA (protege a cota da API)
    private static final int LIMITE_POR_MINUTO = 12;
    private final Map<String, Deque<Long>> mensagensPorUsuario = new ConcurrentHashMap<>();

    private final GeminiChatService geminiChatService;
    private final SuporteService suporteService;
    private final ConversaService conversaService;
    private final AnexoService anexoService;

    public ChatController(GeminiChatService geminiChatService, SuporteService suporteService,
                          ConversaService conversaService, AnexoService anexoService) {
        this.geminiChatService = geminiChatService;
        this.suporteService = suporteService;
        this.conversaService = conversaService;
        this.anexoService = anexoService;
    }

    // ---------------- 1) Conversa com a IA ----------------

    @PostMapping
    public ResponseEntity<?> conversar(@RequestBody PedidoChat pedido, Authentication auth) {

        String mensagem = pedido.mensagem() == null ? "" : pedido.mensagem().trim();

        if (mensagem.isEmpty() || mensagem.length() > 800) {
            return ResponseEntity.badRequest().body("Escreva uma mensagem de até 800 caracteres.");
        }

        if (excedeuLimite(auth.getName())) {
            return ResponseEntity.status(429).body("Muitas mensagens em pouco tempo. Aguarde um instante.");
        }

        try {
            GeminiChatService.Resposta r = geminiChatService.responder(papelDe(auth), pedido.historico(), mensagem);

            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("resposta", r.texto());
            corpo.put("transbordo", r.transbordo());
            return ResponseEntity.ok(corpo);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // ---------------- 2) Falar com o bibliotecário (conversa dentro da plataforma) ----------------

    // Abre uma conversa nova (texto + anexos opcionais)
    @PostMapping(value = "/suporte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> enviarParaAtendente(
            @RequestParam("duvida") String duvida,
            @RequestParam(value = "arquivos", required = false) List<MultipartFile> arquivos,
            Authentication auth) {
        try {
            List<AnexoService.ArquivoValidado> validos = anexoService.validar(arquivos);
            suporteService.salvarMensagemSuporte(auth.getName(), duvida, validos);
            return ResponseEntity.ok("Sua mensagem foi enviada ao bibliotecário. A resposta vai aparecer aqui no chat.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // Conversas do usuário logado, com as respostas do bibliotecário
    @GetMapping("/conversas")
    public ResponseEntity<List<Map<String, Object>>> conversas(Authentication auth) {
        return ResponseEntity.ok(conversaService.conversasDoUsuario(auth.getName()));
    }

    // Continua a conversa (o aluno responde ao bibliotecário), também com anexos
    @PostMapping(value = "/conversas/{id}/responder", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> responderConversa(
            @PathVariable @NonNull UUID id,
            @RequestParam("texto") String texto,
            @RequestParam(value = "arquivos", required = false) List<MultipartFile> arquivos,
            Authentication auth) {
        try {
            List<AnexoService.ArquivoValidado> validos = anexoService.validar(arquivos);
            conversaService.responderComoAluno(auth.getName(), id, texto, validos);
            return ResponseEntity.ok("Mensagem enviada ao bibliotecário.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // O aluno abriu a conversa: marca as respostas como lidas
    @PutMapping("/conversas/{id}/lida")
    public ResponseEntity<String> marcarLida(@PathVariable @NonNull UUID id, Authentication auth) {
        try {
            conversaService.marcarLidas(auth.getName(), id);
            return ResponseEntity.ok("ok");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // ---------------- apoio ----------------

    private String papelDe(Authentication auth) {
        boolean professor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PROFESSOR"));
        boolean bibliotecario = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_BIBLIOTECARIO"));

        return professor ? "professor" : (bibliotecario ? "bibliotecario" : "aluno");
    }

    private boolean excedeuLimite(String usuario) {
        long agora = System.currentTimeMillis();
        Deque<Long> historico = mensagensPorUsuario.computeIfAbsent(usuario, k -> new ArrayDeque<>());

        synchronized (historico) {
            while (!historico.isEmpty() && agora - historico.peekFirst() > 60_000L) {
                historico.pollFirst();
            }

            if (historico.size() >= LIMITE_POR_MINUTO) {
                return true;
            }

            historico.addLast(agora);
            return false;
        }
    }
}
