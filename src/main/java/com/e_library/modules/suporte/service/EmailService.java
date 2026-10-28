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
 * Variáveis de ambiente: BREVO_API_KEY, OUVIDORIA_REMETENTE (e-mail remetente verificado no Brevo)
 * e, opcionalmente, OUVIDORIA_EMAIL (padrão: heeatec@gmail.com).
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

    // Envia a reclamação para a ouvidoria. O "responder" do e-mail vai direto para o usuário.
    public void enviarReclamacao(String protocolo, String nomeUsuario, String emailUsuario, String descricao,
                                 List<AnexoService.ArquivoValidado> anexos) {

        if (apiKey.isBlank() || remetente.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O envio de e-mail ainda não está configurado no servidor.");
        }

        String html = "<h2>Nova reclamação - E-Library</h2>"
                + "<p><b>Protocolo:</b> " + HtmlUtils.htmlEscape(protocolo) + "</p>"
                + "<p><b>Nome:</b> " + HtmlUtils.htmlEscape(nomeUsuario) + "</p>"
                + "<p><b>E-mail institucional:</b> " + HtmlUtils.htmlEscape(emailUsuario) + "</p>"
                + "<p><b>Reclamação:</b></p>"
                + "<p>" + HtmlUtils.htmlEscape(descricao).replace("\n", "<br>") + "</p>"
                + "<p><i>Anexos: " + (anexos.isEmpty() ? "nenhum" : anexos.size() + " arquivo(s) neste e-mail") + "</i></p>";

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sender", Map.of("name", "E-Library", "email", remetente));
        corpo.put("to", List.of(Map.of("email", ouvidoria)));
        corpo.put("replyTo", Map.of("email", emailUsuario, "name", nomeUsuario));
        corpo.put("subject", "[E-Library] Reclamação " + protocolo + " - " + nomeUsuario);
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
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Não foi possível enviar o e-mail para a ouvidoria agora. Tente novamente em instantes.");
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Envio interrompido. Tente novamente.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível enviar o e-mail para a ouvidoria agora. Tente novamente em instantes.");
        }
    }
}
