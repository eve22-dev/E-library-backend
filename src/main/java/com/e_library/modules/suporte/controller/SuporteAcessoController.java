package com.e_library.modules.suporte.controller;

import com.e_library.modules.suporte.entity.SolicitacaoSuporteEntity;
import com.e_library.modules.suporte.repository.SolicitacaoSuporteRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/suporte/acesso")
@CrossOrigin(origins = "*")
public class SuporteAcessoController {

    public record NovaSolicitacao(String nome, String email, String descricao, String website) {}

    // Limite simples contra abuso: 5 pedidos por hora para cada IP
    private static final int LIMITE_POR_HORA = 5;
    private static final long UMA_HORA_MS = 3_600_000L;
    private final Map<String, Deque<Long>> pedidosPorIp = new ConcurrentHashMap<>();

    private final SolicitacaoSuporteRepository repository;

    public SuporteAcessoController(SolicitacaoSuporteRepository repository) {
        this.repository = repository;
    }

    // PÚBLICO: quem não consegue entrar registra o problema (sem login)
    @PostMapping
    public ResponseEntity<String> registrar(@RequestBody NovaSolicitacao dados, HttpServletRequest request) {

        // Campo escondido do formulário: só robôs preenchem. Finge que deu certo e descarta.
        if (dados.website() != null && !dados.website().isBlank()) {
            return ResponseEntity.ok("Solicitação enviada.");
        }

        String nome = dados.nome() == null ? "" : dados.nome().trim();
        String email = dados.email() == null ? "" : dados.email().trim().toLowerCase();
        String descricao = dados.descricao() == null ? "" : dados.descricao().trim();

        if (nome.length() < 3 || nome.length() > 120) {
            return ResponseEntity.badRequest().body("Informe o seu nome completo.");
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 160) {
            return ResponseEntity.badRequest().body("Informe um e-mail institucional válido.");
        }

        if (descricao.length() < 10 || descricao.length() > 2000) {
            return ResponseEntity.badRequest().body("Descreva o problema com pelo menos 10 caracteres (máximo 2000).");
        }

        if (excedeuLimite(ipDe(request))) {
            return ResponseEntity.status(429).body("Muitas solicitações em pouco tempo. Tente novamente mais tarde.");
        }

        SolicitacaoSuporteEntity pedido = new SolicitacaoSuporteEntity();
        pedido.setNome(nome);
        pedido.setEmail(email);
        pedido.setDescricao(descricao);
        repository.save(pedido);

        return ResponseEntity.ok("Solicitação enviada! A equipe da biblioteca entrará em contato pelo e-mail informado.");
    }

    // PAINEL DO BIBLIOTECÁRIO: lista (opcionalmente filtrada por status)
    @GetMapping
    public ResponseEntity<List<SolicitacaoSuporteEntity>> listar(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return ResponseEntity.ok(repository.findAllByOrderByCriadoEmDesc());
        }
        return ResponseEntity.ok(repository.findByStatusIgnoreCaseOrderByCriadoEmDesc(status));
    }

    @PutMapping("/{id}/resolver")
    public ResponseEntity<String> resolver(@PathVariable @NonNull UUID id) {
        SolicitacaoSuporteEntity pedido = repository.findById(id).orElse(null);

        if (pedido == null) {
            return ResponseEntity.status(404).body("Solicitação não encontrada.");
        }

        pedido.setStatus("resolvido");
        pedido.setResolvidoEm(LocalDateTime.now());
        repository.save(pedido);

        return ResponseEntity.ok("Solicitação marcada como resolvida.");
    }

    private String ipDe(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");

        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private boolean excedeuLimite(String ip) {
        long agora = System.currentTimeMillis();
        Deque<Long> historico = pedidosPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (historico) {
            while (!historico.isEmpty() && agora - historico.peekFirst() > UMA_HORA_MS) {
                historico.pollFirst();
            }

            if (historico.size() >= LIMITE_POR_HORA) {
                return true;
            }

            historico.addLast(agora);
            return false;
        }
    }
}
