package com.e_library.modules.emprestimo.controller;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import com.e_library.modules.emprestimo.repository.EmprestimoRepository;
import com.e_library.modules.livro.entity.LivroEntity;
import com.e_library.modules.livro.repository.LivroRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/emprestimos")
@CrossOrigin(origins = "*")
public class EmprestimoController {

    private final EmprestimoRepository emprestimoRepository = null;

    private final LivroRepository livroRepository = null;

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<String> cancelarReserva(@PathVariable @NonNull UUID id) {
        EmprestimoEntity emprestimo = emprestimoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado."));

        // Regra: Não pode cancelar se já foi retirado fisicamente
        if ("retirado".equalsIgnoreCase(emprestimo.getStatus())) {
            return ResponseEntity.badRequest().body("Não é possível cancelar um livro que já foi retirado.");
        }

        emprestimo.setStatus("cancelado");
        emprestimoRepository.save(emprestimo);

        // Devolve a unidade para o estoque do livro
        LivroEntity livro = emprestimo.getLivro();
        if (livro.getQtdDisponivel() != null) {
            livro.setQtdDisponivel(livro.getQtdDisponivel() + 1);
            livroRepository.save(livro);
        }

        return ResponseEntity.ok("Reserva cancelada com sucesso!");
    }
}