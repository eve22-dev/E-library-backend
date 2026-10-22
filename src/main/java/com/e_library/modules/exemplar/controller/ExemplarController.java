package com.e_library.modules.exemplar.controller;

import com.e_library.modules.exemplar.entity.ExemplarEntity;
import com.e_library.modules.exemplar.repository.ExemplarRepository;
import com.e_library.modules.exemplar.service.ExemplarService;
import com.e_library.modules.livro.entity.LivroEntity;
import com.e_library.modules.livro.repository.LivroRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Todas as rotas daqui são exclusivas do bibliotecário (ver SecurityConfig)
@RestController
@RequestMapping("/api/exemplares")
@CrossOrigin(origins = "*")
public class ExemplarController {

    private final ExemplarService exemplarService;
    private final ExemplarRepository exemplarRepository;
    private final LivroRepository livroRepository;

    public ExemplarController(ExemplarService exemplarService, ExemplarRepository exemplarRepository,
                              LivroRepository livroRepository) {
        this.exemplarService = exemplarService;
        this.exemplarRepository = exemplarRepository;
        this.livroRepository = livroRepository;
    }

    // Cadastro de exemplares (por leitor de código de barras ou manual)
    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody ExemplarService.CadastroRequest request) {
        try {
            return ResponseEntity.ok(exemplarService.cadastrar(request));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // Procura uma obra já cadastrada pelo ISBN (usado logo depois de bipar o código do livro)
    @GetMapping("/buscar-livro")
    public ResponseEntity<?> buscarLivroPorIsbn(@RequestParam String isbn) {
        String normalizado = ExemplarService.normalizarIsbn(isbn);

        if (normalizado.isEmpty()) {
            return ResponseEntity.badRequest().body("Informe o ISBN.");
        }

        List<LivroEntity> achados = livroRepository.buscarPorIsbnNormalizado(normalizado);

        if (achados.isEmpty()) {
            return ResponseEntity.status(404).body("Livro não cadastrado.");
        }

        LivroEntity l = achados.get(0);
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("id", l.getId());
        resumo.put("titulo", l.getTitulo());
        resumo.put("autor", l.getAutor());
        resumo.put("editora", l.getEditora());
        resumo.put("isbn", l.getIsbn());
        resumo.put("qtdTotal", l.getQtdTotal());
        resumo.put("qtdDisponivel", l.getQtdDisponivel());
        return ResponseEntity.ok(resumo);
    }

    // Lista os exemplares de um livro, com o código e a situação de cada um
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarPorLivro(@RequestParam UUID livroId) {
        List<Map<String, Object>> lista = new ArrayList<>();

        for (ExemplarEntity ex : exemplarRepository.findByLivro_IdOrderByCodigoAsc(livroId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ex.getId());
            item.put("codigo", ex.getCodigo());
            item.put("status", ex.getStatus());
            lista.add(item);
        }

        return ResponseEntity.ok(lista);
    }
}
