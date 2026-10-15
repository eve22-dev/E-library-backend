package com.e_library.modules.multa.controller;

import com.e_library.modules.multa.entity.MultaEntity;
import com.e_library.modules.multa.repository.MultaRepository;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/multas")
@CrossOrigin(origins = "*")
public class MultaController {

    private final MultaRepository multaRepository;
    private final UserRepository userRepository;

    public MultaController(MultaRepository multaRepository, UserRepository userRepository) {
        this.multaRepository = multaRepository;
        this.userRepository = userRepository;
    }

    // Multas do usuário logado
    @GetMapping("/minhas")
    public ResponseEntity<List<MultaEntity>> minhas(Authentication auth) {
        UserEntity usuario = userRepository.findByEmail(auth.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(multaRepository.findByUsuario_IdOrderByCriadaEmDesc(usuario.getId()));
    }

    // Todas as multas (painel do bibliotecário)
    @GetMapping
    public ResponseEntity<List<MultaEntity>> todas() {
        return ResponseEntity.ok(multaRepository.findAllByOrderByCriadaEmDesc());
    }

    // Pagamento da multa pelo próprio usuário.
    // ATENÇÃO: pagamento SIMULADO (não há gateway de pagamento real integrado).
    @PostMapping("/{id}/pagar")
    public ResponseEntity<String> pagar(@PathVariable @NonNull UUID id, Authentication auth) {
        MultaEntity multa = multaRepository.findById(id).orElse(null);

        if (multa == null) {
            return ResponseEntity.status(404).body("Multa não encontrada.");
        }

        boolean ehDono = multa.getUsuario() != null
            && auth.getName().equalsIgnoreCase(multa.getUsuario().getEmail());

        if (!ehDono) {
            return ResponseEntity.status(403).body("Você não pode pagar a multa de outro usuário.");
        }

        if (!"pendente".equalsIgnoreCase(multa.getStatus())) {
            return ResponseEntity.status(409).body("Esta multa já foi paga.");
        }

        multa.setStatus("paga");
        multa.setPagaEm(LocalDate.now());
        multaRepository.save(multa);

        return ResponseEntity.ok("Pagamento confirmado!");
    }
}
