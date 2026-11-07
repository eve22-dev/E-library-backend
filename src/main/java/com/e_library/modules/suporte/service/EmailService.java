package com.e_library.modules.suporte.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envia e-mails pelo Brevo (por HTTPS, porque o Render gratuito bloqueia as portas SMTP).
 * Variáveis: BREVO_API_KEY, OUVIDORIA_REMETENTE (remetente verificado no Brevo) e, opcional, OUVIDORIA_EMAIL.
 */
@Service
public class EmailService {

    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String remetente;
    private final String ouvidoria;

    public EmailService(
            @Value("${brevo.api-key:}") String apiKey,
            @Value("${ouvidoria.remetente:}") String remetente,
            @Value("${ouvidoria.email:heeatec@gmail.com}") String ouvidoria
    ) {
        this.apiKey = apiKey;
        this.remetente = remetente;
        this.ouvidoria = ouvidoria;
    }

    // Reclamação de um usuário logado: vai para a ouvidoria, com os anexos
    public void enviarReclamacao(String protocolo, String nomeUsuario, String emailUsuario, String descricao,
                                 List<AnexoService.ArquivoValidado> anexos) {

        String html = "<h2>Nova reclamação - E-Library</h2>"
                + linha("Protocolo", protocolo)
                + linha("Nome", nomeUsuario)
                + linha("E-mail institucional", emailUsuario)
                + "<p><b>Reclamação:</b></p>"
                + "<p>" + paragrafo(descricao) + "</p>"
                + "<p><i>Anexos: " + (anexos.isEmpty() ? "nenhum" : anexos.size() + " arquivo(s) neste e-mail") + "</i></p>";

        enviarParaDestino(ouvidoria, "[E-Library] Reclamação " + protocolo + " - " + umaLinha(nomeUsuario),
                html, emailUsuario, nomeUsuario, anexos);
    }

    // Pedido de quem não consegue entrar no sistema: vai para a ouvidoria
    public void enviarSuporteAcesso(String protocolo, String nome, String email, String descricao) {

        String html = "<h2>Suporte de acesso - E-Library</h2>"
                + linha("Protocolo", protocolo)
                + linha("Nome", nome)
                + linha("E-mail institucional informado", email)
                + "<p><b>Descrição do problema:</b></p>"
                + "<p>" + paragrafo(descricao) + "</p>"
                + "<p><i>Pedido feito pela página pública, sem login. Responda a este e-mail para falar com a pessoa.</i></p>";

        enviarParaDestino(ouvidoria, "[E-Library] Suporte de acesso " + protocolo + " - " + umaLinha(nome),
                html, email, nome, List.of());
    }

    // Aviso de prazo para o próprio aluno/professor
    public void enviarAviso(String destinatario, String assunto, String html) {
        enviarParaDestino(destinatario, assunto, html, remetente, "E-Library", List.of());
    }

    // ---------------- envio ----------------

    private void enviarParaDestino(String destinatario, String assunto, String html,
                                   String respostaPara, String nomeResposta,
                                   List<AnexoService.ArquivoValidado> anexos) {

        if (apiKey.isBlank() || remetente.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O envio de e-mail ainda não está configurado no servidor.");
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sender", Map.of("name", "E-Library", "email", remetente));
        corpo.put("to", List.of(Map.of("email", destinatario)));
        corpo.put("replyTo", Map.of("email", respostaPara, "name", umaLinha(nomeResposta)));
        corpo.put("subject", assunto);
        corpo.put("htmlContent", html);

        if (!anexos.isEmpty()) {
            List<Map<String, String>> lista = new ArrayList<>();
            for (AnexoService.ArquivoValidado a : anexos) {
                lista.add(Map.of("name", a.nome(), "content", Base64.getEncoder().encodeToString(a.conteudo())));
            }
            corpo.put("attachment", lista);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(URL_BREVO))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo)))
                    .build();

            HttpResponse<String> resposta = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
                System.err.println("Brevo respondeu " + resposta.statusCode() + ": "
                        + resposta.body().substring(0, Math.min(300, resposta.body().length())));
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Não foi possível enviar o e-mail agora. Tente novamente em instantes.");
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Envio interrompido. Tente novamente.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível enviar o e-mail agora. Tente novamente em instantes.");
        }
    }

    private String linha(String rotulo, String valor) {
        return "<p><b>" + rotulo + ":</b> " + HtmlUtils.htmlEscape(valor == null ? "" : valor) + "</p>";
    }

    private String paragrafo(String texto) {
        return HtmlUtils.htmlEscape(texto == null ? "" : texto).replace("\n", "<br>");
    }

    private String umaLinha(String texto) {
        return texto == null ? "" : texto.replaceAll("[\\r\\n]+", " ").trim();
    }
}
