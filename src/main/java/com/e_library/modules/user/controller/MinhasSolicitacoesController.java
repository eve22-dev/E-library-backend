package com.e_library.modules.user.controller;

import com.e_library.modules.suporte.service.ConversaService;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

// Tudo o que o usuário logado enviou: conversas do chat (com as respostas) e protocolos
@RestController
@RequestMapping("/api/minhas-solicitacoes")
@CrossOrigin(origins = "*")
public class MinhasSolicitacoesController {

    private final UserRepository userRepository;
    private final ConversaService conversaService;
    private final JdbcTemplate jdbc;

    public MinhasSolicitacoesController(UserRepository userRepository, ConversaService conversaService, JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.conversaService = conversaService;
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(Authentication auth) {
        UserEntity usuario = userRepository.findByEmail(auth.getName()).orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("conversas", conversaService.conversasDoUsuario(usuario.getEmail()));

        // Reclamações (user_id) e pedidos de suporte de acesso (e-mail informado) que batem com este usuário
        corpo.put("reclamacoes", jdbc.queryForList(
                "SELECT protocolo, tipo, status_envio, criada_em FROM reclamacoes " +
                "WHERE user_id = ? OR (email_informado IS NOT NULL AND LOWER(email_informado) = LOWER(?)) " +
                "ORDER BY criada_em DESC",
                usuario.getId(), usuario.getEmail()));

        return ResponseEntity.ok(corpo);
    }
}
