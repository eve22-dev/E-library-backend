package com.e_library.modules.user.controller;

import com.e_library.modules.user.service.CriacaoUsuarioService;

import org.springframework.http.ResponseEntity;
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

    private final CriacaoUsuarioService criacaoUsuarioService;

    public AdminUsuariosController(CriacaoUsuarioService criacaoUsuarioService) {
        this.criacaoUsuarioService = criacaoUsuarioService;
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
}
