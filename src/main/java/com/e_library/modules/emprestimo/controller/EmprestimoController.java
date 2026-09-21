package com.e_library.modules.emprestimo.controller;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import com.e_library.modules.emprestimo.service.EmprestimoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/emprestimos")
@CrossOrigin(origins = "*")
public class EmprestimoController {

    private final EmprestimoService emprestimoService;

    public EmprestimoController(EmprestimoService emprestimoService) {
        this.emprestimoService = emprestimoService;
    }

    // Endpoint para o aluno reservar um livro
    @PostMapping("/reservar")
    public ResponseEntity<EmprestimoEntity> reservarLivro(@RequestBody Map<String, String> payload) {
        UUID userId = UUID.fromString(payload.get("user_id"));
        UUID livroId = UUID.fromString(payload.get("livro_id"));

        EmprestimoEntity emprestimo = emprestimoService.realizarEmprestimo(userId, livroId);
        return ResponseEntity.ok(emprestimo);
    }

    // Endpoint para listar os empréstimos de um aluno
    public List<EmprestimoEntity> listarPorUsuario(@PathVariable UUID userId) {
        return emprestimoService.listarPorUsuario(userId);
    }

    // Endpoint para a home/painel do bibliotecário (ordenado por urgência)
    @GetMapping("/bibliotecario/urgentes")
    public ResponseEntity<List<EmprestimoEntity>> listarParaBibliotecario() {
        List<EmprestimoEntity> lista = emprestimoService.listarParaPainelBibliotecario();
        return ResponseEntity.ok(lista);
    }
}