package com.e_library.modules.user.controller;

import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@CrossOrigin(origins = "*")
public class PerfilController {

    private final UserRepository userRepository;

    public PerfilController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Dados do usuário logado (nome, e-mail, papel, RGM e curso)
    @GetMapping
    public ResponseEntity<?> meusDados(Authentication auth) {
        UserEntity user = userRepository.findByEmail(auth.getName()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body("Usuário não encontrado.");
        }

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", user.getId());
        dados.put("nome", user.getName());
        dados.put("email", user.getEmail());
        dados.put("papel", user.getRole());
        dados.put("rgm", user.getRgm());
        dados.put("curso", user.getCurso());
        return ResponseEntity.ok(dados);
    }
}
