package com.e_library.modules.livro.controller;

import com.e_library.modules.livro.entity.CategoriaEntity;
import com.e_library.modules.livro.entity.LivroEntity;
import com.e_library.modules.livro.repository.CategoriaRepository;
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

    private final LivroRepository livroRepository;
    private final CategoriaRepository categoriaRepository;

    // Construtor para Injeção de Dependência
    public LivroController(LivroRepository livroRepository, CategoriaRepository categoriaRepository) {
        this.livroRepository = livroRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // 1. Cadastrar Categoria (Visão do Bibliotecário)
    @PostMapping("/categorias")
    public ResponseEntity<CategoriaEntity> cadastrarCategoria(@RequestBody CategoriaEntity categoria) {
        CategoriaEntity salva = categoriaRepository.save(categoria);
        return ResponseEntity.ok(salva);
    }
    // Editar Categoria (Visão do Bibliotecário)
    @PutMapping("/categorias/{id}")
    public ResponseEntity<?> editarCategoria(@PathVariable @NonNull UUID id, @RequestBody CategoriaEntity dados) {
        if (dados.getNome() == null || dados.getNome().isBlank()) {
        return ResponseEntity.badRequest().body("O nome da categoria é obrigatório.");
    }

    CategoriaEntity categoria = categoriaRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Categoria não encontrada."));

    categoria.setNome(dados.getNome().trim());
    categoria.setDescricao(dados.getDescricao());

    return ResponseEntity.ok(categoriaRepository.save(categoria));
    }
    // 2. Cadastrar Livro (Visão do Bibliotecário)
    @PostMapping
    public ResponseEntity<LivroEntity> cadastrarLivro(@RequestBody LivroEntity novoLivro) {
        novoLivro.setAtivo(true);
        if (novoLivro.getQtdTotal() != null) {
            novoLivro.setQtdDisponivel(novoLivro.getQtdTotal());
        }
        LivroEntity salvo = livroRepository.save(novoLivro);
        return ResponseEntity.ok(salvo);
    }

    // 3. Desativar livro com motivo (Visão do Bibliotecário)
    @PutMapping("/{id}/desativar")
    public ResponseEntity<String> desativarLivro(@PathVariable @NonNull UUID id, @RequestBody Map<String, String> payload) {
        String motivo = payload.get("motivo");

        LivroEntity livro = livroRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Livro não encontrado."));

        livro.setAtivo(false);
        livro.setMotivoInatividade(motivo);
        livroRepository.save(livro);

        return ResponseEntity.ok("Livro desativado com sucesso. Motivo registado.");
    }

    // 4. Pesquisa dinâmica com filtros (Visão do Usuário)
    @GetMapping("/pesquisa")
    public ResponseEntity<List<LivroEntity>> pesquisarLivros(
            @RequestParam String filtro, 
            @RequestParam String termo) {
        
        List<LivroEntity> resultados;

        switch (filtro.toLowerCase()) {
            case "autor":
                resultados = livroRepository.findByAutorContainingIgnoreCaseAndAtivoTrue(termo);
                break;
            case "isbn":
                resultados = livroRepository.findByIsbnContainingIgnoreCaseAndAtivoTrue(termo);
                break;
            case "editora":
                resultados = livroRepository.findByEditoraContainingIgnoreCaseAndAtivoTrue(termo);
                break;
            case "titulo":
            default:
                resultados = livroRepository.findByTituloContainingIgnoreCaseAndAtivoTrue(termo);
                break;
        }

        return ResponseEntity.ok(resultados);
    }
}