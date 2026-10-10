package com.e_library.modules.emprestimo.controller;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import com.e_library.modules.emprestimo.repository.EmprestimoRepository;
import com.e_library.modules.livro.entity.LivroEntity;
import com.e_library.modules.livro.repository.LivroRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/emprestimos")
@CrossOrigin(origins = "*") // Permite que o front-end acesse a API
public class EmprestimoController {

    // 1. CORREÇÃO: Sem "= null". Deixe o Spring Boot injetar os repositórios.
    private final EmprestimoRepository emprestimoRepository;
    private final LivroRepository livroRepository;

    // Construtor para Injeção de Dependência do Spring
    public EmprestimoController(EmprestimoRepository emprestimoRepository, LivroRepository livroRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.livroRepository = livroRepository;
    }

    // =========================================================
    // ROTAS DO USUÁRIO (O seu código excelente de cancelamento)
    // =========================================================
    
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
        if (livro != null && livro.getQtdDisponivel() != null) {
            livro.setQtdDisponivel(livro.getQtdDisponivel() + 1);
            livroRepository.save(livro);
        }

        return ResponseEntity.ok("Reserva cancelada com sucesso!");
    }

    // =========================================================
    // ROTAS DO BIBLIOTECÁRIO (Para funcionar o Painel)
    // =========================================================

    // Lista as solicitações pendentes para o bibliotecário ver
    @GetMapping("/solicitados")
    public ResponseEntity<List<EmprestimoEntity>> listarSolicitados() {
        // Atenção: Certifique-se de que no seu banco salva minúsculo "solicitado" ou maiúsculo "SOLICITADO"
        List<EmprestimoEntity> solicitados = emprestimoRepository.findByStatus("solicitado");
        return ResponseEntity.ok(solicitados);
    }

    // Botão "Marcar como Retirado" que o bibliotecário aperta quando entrega o livro físico
    @PutMapping("/{id}/retirar")
    public ResponseEntity<String> marcarComoRetirado(@PathVariable @NonNull UUID id) {
        EmprestimoEntity emprestimo = emprestimoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado."));

        // Regra extra: não pode retirar se o usuário já tiver cancelado antes
        if ("cancelado".equalsIgnoreCase(emprestimo.getStatus())) {
            return ResponseEntity.badRequest().body("Não é possível retirar um livro cuja reserva foi cancelada.");
        }

        emprestimo.setStatus("retirado");
        
        // Se no futuro você adicionar 'dataRetirada' na entidade, você pode setar aqui:
        // emprestimo.setDataRetirada(LocalDate.now());

        emprestimoRepository.save(emprestimo);

        return ResponseEntity.ok("Livro marcado como retirado com sucesso!");
    }
}