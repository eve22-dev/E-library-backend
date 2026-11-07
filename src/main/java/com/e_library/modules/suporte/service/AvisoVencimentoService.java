package com.e_library.modules.suporte.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Avisos de prazo por e-mail. Roda todo dia às 8h (horário de Brasília).
 * Vai para quem ativou a opção no perfil e tem livro que vence em até 3 dias ou já venceu.
 * Cada empréstimo recebe no máximo um aviso a cada 3 dias.
 */
@Service
public class AvisoVencimentoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String SQL_PENDENTES =
            "SELECT e.id, e.data_prevista, u.nome, u.email, l.titulo " +
            "FROM emprestimos e " +
            "JOIN usuarios u ON u.id = e.user_id " +
            "JOIN livros l ON l.id = e.livro_id " +
            "WHERE e.status IN ('retirado', 'atrasado') " +
            "AND u.notificar_email = true " +
            "AND e.data_prevista <= CURRENT_DATE + 3 " +
            "AND (e.ultimo_aviso IS NULL OR e.ultimo_aviso <= CURRENT_DATE - 3)";

    private final JdbcTemplate jdbc;
    private final EmailService emailService;

    public AvisoVencimentoService(JdbcTemplate jdbc, EmailService emailService) {
        this.jdbc = jdbc;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    public void enviarAvisosAgendados() {
        enviarAvisos();
    }

    // Também usado pelo botão do painel (para testar antes da apresentação). Retorna quantos avisos saíram.
    public int enviarAvisos() {
        int enviados = 0;

        for (Map<String, Object> p : jdbc.queryForList(SQL_PENDENTES)) {
            try {
                UUID id = (UUID) p.get("id");
                LocalDate prazo = ((java.sql.Date) p.get("data_prevista")).toLocalDate();
                String nome = (String) p.get("nome");
                String email = (String) p.get("email");
                String titulo = (String) p.get("titulo");

                String situacao = prazo.isBefore(LocalDate.now())
                        ? "está atrasado desde " + prazo.format(DATA) + ". A multa é de R$ 2,00 por dia de atraso."
                        : "vence em " + prazo.format(DATA) + ".";

                String html = "<p>Olá, " + HtmlUtils.htmlEscape(nome) + ".</p>"
                        + "<p>O livro <b>" + HtmlUtils.htmlEscape(titulo) + "</b> " + situacao + "</p>"
                        + "<p>Se precisar de mais prazo, peça a renegociação em Meus Empréstimos (disponível a partir de 3 dias antes do vencimento).</p>"
                        + "<p><i>Para deixar de receber estes avisos, desative a opção no seu perfil.</i></p>";

                emailService.enviarAviso(email, "[E-Library] Aviso de prazo: " + titulo, html);
                jdbc.update("UPDATE emprestimos SET ultimo_aviso = CURRENT_DATE WHERE id = ?", id);
                enviados++;
            } catch (Exception e) {
                System.err.println("Aviso de prazo não enviado: " + e.getMessage());
            }
        }

        return enviados;
    }
}
