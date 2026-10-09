package com.e_library.modules.livro.controller;

import com.e_library.modules.livro.entity.LivroEntity;
import com.e_library.modules.livro.repository.LivroRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/livros")
@CrossOrigin(origins = "*")
public class LivroController {

    private final LivroRepository livroRepository = null;

    // 1. Desativar livro com motivo (Visão do Bibliotecário)
    @PutMapping("/{id}/desativar")
    public ResponseEntity<String> desativarLivro(@PathVariable @NonNull UUID id, @RequestBody Map<String, String> payload) {
        String motivo = payload.get("motivo");

        LivroEntity livro = livroRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Livro não encontrado."));

        livro.setAtivo(false);
        livro.setMotivoInatividade(motivo);
        livroRepository.save(livro);

        return ResponseEntity.ok("Livro desativado com sucesso. Motivo registrado.");
    }

    // 2. Pesquisa dinâmica com filtros (Visão do Usuário)
    @GetMapping("/pesquisa")
    public ResponseEntity<List<LivroEntity>> pesquisarLivros(
            @RequestParam String filtro, 
            @RequestParam String termo) {
        
        List<LivroEntity> resultados;

        // Verifica qual filtro o usuário selecionou no dropdown
        switch (filtro.toLowerCase()) {
            case "autor":
                resultados = livroRepository.findByAutorContainingIgnoreCaseAndAtivoTrue(termo);
                break;
            case "isbn":
                resultados = livroRepository.findByIsbnContainingIgnoreCaseAndAtivoTrue(termo);
                break;
            case "titulo":
            default:
                resultados = livroRepository.findByTituloContainingIgnoreCaseAndAtivoTrue(termo);
                break;
        }

        return ResponseEntity.ok(resultados);
    }
    @PostMapping
    public ResponseEntity<LivroEntity> cadastrarLivro(@RequestBody LivroEntity novoLivro) {
        novoLivro.setAtivo(true);
        LivroEntity salvo = livroRepository.save(novoLivro);
        return ResponseEntity.ok(salvo);
    }
}