package com.e_library.modules.suporte.controller;

import com.e_library.modules.suporte.service.AnexoService;
import com.e_library.modules.suporte.service.EmailService;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reclamacoes")
@CrossOrigin(origins = "*")
public class ReclamacaoController {

    private final UserRepository userRepository;
    private final AnexoService anexoService;
    private final EmailService emailService;
    private final JdbcTemplate jdbc;

    public ReclamacaoController(UserRepository userRepository, AnexoService anexoService,
                                EmailService emailService, JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.anexoService = anexoService;
        this.emailService = emailService;
        this.jdbc = jdbc;
    }

    // A reclamação NÃO vai para o painel: sai por e-mail para a ouvidoria.
    // No banco fica só o protocolo (quem, quando e se o e-mail foi enviado).
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> registrar(
            @RequestParam("descricao") String descricao,
            @RequestParam(value = "arquivos", required = false) List<MultipartFile> arquivos,
            Authentication auth) {

        String texto = descricao == null ? "" : descricao.trim();

        if (texto.length() < 10 || texto.length() > 3000) {
            return ResponseEntity.badRequest().body("Descreva a reclamação com pelo menos 10 caracteres (máximo 3000).");
        }

        UserEntity usuario = userRepository.findByEmail(auth.getName()).orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(401).body("Usuário não encontrado.");
        }

        try {
            List<AnexoService.ArquivoValidado> anexos = anexoService.validar(arquivos);

            UUID id = UUID.randomUUID();
            String protocolo = "REC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + id.toString().substring(0, 4).toUpperCase();

            String statusEnvio = "enviada";

            try {
                emailService.enviarReclamacao(protocolo, usuario.getName(), usuario.getEmail(), texto, anexos);
            } catch (ResponseStatusException falhaEmail) {
                statusEnvio = "falhou";
                gravarProtocolo(id, usuario.getId(), protocolo, anexos.size(), statusEnvio);
                return ResponseEntity.status(falhaEmail.getStatusCode()).body(falhaEmail.getReason());
            }

            gravarProtocolo(id, usuario.getId(), protocolo, anexos.size(), statusEnvio);

            return ResponseEntity.ok("Reclamação enviada à ouvidoria! Protocolo: " + protocolo);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    private void gravarProtocolo(UUID id, UUID userId, String protocolo, int qtdAnexos, String statusEnvio) {
        jdbc.update(
                "INSERT INTO reclamacoes (id, user_id, protocolo, qtd_anexos, status_envio) VALUES (?, ?, ?, ?, ?)",
                id, userId, protocolo, qtdAnexos, statusEnvio);
    }
}
