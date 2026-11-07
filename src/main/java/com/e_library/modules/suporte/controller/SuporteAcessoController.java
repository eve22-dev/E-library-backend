package com.e_library.modules.suporte.controller;

import com.e_library.modules.suporte.service.EmailService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Página pública de suporte: quem não consegue entrar manda o problema.
 * O pedido NÃO fica no painel: vai por e-mail para a ouvidoria (como as reclamações).
 * No banco fica só o protocolo.
 */
@RestController
@RequestMapping("/api/suporte/acesso")
@CrossOrigin(origins = "*")
public class SuporteAcessoController {

    public record NovaSolicitacao(String nome, String email, String descricao, String website) {}

    // Proteção contra abuso (a rota é pública): 5 pedidos por hora por IP e 40 por hora no total
    private static final int LIMITE_POR_IP = 5;
    private static final int LIMITE_TOTAL = 40;
    private static final long UMA_HORA_MS = 3_600_000L;

    private final Map<String, Deque<Long>> pedidosPorIp = new ConcurrentHashMap<>();
    private final Deque<Long> pedidosTotais = new ArrayDeque<>();

    private final EmailService emailService;
    private final JdbcTemplate jdbc;

    public SuporteAcessoController(EmailService emailService, JdbcTemplate jdbc) {
        this.emailService = emailService;
        this.jdbc = jdbc;
    }

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

        UUID id = UUID.randomUUID();
        String protocolo = "SUP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + id.toString().substring(0, 4).toUpperCase();

        try {
            emailService.enviarSuporteAcesso(protocolo, nome, email, descricao);
        } catch (ResponseStatusException falhaEmail) {
            gravarProtocolo(id, protocolo, "falhou", nome, email);
            return ResponseEntity.status(falhaEmail.getStatusCode()).body(falhaEmail.getReason());
        }

        gravarProtocolo(id, protocolo, "enviada", nome, email);

        return ResponseEntity.ok("Solicitação enviada! Protocolo: " + protocolo
                + ". A equipe da biblioteca entrará em contato pelo e-mail informado.");
    }

    // Registro do protocolo (quem enviou, nome e e-mail informados); o conteúdo vai por e-mail
    private void gravarProtocolo(UUID id, String protocolo, String statusEnvio, String nome, String email) {
        jdbc.update(
                "INSERT INTO reclamacoes (id, user_id, protocolo, qtd_anexos, status_envio, tipo, nome_informado, email_informado) VALUES (?, NULL, ?, 0, ?, 'suporte_acesso', ?, ?)",
                id, protocolo, statusEnvio, nome, email);
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

        synchronized (pedidosTotais) {
            while (!pedidosTotais.isEmpty() && agora - pedidosTotais.peekFirst() > UMA_HORA_MS) {
                pedidosTotais.pollFirst();
            }

            if (pedidosTotais.size() >= LIMITE_TOTAL) {
                return true;
            }
        }

        Deque<Long> historico = pedidosPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (historico) {
            while (!historico.isEmpty() && agora - historico.peekFirst() > UMA_HORA_MS) {
                historico.pollFirst();
            }

            if (historico.size() >= LIMITE_POR_IP) {
                return true;
            }

            historico.addLast(agora);
        }

        synchronized (pedidosTotais) {
            pedidosTotais.addLast(agora);
        }

        return false;
    }
}
