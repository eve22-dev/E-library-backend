package com.e_library.modules.user.controller;

import com.e_library.modules.user.service.CriacaoUsuarioService;
import com.e_library.modules.user.service.GestaoUsuarioService;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

// Só o administrador de TI chega aqui (ver SecurityConfig: /api/admin/** exige ROLE_TI)
@RestController
@RequestMapping("/api/admin/usuarios")
@CrossOrigin(origins = "*")
public class AdminUsuariosController {

    public record NovoUsuario(String nome, String email, String senha, String papel, String rgm, String curso) {}

    public record EdicaoUsuario(String nome, String papel, String rgm, String curso) {}

    public record NovaSenha(String senha) {}

    private final CriacaoUsuarioService criacaoUsuarioService;
    private final GestaoUsuarioService gestaoUsuarioService;

    public AdminUsuariosController(CriacaoUsuarioService criacaoUsuarioService, GestaoUsuarioService gestaoUsuarioService) {
        this.criacaoUsuarioService = criacaoUsuarioService;
        this.gestaoUsuarioService = gestaoUsuarioService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String nome,
                                    @RequestParam(required = false) String email,
                                    @RequestParam(required = false) String rgm,
                                    @RequestParam(required = false) String papel,
                                    @RequestParam(required = false) String situacao) {
        return ResponseEntity.ok(gestaoUsuarioService.listar(nome, email, rgm, papel, situacao));
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody NovoUsuario corpo) {
        try {
            UUID id = criacaoUsuarioService.criar(new CriacaoUsuarioService.Novo(
                    corpo.nome(), corpo.email(), corpo.senha(), corpo.papel(), corpo.rgm(), corpo.curso()));

            return ResponseEntity.ok(Map.of(
                    "mensagem", "Usuário cadastrado com sucesso!",
                    "id", id.toString(),
                    "papel", corpo.papel()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> editar(@PathVariable @NonNull UUID id, @RequestBody EdicaoUsuario corpo,
                                         Authentication auth) {
        try {
            gestaoUsuarioService.atualizar(id, auth.getName(),
                    new GestaoUsuarioService.Dados(corpo.nome(), corpo.papel(), corpo.rgm(), corpo.curso()));
            return ResponseEntity.ok("Dados do usuário atualizados.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/{id}/inativar")
    public ResponseEntity<String> inativar(@PathVariable @NonNull UUID id, Authentication auth) {
        try {
            gestaoUsuarioService.definirAtivo(id, auth.getName(), false);
            return ResponseEntity.ok("Usuário inativado. Ele não consegue mais entrar no sistema.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/{id}/reativar")
    public ResponseEntity<String> reativar(@PathVariable @NonNull UUID id, Authentication auth) {
        try {
            gestaoUsuarioService.definirAtivo(id, auth.getName(), true);
            return ResponseEntity.ok("Usuário reativado.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/{id}/senha")
    public ResponseEntity<String> redefinirSenha(@PathVariable @NonNull UUID id, @RequestBody NovaSenha corpo) {
        try {
            criacaoUsuarioService.redefinirSenha(id, corpo.senha());
            return ResponseEntity.ok("Senha redefinida.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}
